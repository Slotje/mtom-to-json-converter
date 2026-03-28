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
        byte[] effectiveContent = mtomContent;

        MimeDetectionResult mimeDetection = detectAndStripMimeHeaders(mtomContent);
        if (mimeDetection != null) {
            effectiveContentType = mimeDetection.contentType;
            effectiveContent = mimeDetection.body;
        }

        if (effectiveContentType != null && effectiveContentType.contains("multipart/related")) {
            parseMultipartMime(effectiveContent, effectiveContentType, result);
        } else {
            // Plain XML (not MTOM wrapped)
            String xmlContent = new String(effectiveContent, StandardCharsets.UTF_8);
            result.setRawXml(xmlContent);
            result.setXmlDocument(parseXml(xmlContent));
        }

        return result;
    }

    private void parseMultipartMime(byte[] content, String contentType, ParsedMtomMessage result) throws Exception {
        ByteArrayDataSource dataSource = new ByteArrayDataSource(content, contentType);
        MimeMultipart multipart = new MimeMultipart(dataSource);

        // Extract start parameter to identify XOP root part
        String startContentId = extractStartContentId(contentType);

        // Collect all parts by content-id
        List<String> xmlParts = new ArrayList<>();
        Map<String, byte[]> attachmentsByContentId = new HashMap<>();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            String partContentType = part.getContentType();
            String partContentId = getPartContentId(part);
            byte[] partData = part.getInputStream().readAllBytes();

            // Check if this is the XOP root/manifest part (just contains xop:Include references)
            if (isXopManifest(partContentId, startContentId, i, partData)) {
                // Store referenced content-ids but skip the manifest itself
                continue;
            }

            boolean isXml = partContentType.contains("xml") || partContentType.contains("xop+xml");

            if (isXml) {
                String xmlContent = new String(partData, StandardCharsets.UTF_8);
                // Strip XML declaration for combining
                xmlContent = xmlContent.replaceFirst("^<\\?xml[^?]*\\?>\\s*", "");
                xmlParts.add(xmlContent);

                // Also store by content-id so XOP includes can reference it
                if (partContentId != null) {
                    attachmentsByContentId.put(partContentId, partData);
                }
            } else {
                // Binary attachment
                if (partContentId != null) {
                    attachmentsByContentId.put(partContentId, partData);
                }
            }
        }

        result.setAttachments(attachmentsByContentId);

        // Build combined XML document from all XML parts
        if (xmlParts.size() == 1) {
            String xml = xmlParts.get(0);
            result.setRawXml(xml);
            result.setXmlDocument(parseXml(xml));
        } else if (xmlParts.size() > 1) {
            String combined = "<mtom-bericht>" + String.join("", xmlParts) + "</mtom-bericht>";
            result.setRawXml(combined);
            result.setXmlDocument(parseXml(combined));
        }

        // Try to decode Base64 content in the XML (e.g. MHS body with zipped payload)
        resolveBase64Payloads(result);
    }

    private String getPartContentId(BodyPart part) throws Exception {
        String[] headers = part.getHeader("Content-ID");
        if (headers == null || headers.length == 0) return null;
        return headers[0].replaceAll("[<>]", "").trim();
    }

    private String extractStartContentId(String contentType) {
        int startIdx = contentType.indexOf("start=\"");
        if (startIdx < 0) return null;
        int begin = startIdx + 7;
        int end = contentType.indexOf("\"", begin);
        if (end < 0) return null;
        return contentType.substring(begin, end).replaceAll("[<>]", "").trim();
    }

    private boolean isXopManifest(String partContentId, String startContentId, int index, byte[] data) {
        // Match by start parameter content-id
        if (startContentId != null && partContentId != null && partContentId.equals(startContentId)) {
            return true;
        }
        // Fallback: check if first part is just an XOP container
        if (index == 0) {
            String text = new String(data, StandardCharsets.UTF_8).trim();
            if (text.startsWith("<xop") && text.contains("xop:Include")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Try to find and decode Base64-encoded payloads in the XML document.
     * For example, the MHS body often contains a Base64-encoded ZIP with the actual message.
     */
    private void resolveBase64Payloads(ParsedMtomMessage message) {
        if (message.getXmlDocument() == null) return;

        // Look for elements with long Base64-like text content
        resolveBase64Recursive(message.getXmlDocument().getDocumentElement(), message);
    }

    private void resolveBase64Recursive(Element element, ParsedMtomMessage message) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                resolveBase64Recursive((Element) children.item(i), message);
            }
        }

        // Check if this leaf element has long Base64-like content
        if (!hasChildElements(element)) {
            String text = element.getTextContent();
            if (text != null && text.length() > 200 && text.matches("[A-Za-z0-9+/=\\s]+")) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(text.replaceAll("\\s", ""));
                    byte[] unzipped = tryUnzip(decoded);
                    byte[] payload = unzipped != null ? unzipped : decoded;
                    String payloadText = new String(payload, StandardCharsets.UTF_8);

                    // If the decoded content is XML, try to parse and embed it
                    if (payloadText.trim().startsWith("<")) {
                        try {
                            Document payloadDoc = parseXml(payloadText);
                            // Import the payload's root element into our document
                            Node imported = element.getOwnerDocument().importNode(
                                payloadDoc.getDocumentElement(), true);
                            // Replace Base64 text with parsed XML
                            while (element.hasChildNodes()) {
                                element.removeChild(element.getFirstChild());
                            }
                            element.appendChild(imported);
                        } catch (Exception e) {
                            // Not valid XML, store as attachment
                            message.getAttachments().put("decoded-payload", payload);
                        }
                    } else {
                        message.getAttachments().put("decoded-payload", payload);
                    }
                } catch (Exception e) {
                    // Not Base64, leave as is
                }
            }
        }
    }

    private boolean hasChildElements(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) return true;
        }
        return false;
    }

    private static class MimeDetectionResult {
        String contentType;
        byte[] body;
        MimeDetectionResult(String contentType, byte[] body) {
            this.contentType = contentType;
            this.body = body;
        }
    }

    /**
     * Detect MIME format from content, extract Content-Type header,
     * and strip envelope headers so MimeMultipart gets only the body.
     */
    private MimeDetectionResult detectAndStripMimeHeaders(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);

        // Check if content starts with MIME headers
        if (!text.startsWith("MIME-Version:") && !text.startsWith("Content-Type:") && !text.startsWith("--")) {
            return null;
        }

        // If starts with boundary directly, no headers to strip
        if (text.startsWith("--")) {
            String firstLine = text.substring(2, text.indexOf('\n')).trim();
            String ct = "multipart/related; boundary=\"" + firstLine + "\"; type=\"application/xop+xml\"";
            return new MimeDetectionResult(ct, content);
        }

        // Parse MIME envelope headers to find Content-Type and the body start
        String contentTypeLine = null;
        int bodyStart = -1;

        // Find the blank line separating headers from body
        int idx = 0;
        String[] lines = text.split("\n");
        int charPos = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].replace("\r", "");

            if (line.isEmpty()) {
                // Blank line = end of headers, body starts after
                bodyStart = charPos + lines[i].length() + 1; // +1 for \n
                break;
            }

            if (line.toLowerCase().startsWith("content-type:")) {
                StringBuilder ct = new StringBuilder(line.substring("content-type:".length()).trim());
                // Handle continuation lines
                for (int j = i + 1; j < lines.length; j++) {
                    String next = lines[j].replace("\r", "");
                    if (next.startsWith(" ") || next.startsWith("\t")) {
                        ct.append(" ").append(next.trim());
                    } else {
                        break;
                    }
                }
                contentTypeLine = ct.toString();
            }

            charPos += lines[i].length() + 1; // +1 for \n
        }

        if (contentTypeLine != null && contentTypeLine.contains("multipart/related") && bodyStart > 0) {
            byte[] body = new byte[content.length - bodyStart];
            System.arraycopy(content, bodyStart, body, 0, body.length);
            return new MimeDetectionResult(contentTypeLine, body);
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
