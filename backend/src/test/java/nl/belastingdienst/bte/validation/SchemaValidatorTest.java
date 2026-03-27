package nl.belastingdienst.bte.validation;

import nl.belastingdienst.bte.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {

    private SchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SchemaValidator();
    }

    private ClientConfig createValidConfig() {
        ClientConfig config = new ClientConfig();
        config.setClientId("test-id");
        config.setClientName("Test Name");

        MetadataMapping mapping = new MetadataMapping();
        mapping.setObjectStore("TestStore");
        mapping.setDocumentClass("TestDoc");

        FieldMapping field = new FieldMapping();
        field.setSourceField("//test");
        field.setTargetProperty("testProp");
        field.setDataType("string");
        field.setRequired(true);
        mapping.setFields(List.of(field));

        config.setMetadataMapping(mapping);

        BusinessInfo bizInfo = new BusinessInfo();
        bizInfo.setContactEmail("test@example.com");
        bizInfo.setSupportGroup("TestGroup");
        config.setBusinessInfo(bizInfo);

        return config;
    }

    @Test
    void testValidConfig() {
        ValidationResult result = validator.validate(createValidConfig());
        assertTrue(result.isValid());
        assertEquals("SCHEMA", result.getLayer());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void testMissingClientId() {
        ClientConfig config = createValidConfig();
        config.setClientId(null);
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("clientId")));
    }

    @Test
    void testBlankClientId() {
        ClientConfig config = createValidConfig();
        config.setClientId("   ");
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("clientId")));
    }

    @Test
    void testMissingClientName() {
        ClientConfig config = createValidConfig();
        config.setClientName(null);
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("clientName")));
    }

    @Test
    void testBlankClientName() {
        ClientConfig config = createValidConfig();
        config.setClientName("");
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
    }

    @Test
    void testMissingMetadataMapping() {
        ClientConfig config = createValidConfig();
        config.setMetadataMapping(null);
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("metadataMapping")));
    }

    @Test
    void testMissingObjectStore() {
        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setObjectStore(null);
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("objectStore")));
    }

    @Test
    void testBlankObjectStore() {
        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setObjectStore("  ");
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
    }

    @Test
    void testMissingDocumentClass() {
        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setDocumentClass(null);
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("documentClass")));
    }

    @Test
    void testBlankDocumentClass() {
        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setDocumentClass("");
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
    }

    @Test
    void testMissingFields() {
        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setFields(null);
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("fields")));
    }

    @Test
    void testEmptyFields() {
        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setFields(new ArrayList<>());
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
    }

    @Test
    void testFieldMissingSourceField() {
        ClientConfig config = createValidConfig();
        config.getMetadataMapping().getFields().get(0).setSourceField(null);
        // Fields is immutable list from List.of, need mutable
        FieldMapping f = new FieldMapping();
        f.setTargetProperty("prop");
        f.setDataType("string");
        config.getMetadataMapping().setFields(List.of(f));

        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("sourceField")));
    }

    @Test
    void testFieldBlankSourceField() {
        FieldMapping f = new FieldMapping();
        f.setSourceField("  ");
        f.setTargetProperty("prop");
        f.setDataType("string");

        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setFields(List.of(f));

        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
    }

    @Test
    void testFieldMissingTargetProperty() {
        FieldMapping f = new FieldMapping();
        f.setSourceField("//field");
        f.setDataType("string");

        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setFields(List.of(f));

        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("targetProperty")));
    }

    @Test
    void testFieldBlankTargetProperty() {
        FieldMapping f = new FieldMapping();
        f.setSourceField("//field");
        f.setTargetProperty("");
        f.setDataType("string");

        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setFields(List.of(f));

        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
    }

    @Test
    void testFieldInvalidDataType() {
        FieldMapping f = new FieldMapping();
        f.setSourceField("//field");
        f.setTargetProperty("prop");
        f.setDataType("float");

        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setFields(List.of(f));

        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("VAL_002")));
    }

    @Test
    void testFieldValidDataTypes() {
        for (String type : List.of("string", "integer", "boolean", "date", "String", "INTEGER")) {
            FieldMapping f = new FieldMapping();
            f.setSourceField("//field");
            f.setTargetProperty("prop");
            f.setDataType(type);

            ClientConfig config = createValidConfig();
            config.getMetadataMapping().setFields(List.of(f));

            ValidationResult result = validator.validate(config);
            assertTrue(result.getErrors().stream().noneMatch(e -> e.getCode().equals("VAL_002")),
                "Type '" + type + "' should be valid");
        }
    }

    @Test
    void testFieldNullDataTypeIsValid() {
        FieldMapping f = new FieldMapping();
        f.setSourceField("//field");
        f.setTargetProperty("prop");
        f.setDataType(null);

        ClientConfig config = createValidConfig();
        config.getMetadataMapping().setFields(List.of(f));

        ValidationResult result = validator.validate(config);
        assertTrue(result.getErrors().stream().noneMatch(e -> e.getCode().equals("VAL_002")));
    }

    @Test
    void testMissingBusinessInfo() {
        ClientConfig config = createValidConfig();
        config.setBusinessInfo(null);
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("businessInfo")));
    }

    @Test
    void testMissingContactEmail() {
        ClientConfig config = createValidConfig();
        config.getBusinessInfo().setContactEmail(null);
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("contactEmail")));
    }

    @Test
    void testBlankContactEmail() {
        ClientConfig config = createValidConfig();
        config.getBusinessInfo().setContactEmail("   ");
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
    }

    @Test
    void testInvalidEmailFormat() {
        ClientConfig config = createValidConfig();
        config.getBusinessInfo().setContactEmail("not-an-email");
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("VAL_003")));
    }

    @Test
    void testValidEmailFormats() {
        for (String email : List.of("a@b.com", "user+tag@example.org", "test_user@domain.co.uk")) {
            ClientConfig config = createValidConfig();
            config.getBusinessInfo().setContactEmail(email);
            ValidationResult result = validator.validate(config);
            assertTrue(result.getErrors().stream().noneMatch(e -> e.getCode().equals("VAL_003")),
                "Email '" + email + "' should be valid");
        }
    }

    @Test
    void testMissingSupportGroup() {
        ClientConfig config = createValidConfig();
        config.getBusinessInfo().setSupportGroup(null);
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getMessage().contains("supportGroup")));
    }

    @Test
    void testBlankSupportGroup() {
        ClientConfig config = createValidConfig();
        config.getBusinessInfo().setSupportGroup("");
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
    }

    @Test
    void testMultipleErrors() {
        ClientConfig config = new ClientConfig();
        // Everything null
        ValidationResult result = validator.validate(config);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().size() >= 3); // clientId, clientName, metadataMapping, businessInfo
    }
}
