package nl.belastingdienst.bte.validation;

import nl.belastingdienst.bte.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BusinessRuleValidatorTest {

    private BusinessRuleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BusinessRuleValidator();
    }

    private ClientConfig createConfigWithFields(List<FieldMapping> fields) {
        ClientConfig config = new ClientConfig();
        config.setClientId("test");
        config.setClientName("Test");
        MetadataMapping mapping = new MetadataMapping();
        mapping.setObjectStore("OS");
        mapping.setDocumentClass("DC");
        mapping.setFields(fields);
        config.setMetadataMapping(mapping);
        return config;
    }

    private FieldMapping createField(String source, String target, String dataType, boolean required) {
        FieldMapping f = new FieldMapping();
        f.setSourceField(source);
        f.setTargetProperty(target);
        f.setDataType(dataType);
        f.setRequired(required);
        return f;
    }

    @Test
    void testValidConfigNoDuplicates() {
        List<FieldMapping> fields = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            fields.add(createField("//field" + i, "prop" + i, "string", false));
        }
        ClientConfig config = createConfigWithFields(fields);
        Map<String, String> extracted = new HashMap<>();

        ValidationResult result = validator.validate(config, extracted);
        assertTrue(result.isValid());
        assertEquals("BUSINESS", result.getLayer());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void testDuplicateTargetProperty() {
        List<FieldMapping> fields = List.of(
            createField("//field1", "sameProp", "string", false),
            createField("//field2", "sameProp", "string", false)
        );
        ClientConfig config = createConfigWithFields(fields);

        ValidationResult result = validator.validate(config, Map.of());
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("VAL_004")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("sameProp")));
    }

    @Test
    void testNullTargetPropertySkipped() {
        FieldMapping f = createField("//field1", null, "string", false);
        ClientConfig config = createConfigWithFields(List.of(f));

        ValidationResult result = validator.validate(config, Map.of());
        // Should not throw, null targetProperty is skipped in duplicate check
        assertTrue(result.isValid() || !result.getErrors().stream().anyMatch(e -> e.getCode().equals("VAL_004")));
    }

    @Test
    void testRequiredFieldMissingValueNoDefault() {
        FieldMapping f = createField("//missing", "prop1", "string", true);
        ClientConfig config = createConfigWithFields(List.of(f));

        Map<String, String> extracted = Map.of("//missing", "(niet gevonden)");
        ValidationResult result = validator.validate(config, extracted);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("VAL_001")));
    }

    @Test
    void testRequiredFieldMissingValueWithDefault() {
        FieldMapping f = createField("//missing", "prop1", "string", true);
        f.setDefaultValue("fallback");
        ClientConfig config = createConfigWithFields(List.of(f));

        Map<String, String> extracted = Map.of("//missing", "(niet gevonden)");
        ValidationResult result = validator.validate(config, extracted);
        // Should NOT error because default exists
        assertTrue(result.getErrors().stream().noneMatch(e ->
            e.getMessage().contains("prop1") && e.getCode().equals("VAL_001")));
    }

    @Test
    void testRequiredFieldNullValueNoDefault() {
        FieldMapping f = createField("//absent", "prop1", "string", true);
        ClientConfig config = createConfigWithFields(List.of(f));

        Map<String, String> extracted = new HashMap<>(); // key not present
        ValidationResult result = validator.validate(config, extracted);
        assertFalse(result.isValid());
    }

    @Test
    void testRequiredFieldWithNullSourceField() {
        FieldMapping f = createField(null, "prop1", "string", true);
        ClientConfig config = createConfigWithFields(List.of(f));

        ValidationResult result = validator.validate(config, Map.of());
        // sourceField is null, so this branch is skipped
        assertTrue(result.getErrors().stream().noneMatch(e -> e.getCode().equals("VAL_001")));
    }

    @Test
    void testNonRequiredFieldMissingValueOk() {
        FieldMapping f = createField("//missing", "prop1", "string", false);
        ClientConfig config = createConfigWithFields(List.of(f));

        Map<String, String> extracted = Map.of("//missing", "(niet gevonden)");
        ValidationResult result = validator.validate(config, extracted);
        assertTrue(result.getErrors().stream().noneMatch(e -> e.getCode().equals("VAL_001")));
    }

    @Test
    void testInvalidDateFormat() {
        FieldMapping f = createField("//date", "dateProp", "date", false);
        f.setFormat("INVALID_PATTERN{{{");
        ClientConfig config = createConfigWithFields(List.of(f));

        ValidationResult result = validator.validate(config, Map.of());
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("VAL_003")));
    }

    @Test
    void testValidDateFormat() {
        FieldMapping f = createField("//date", "dateProp", "date", false);
        f.setFormat("yyyy-MM-dd");
        ClientConfig config = createConfigWithFields(List.of(f));

        ValidationResult result = validator.validate(config, Map.of());
        assertTrue(result.getErrors().stream().noneMatch(e -> e.getCode().equals("VAL_003")));
    }

    @Test
    void testNonDateFieldWithFormatIgnored() {
        FieldMapping f = createField("//str", "strProp", "string", false);
        f.setFormat("yyyy-MM-dd");
        ClientConfig config = createConfigWithFields(List.of(f));

        ValidationResult result = validator.validate(config, Map.of());
        assertTrue(result.getErrors().stream().noneMatch(e -> e.getCode().equals("VAL_003")));
    }

    @Test
    void testDateFieldWithNullFormatOk() {
        FieldMapping f = createField("//date", "dateProp", "date", false);
        f.setFormat(null);
        ClientConfig config = createConfigWithFields(List.of(f));

        ValidationResult result = validator.validate(config, Map.of());
        assertTrue(result.getErrors().stream().noneMatch(e -> e.getCode().equals("VAL_003")));
    }

    @Test
    void testWarningFewerThan11Fields() {
        List<FieldMapping> fields = List.of(
            createField("//f1", "p1", "string", false),
            createField("//f2", "p2", "string", false)
        );
        ClientConfig config = createConfigWithFields(fields);

        ValidationResult result = validator.validate(config, Map.of());
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.getCode().equals("VAL_W01")));
    }

    @Test
    void testNoWarningWith11PlusFields() {
        List<FieldMapping> fields = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            fields.add(createField("//f" + i, "p" + i, "string", false));
        }
        ClientConfig config = createConfigWithFields(fields);

        ValidationResult result = validator.validate(config, Map.of());
        assertTrue(result.getWarnings().stream().noneMatch(w -> w.getCode().equals("VAL_W01")));
    }

    @Test
    void testNullMetadataMapping() {
        ClientConfig config = new ClientConfig();
        ValidationResult result = validator.validate(config, Map.of());
        assertTrue(result.isValid());
        assertEquals("BUSINESS", result.getLayer());
    }

    @Test
    void testNullFields() {
        ClientConfig config = new ClientConfig();
        MetadataMapping mapping = new MetadataMapping();
        mapping.setFields(null);
        config.setMetadataMapping(mapping);

        ValidationResult result = validator.validate(config, Map.of());
        assertTrue(result.isValid());
    }
}
