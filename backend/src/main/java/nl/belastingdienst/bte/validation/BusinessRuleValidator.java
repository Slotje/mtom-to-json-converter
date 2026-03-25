package nl.belastingdienst.bte.validation;

import jakarta.enterprise.context.ApplicationScoped;
import nl.belastingdienst.bte.model.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class BusinessRuleValidator {

    public ValidationResult validate(ClientConfig config, Map<String, String> extractedFields) {
        ValidationResult result = new ValidationResult("BUSINESS");

        if (config.getMetadataMapping() == null || config.getMetadataMapping().getFields() == null) {
            return result;
        }

        var fields = config.getMetadataMapping().getFields();

        // Check for duplicate target properties
        Set<String> targetProps = new HashSet<>();
        for (FieldMapping field : fields) {
            if (field.getTargetProperty() != null) {
                if (!targetProps.add(field.getTargetProperty())) {
                    result.addError("VAL_004",
                        "Dubbele targetProperty: '" + field.getTargetProperty() + "'",
                        "Elke targetProperty mag maar een keer voorkomen in de mapping. Verwijder of hernoem de duplicate.");
                }
            }
        }

        // Check required fields have values
        for (FieldMapping field : fields) {
            if (field.isRequired() && field.getSourceField() != null) {
                String value = extractedFields.get(field.getSourceField());
                if (value == null || value.equals("(niet gevonden)")) {
                    if (field.getDefaultValue() == null) {
                        result.addError("VAL_001",
                            "Verplicht veld '" + field.getTargetProperty() + "' (bron: " + field.getSourceField() + ") heeft geen waarde",
                            "Voeg een defaultValue toe aan de mapping of controleer of het bronveld aanwezig is in het MTOM bericht");
                    }
                }
            }
        }

        // Validate date format strings
        for (FieldMapping field : fields) {
            if ("date".equals(field.getDataType()) && field.getFormat() != null) {
                try {
                    java.time.format.DateTimeFormatter.ofPattern(field.getFormat());
                } catch (IllegalArgumentException e) {
                    result.addError("VAL_003",
                        "Ongeldig datumformaat '" + field.getFormat() + "' voor veld '" + field.getTargetProperty() + "'",
                        "Gebruik een geldig Java DateTimeFormatter patroon, bijvoorbeeld: yyyy-MM-dd of dd-MM-yyyy");
                }
            }
        }

        // Warning if fewer than recommended number of fields
        if (fields.size() < 11) {
            result.addWarning("VAL_W01",
                "Configuratie bevat " + fields.size() + " veldmappings (aanbevolen: minimaal 11 verplichte DNI/IVAA velden)",
                "Overweeg de 11 standaard DNI/IVAA metadata velden toe te voegen (BBM1-BBM10, DocumentTitle)");
        }

        return result;
    }
}
