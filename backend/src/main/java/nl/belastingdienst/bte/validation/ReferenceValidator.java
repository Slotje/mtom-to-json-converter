package nl.belastingdienst.bte.validation;

import jakarta.enterprise.context.ApplicationScoped;
import nl.belastingdienst.bte.model.*;
import org.w3c.dom.Document;

import javax.xml.xpath.*;

@ApplicationScoped
public class ReferenceValidator {

    public ValidationResult validate(ClientConfig config, Document xmlDocument) {
        ValidationResult result = new ValidationResult("REFERENCE");

        if (config.getMetadataMapping() == null || config.getMetadataMapping().getFields() == null) {
            return result;
        }

        if (xmlDocument == null) {
            result.addWarning("VAL_W01", "Geen XML document beschikbaar voor referentie-validatie",
                "Upload een MTOM bericht om referentie-integriteit te valideren");
            return result;
        }

        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        for (FieldMapping field : config.getMetadataMapping().getFields()) {
            if (field.getSourceField() != null) {
                // Validate XPath expression is syntactically correct
                try {
                    xpath.compile(field.getSourceField());
                } catch (XPathExpressionException e) {
                    result.addError("VAL_005",
                        "Ongeldige XPath expressie '" + field.getSourceField() + "' voor veld '" + field.getTargetProperty() + "'",
                        "Controleer de XPath syntax. Voorbeeld: //elementNaam of //element[@attribuut='waarde']");
                }

                // Check if XPath actually resolves to a value in the document
                try {
                    String adaptedXpath = adaptXPathForNamespaces(field.getSourceField());
                    XPathExpression expr = xpath.compile(adaptedXpath);
                    String value = expr.evaluate(xmlDocument);

                    if ((value == null || value.isEmpty()) && field.isRequired() && field.getDefaultValue() == null) {
                        result.addWarning("VAL_W01",
                            "XPath '" + field.getSourceField() + "' geeft geen resultaat in het huidige bericht",
                            "Controleer of het XPath pad correct is of voeg een defaultValue toe");
                    }

                    // Type compatibility check
                    if (value != null && !value.isEmpty() && field.getDataType() != null) {
                        checkTypeCompatibility(result, field, value);
                    }
                } catch (XPathExpressionException e) {
                    // Already reported above
                }
            }
        }

        return result;
    }

    private void checkTypeCompatibility(ValidationResult result, FieldMapping field, String value) {
        switch (field.getDataType().toLowerCase()) {
            case "integer":
                try {
                    Long.parseLong(value);
                } catch (NumberFormatException e) {
                    result.addError("VAL_002",
                        "Type-incompatibiliteit voor veld '" + field.getTargetProperty() + "': verwacht integer, gevonden '" + value + "'",
                        "Wijzig het dataType naar 'string' of controleer of de brondata een geheel getal bevat");
                }
                break;
            case "boolean":
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    result.addError("VAL_002",
                        "Type-incompatibiliteit voor veld '" + field.getTargetProperty() + "': verwacht boolean, gevonden '" + value + "'",
                        "Wijzig het dataType naar 'string' of controleer of de brondata 'true' of 'false' bevat");
                }
                break;
            case "date":
                if (!value.matches("\\d{4}-\\d{2}-\\d{2}.*") && !value.matches("\\d{2}-\\d{2}-\\d{4}.*")) {
                    result.addWarning("VAL_W01",
                        "Waarde '" + value + "' voor veld '" + field.getTargetProperty() + "' lijkt geen datum te zijn",
                        "Controleer het datumformaat of wijzig het dataType");
                }
                break;
        }
    }

    private String adaptXPathForNamespaces(String xpathExpr) {
        if (xpathExpr.startsWith("//")) {
            String remainder = xpathExpr.substring(2);
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
        return xpathExpr;
    }
}
