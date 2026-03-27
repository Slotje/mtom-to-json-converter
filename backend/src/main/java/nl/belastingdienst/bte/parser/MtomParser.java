package nl.belastingdienst.bte.parser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@ApplicationScoped
public class MtomParser {

    public static class ParsedMtomMessage {
        private Document xmlDocument;
        private Map<String, byte[]> attachments = new HashMap<>();
        private String rawXml;

        public Document getXmlDocument() { return xmlDocument; }
        public void setXmlDocument(Document xmlDocument) { this.xmlDocument = xmlDocument; }
        public Map<String, byte[]> getAttachments() { return attachments; }
        public void setAttachments(Map<String, byte[]> attachments) { this.attachments = attachments; }
        public String getRawXml() { return rawXml; }
        public void setRawXml(String rawXml) { this.rawXml = rawXml; }
    }

    public ParsedMtomMessage parse(byte[] mtomContent, String contentType) throws Exception {
        ParsedMtomMessage result = new ParsedMtomMessage();

        // Auto-detect MIME multipart from content if content-type doesn't indicate it
        String effectiveContentType = contentType;
        if (effectiveContentType == null || !effectiveContentType.contains("multipart/related")) {
            String detectedContentType = detectMimeContentType(mtomContent);
            if (detectedContentType != null) {
                effectiveContentType = detectedContentType;
            }
        }

        if (effectiveContentType != null && effectiveContentType.contains("multipart/related")) {
            // MTOM/MIME multipart message
            ByteArrayDataSource dataSource = new ByteArrayDataSource(mtomContent, effectiveContentType);
            MimeMultipart multipart = new MimeMultipart(dataSource);

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                String partContentType = part.getContentType();

                if (i == 0 || partContentType.contains("text/xml") || partContentType.contains("application/xop+xml")) {
                    // SOAP envelope / XML part
                    String xmlContent = new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    result.setRawXml(xmlContent);
                    result.setXmlDocument(parseXml(xmlContent));
                } else {
                    // Binary attachment
                    String contentId = part.getHeader("Content-ID") != null ? part.getHeader("Content-ID")[0] : "attachment-" + i;
                    contentId = contentId.replaceAll("[<>]", "");
                    byte[] attachmentData = part.getInputStream().readAllBytes();

                    // Try to decode base64 and unzip if applicable
                    try {
                        byte[] decoded = Base64.getDecoder().decode(attachmentData);
                        byte[] unzipped = tryUnzip(decoded);
                        result.getAttachments().put(contentId, unzipped != null ? unzipped : decoded);
                    } catch (Exception e) {
                        result.getAttachments().put(contentId, attachmentData);
                    }
                }
            }
        } else {
            // Plain XML (not MTOM wrapped)
            String xmlContent = new String(mtomContent, StandardCharsets.UTF_8);
            result.setRawXml(xmlContent);
            result.setXmlDocument(parseXml(xmlContent));
        }

        // Resolve XOP includes
        resolveXopIncludes(result);

        return result;
    }

    /**
     * Auto-detect MIME multipart format from content.
     * Parses MIME headers to extract Content-Type with boundary.
     */
    private String detectMimeContentType(byte[] content) {
        String header = new String(content, 0, Math.min(content.length, 1024), StandardCharsets.UTF_8);

        // Check for MIME-Version header indicating a MIME message
        if (!header.startsWith("MIME-Version:") && !header.contains("\nMIME-Version:")) {
            // Also check if content starts directly with a MIME boundary
            if (!header.startsWith("--")) {
                return null;
            }
        }

        // Extract Content-Type from MIME headers
        String contentTypeLine = null;
        String[] lines = header.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().startsWith("content-type:")) {
                StringBuilder ct = new StringBuilder(lines[i].substring("content-type:".length()).trim());
                // Handle continuation lines (starting with whitespace)
                for (int j = i + 1; j < lines.length; j++) {
                    if (lines[j].startsWith(" ") || lines[j].startsWith("\t")) {
                        ct.append(" ").append(lines[j].trim());
                    } else {
                        break;
                    }
                }
                contentTypeLine = ct.toString();
                break;
            }
        }

        if (contentTypeLine != null && contentTypeLine.contains("multipart/related")) {
            return contentTypeLine;
        }

        // If starts with boundary marker, try to construct content type
        if (header.startsWith("--")) {
            String boundary = lines[0].substring(2).trim();
            return "multipart/related; boundary=\"" + boundary + "\"; type=\"application/xop+xml\"";
        }

        return null;
    }

    public String evaluateXPath(Document document, String xpathExpression) {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            // Handle namespace-agnostic XPath by using local-name() workaround
            String adaptedXpath = adaptXPathForNamespaces(xpathExpression);

            XPathExpression expr = xpath.compile(adaptedXpath);
            String result = expr.evaluate(document);
            return result != null && !result.isEmpty() ? result : null;
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    private String adaptXPathForNamespaces(String xpath) {
        // Convert simple XPath like //ecmid to namespace-agnostic version
        // //ecmid becomes //*[local-name()='ecmid']
        // //value[@key='X'] becomes //*[local-name()='value'][@key='X']
        if (xpath.startsWith("//")) {
            String remainder = xpath.substring(2);
            // Check for attribute predicates
            int bracketIdx = remainder.indexOf('[');
            if (bracketIdx > 0) {
                String elementName = remainder.substring(0, bracketIdx);
                String predicate = remainder.substring(bracketIdx);
                if (!elementName.contains("local-name")) {
                    return "//*[local-name()='" + elementName + "']" + predicate;
                }
            } else if (!remainder.contains("/") && !remainder.contains("[") && !remainder.contains("local-name")) {
                return "//*[local-name()='" + remainder + "']";
            }
        }
        return xpath;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // Disable external entities for security
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private void resolveXopIncludes(ParsedMtomMessage message) {
        if (message.getXmlDocument() == null) return;

        NodeList includes = message.getXmlDocument().getElementsByTagNameNS(
            "http://www.w3.org/2004/08/xop/include", "Include");

        for (int i = 0; i < includes.getLength(); i++) {
            Element include = (Element) includes.item(i);
            String href = include.getAttribute("href");
            if (href != null && href.startsWith("cid:")) {
                String contentId = href.substring(4);
                byte[] data = message.getAttachments().get(contentId);
                if (data != null) {
                    String textContent = new String(data, StandardCharsets.UTF_8);
                    Node parent = include.getParentNode();
                    parent.removeChild(include);
                    parent.setTextContent(textContent);
                }
            }
        }
    }

    private byte[] tryUnzip(byte[] data) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry != null) {
                return zis.readAllBytes();
            }
        } catch (Exception e) {
            // Not a zip file
        }
        return null;
    }
}
