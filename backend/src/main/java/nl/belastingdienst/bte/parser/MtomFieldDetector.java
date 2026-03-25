package nl.belastingdienst.bte.parser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.belastingdienst.bte.model.DetectedField;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MtomFieldDetector {

    @Inject
    MtomParser mtomParser;

    public List<DetectedField> detectFields(byte[] mtomContent, String contentType) throws Exception {
        MtomParser.ParsedMtomMessage parsed = mtomParser.parse(mtomContent, contentType);
        List<DetectedField> fields = new ArrayList<>();

        if (parsed.getXmlDocument() != null) {
            detectFieldsRecursive(parsed.getXmlDocument().getDocumentElement(), "", fields);
        }

        return fields;
    }

    private void detectFieldsRecursive(Element element, String parentPath, List<DetectedField> fields) {
        String localName = element.getLocalName() != null ? element.getLocalName() : element.getTagName();
        String currentPath = parentPath.isEmpty() ? "//" + localName : parentPath + "/" + localName;

        // Check for key-value pattern: <value key="X">Y</value>
        if (element.hasAttribute("key")) {
            String key = element.getAttribute("key");
            String value = getTextContent(element);
            String xpath = "//" + localName + "[@key='" + key + "']";
            fields.add(new DetectedField(
                key,
                xpath,
                value,
                detectDataType(value)
            ));
            return;
        }

        // Check for leaf nodes with text content
        String textContent = getDirectTextContent(element);
        if (textContent != null && !textContent.trim().isEmpty() && !hasChildElements(element)) {
            fields.add(new DetectedField(
                localName,
                "//" + localName,
                textContent.trim(),
                detectDataType(textContent.trim())
            ));
        }

        // Also detect attributes
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String attrName = attr.getLocalName() != null ? attr.getLocalName() : attr.getNodeName();
            if (!attrName.startsWith("xmlns") && !attrName.equals("key")) {
                String attrValue = attr.getNodeValue();
                fields.add(new DetectedField(
                    localName + "@" + attrName,
                    "//" + localName + "/@" + attrName,
                    attrValue,
                    detectDataType(attrValue)
                ));
            }
        }

        // Recurse into child elements
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                detectFieldsRecursive((Element) children.item(i), currentPath, fields);
            }
        }
    }

    private String getTextContent(Element element) {
        StringBuilder sb = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.TEXT_NODE) {
                sb.append(children.item(i).getNodeValue());
            }
        }
        return sb.toString().trim();
    }

    private String getDirectTextContent(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.TEXT_NODE) {
                String text = children.item(i).getNodeValue().trim();
                if (!text.isEmpty()) return text;
            }
        }
        return null;
    }

    private boolean hasChildElements(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) return true;
        }
        return false;
    }

    private String detectDataType(String value) {
        if (value == null || value.isEmpty()) return "string";

        // Check boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) return "boolean";

        // Check integer
        try {
            Long.parseLong(value);
            return "integer";
        } catch (NumberFormatException e) {}

        // Check date patterns
        if (value.matches("\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2}:\\d{2}.*)?")) return "date";
        if (value.matches("\\d{2}-\\d{2}-\\d{4}")) return "date";

        return "string";
    }
}
