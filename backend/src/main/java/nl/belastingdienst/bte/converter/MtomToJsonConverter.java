package nl.belastingdienst.bte.converter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.belastingdienst.bte.model.*;
import nl.belastingdienst.bte.parser.MtomParser;
import nl.belastingdienst.bte.validation.SchemaValidator;
import nl.belastingdienst.bte.validation.BusinessRuleValidator;
import nl.belastingdienst.bte.validation.ReferenceValidator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class MtomToJsonConverter {

    @Inject
    MtomParser mtomParser;

    @Inject
    SchemaValidator schemaValidator;

    @Inject
    BusinessRuleValidator businessRuleValidator;

    @Inject
    ReferenceValidator referenceValidator;

    public ConversionResult convert(byte[] mtomContent, String contentType, ClientConfig config) {
        ConversionResult result = new ConversionResult();
        List<ValidationResult> validationResults = new ArrayList<>();
        Map<String, String> extractedFields = new LinkedHashMap<>();

        try {
            // Parse MTOM message
            MtomParser.ParsedMtomMessage parsed = mtomParser.parse(mtomContent, contentType);

            // Extract fields using XPath mappings from config
            Map<String, Object> jsonOutput = new LinkedHashMap<>();
            jsonOutput.put("clientId", config.getClientId());
            jsonOutput.put("clientName", config.getClientName());
            jsonOutput.put("objectStore", config.getMetadataMapping().getObjectStore());
            jsonOutput.put("documentClass", config.getMetadataMapping().getDocumentClass());

            Map<String, Object> metadata = new LinkedHashMap<>();

            for (FieldMapping field : config.getMetadataMapping().getFields()) {
                String value = mtomParser.evaluateXPath(parsed.getXmlDocument(), field.getSourceField());

                if (value != null) {
                    extractedFields.put(field.getSourceField(), value);
                    Object typedValue = convertValue(value, field.getDataType(), field.getFormat());
                    metadata.put(field.getTargetProperty(), typedValue);
                } else if (field.getDefaultValue() != null) {
                    extractedFields.put(field.getSourceField(), "(default) " + field.getDefaultValue());
                    metadata.put(field.getTargetProperty(), field.getDefaultValue());
                } else {
                    extractedFields.put(field.getSourceField(), "(niet gevonden)");
                }
            }

            jsonOutput.put("metadata", metadata);
            jsonOutput.put("processedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            result.setJsonOutput(jsonOutput);
            result.setExtractedFields(extractedFields);

            // Run 3-layer validation
            validationResults.add(schemaValidator.validate(config));
            validationResults.add(businessRuleValidator.validate(config, extractedFields));
            validationResults.add(referenceValidator.validate(config, parsed.getXmlDocument()));

            result.setValidationResults(validationResults);
            result.setSuccess(validationResults.stream().allMatch(ValidationResult::isValid));

        } catch (Exception e) {
            result.setSuccess(false);
            ValidationResult errorResult = new ValidationResult("SCHEMA");
            errorResult.addError("PARSE_001", "Fout bij het parsen van het MTOM bericht: " + e.getMessage(),
                "Controleer of het bestand een geldig MTOM/MIME bericht is");
            validationResults.add(errorResult);
            result.setValidationResults(validationResults);
        }

        return result;
    }

    private Object convertValue(String value, String dataType, String format) {
        if (value == null) return null;

        switch (dataType != null ? dataType.toLowerCase() : "string") {
            case "integer":
                try { return Long.parseLong(value); } catch (NumberFormatException e) { return value; }
            case "boolean":
                return Boolean.parseBoolean(value);
            case "date":
                return value; // Keep as string, frontend can format
            default:
                return value;
        }
    }
}
