package nl.belastingdienst.bte.validation;

import jakarta.enterprise.context.ApplicationScoped;
import nl.belastingdienst.bte.model.*;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
public class SchemaValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Set<String> VALID_DATA_TYPES = Set.of("string", "integer", "boolean", "date");

    public ValidationResult validate(ClientConfig config) {
        ValidationResult result = new ValidationResult("SCHEMA");

        // Validate required top-level fields
        if (config.getClientId() == null || config.getClientId().isBlank()) {
            result.addError("VAL_001", "Verplicht veld 'clientId' ontbreekt",
                "Voeg een clientId (UUID) toe aan de configuratie");
        }
        if (config.getClientName() == null || config.getClientName().isBlank()) {
            result.addError("VAL_001", "Verplicht veld 'clientName' ontbreekt",
                "Voeg een clientName toe aan de configuratie");
        }

        // Validate metadataMapping
        if (config.getMetadataMapping() == null) {
            result.addError("VAL_001", "Verplicht veld 'metadataMapping' ontbreekt",
                "Voeg een metadataMapping sectie toe met objectStore, documentClass en fields");
        } else {
            MetadataMapping mapping = config.getMetadataMapping();
            if (mapping.getObjectStore() == null || mapping.getObjectStore().isBlank()) {
                result.addError("VAL_001", "Verplicht veld 'objectStore' ontbreekt",
                    "Voeg een objectStore naam toe aan de metadataMapping");
            }
            if (mapping.getDocumentClass() == null || mapping.getDocumentClass().isBlank()) {
                result.addError("VAL_001", "Verplicht veld 'documentClass' ontbreekt",
                    "Voeg een documentClass toe aan de metadataMapping");
            }
            if (mapping.getFields() == null || mapping.getFields().isEmpty()) {
                result.addError("VAL_001", "Verplicht veld 'fields' ontbreekt of is leeg",
                    "Voeg minimaal 1 veldmapping toe aan metadataMapping.fields");
            } else {
                // Validate each field mapping
                for (int i = 0; i < mapping.getFields().size(); i++) {
                    FieldMapping field = mapping.getFields().get(i);
                    String prefix = "fields[" + i + "]";

                    if (field.getSourceField() == null || field.getSourceField().isBlank()) {
                        result.addError("VAL_001", prefix + ": sourceField ontbreekt",
                            "Voeg een sourceField (XPath expressie) toe aan de veldmapping");
                    }
                    if (field.getTargetProperty() == null || field.getTargetProperty().isBlank()) {
                        result.addError("VAL_001", prefix + ": targetProperty ontbreekt",
                            "Voeg een targetProperty toe aan de veldmapping");
                    }
                    if (field.getDataType() != null && !VALID_DATA_TYPES.contains(field.getDataType().toLowerCase())) {
                        result.addError("VAL_002", prefix + ": Ongeldig datatype '" + field.getDataType() + "'",
                            "Gebruik een van: string, integer, boolean, date");
                    }
                }
            }
        }

        // Validate businessInfo
        if (config.getBusinessInfo() == null) {
            result.addError("VAL_001", "Verplicht veld 'businessInfo' ontbreekt",
                "Voeg een businessInfo sectie toe met contactEmail en supportGroup");
        } else {
            if (config.getBusinessInfo().getContactEmail() == null || config.getBusinessInfo().getContactEmail().isBlank()) {
                result.addError("VAL_001", "Verplicht veld 'contactEmail' ontbreekt",
                    "Voeg een geldig e-mailadres toe aan businessInfo.contactEmail");
            } else if (!EMAIL_PATTERN.matcher(config.getBusinessInfo().getContactEmail()).matches()) {
                result.addError("VAL_003", "Ongeldig e-mailformaat: '" + config.getBusinessInfo().getContactEmail() + "'",
                    "Gebruik een geldig e-mailadres, bijvoorbeeld: team@example.com");
            }
            if (config.getBusinessInfo().getSupportGroup() == null || config.getBusinessInfo().getSupportGroup().isBlank()) {
                result.addError("VAL_001", "Verplicht veld 'supportGroup' ontbreekt",
                    "Voeg een supportGroup naam toe aan businessInfo");
            }
        }

        return result;
    }
}
