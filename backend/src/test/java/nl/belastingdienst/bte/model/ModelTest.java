package nl.belastingdienst.bte.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    // --- ClientConfig ---

    @Test
    void testClientConfigGettersSetters() {
        ClientConfig config = new ClientConfig();
        config.setClientId("id-1");
        config.setClientName("Name-1");

        assertEquals("id-1", config.getClientId());
        assertEquals("Name-1", config.getClientName());
        assertNull(config.getMetadataMapping());
        assertNull(config.getBusinessInfo());
        assertNull(config.getProcessingRules());

        MetadataMapping mapping = new MetadataMapping();
        config.setMetadataMapping(mapping);
        assertSame(mapping, config.getMetadataMapping());

        BusinessInfo biz = new BusinessInfo();
        config.setBusinessInfo(biz);
        assertSame(biz, config.getBusinessInfo());

        ProcessingRules rules = new ProcessingRules();
        config.setProcessingRules(rules);
        assertSame(rules, config.getProcessingRules());
    }

    // --- MetadataMapping ---

    @Test
    void testMetadataMappingGettersSetters() {
        MetadataMapping mapping = new MetadataMapping();
        mapping.setObjectStore("OS1");
        mapping.setDocumentClass("DC1");
        mapping.setFields(new ArrayList<>());

        assertEquals("OS1", mapping.getObjectStore());
        assertEquals("DC1", mapping.getDocumentClass());
        assertNotNull(mapping.getFields());
        assertTrue(mapping.getFields().isEmpty());
    }

    // --- FieldMapping ---

    @Test
    void testFieldMappingGettersSetters() {
        FieldMapping f = new FieldMapping();
        f.setSourceField("//field");
        f.setTargetProperty("prop");
        f.setDataType("string");
        f.setRequired(true);
        f.setFormat("yyyy-MM-dd");
        f.setDefaultValue("default");
        f.setMultiValue(true);
        f.setTransformation("toUpper");

        assertEquals("//field", f.getSourceField());
        assertEquals("prop", f.getTargetProperty());
        assertEquals("string", f.getDataType());
        assertTrue(f.isRequired());
        assertEquals("yyyy-MM-dd", f.getFormat());
        assertEquals("default", f.getDefaultValue());
        assertTrue(f.isMultiValue());
        assertEquals("toUpper", f.getTransformation());
    }

    @Test
    void testFieldMappingDefaults() {
        FieldMapping f = new FieldMapping();
        assertFalse(f.isRequired());
        assertFalse(f.isMultiValue());
        assertNull(f.getSourceField());
        assertNull(f.getTargetProperty());
        assertNull(f.getDataType());
        assertNull(f.getFormat());
        assertNull(f.getDefaultValue());
        assertNull(f.getTransformation());
    }

    // --- BusinessInfo ---

    @Test
    void testBusinessInfoGettersSetters() {
        BusinessInfo biz = new BusinessInfo();
        biz.setContactEmail("a@b.com");
        biz.setSupportGroup("Group1");
        biz.setMaxMessageSize(1048576);
        biz.setMaxMessagesPerDay(1000);

        assertEquals("a@b.com", biz.getContactEmail());
        assertEquals("Group1", biz.getSupportGroup());
        assertEquals(1048576, biz.getMaxMessageSize());
        assertEquals(1000, biz.getMaxMessagesPerDay());
    }

    @Test
    void testBusinessInfoDefaults() {
        BusinessInfo biz = new BusinessInfo();
        assertNull(biz.getContactEmail());
        assertNull(biz.getSupportGroup());
        assertNull(biz.getMaxMessageSize());
        assertNull(biz.getMaxMessagesPerDay());
    }

    // --- ProcessingRules ---

    @Test
    void testProcessingRulesGettersSetters() {
        ProcessingRules rules = new ProcessingRules();
        rules.setRetentionDays(60);
        rules.setAutoRetryEnabled(false);
        rules.setProcessingEnabled(false);

        assertEquals(60, rules.getRetentionDays());
        assertFalse(rules.isAutoRetryEnabled());
        assertFalse(rules.isProcessingEnabled());
    }

    @Test
    void testProcessingRulesDefaults() {
        ProcessingRules rules = new ProcessingRules();
        assertEquals(30, rules.getRetentionDays());
        assertTrue(rules.isAutoRetryEnabled());
        assertTrue(rules.isProcessingEnabled());
    }

    // --- DetectedField ---

    @Test
    void testDetectedFieldConstructorAndGetters() {
        DetectedField f = new DetectedField("name", "//name", "value", "string");
        assertEquals("name", f.getFieldName());
        assertEquals("//name", f.getXpathExpression());
        assertEquals("value", f.getSampleValue());
        assertEquals("string", f.getDetectedType());
    }

    @Test
    void testDetectedFieldDefaultConstructor() {
        DetectedField f = new DetectedField();
        assertNull(f.getFieldName());
        assertNull(f.getXpathExpression());
        assertNull(f.getSampleValue());
        assertNull(f.getDetectedType());
    }

    @Test
    void testDetectedFieldSetters() {
        DetectedField f = new DetectedField();
        f.setFieldName("fn");
        f.setXpathExpression("//fn");
        f.setSampleValue("sv");
        f.setDetectedType("integer");

        assertEquals("fn", f.getFieldName());
        assertEquals("//fn", f.getXpathExpression());
        assertEquals("sv", f.getSampleValue());
        assertEquals("integer", f.getDetectedType());
    }

    // --- ConversionResult ---

    @Test
    void testConversionResultGettersSetters() {
        ConversionResult result = new ConversionResult();
        result.setSuccess(true);
        result.setJsonOutput(Map.of("key", "value"));
        result.setValidationResults(List.of(new ValidationResult("SCHEMA")));
        result.setExtractedFields(Map.of("field", "val"));

        assertTrue(result.isSuccess());
        assertEquals("value", result.getJsonOutput().get("key"));
        assertEquals(1, result.getValidationResults().size());
        assertEquals("val", result.getExtractedFields().get("field"));
    }

    @Test
    void testConversionResultDefaults() {
        ConversionResult result = new ConversionResult();
        assertFalse(result.isSuccess());
        assertNull(result.getJsonOutput());
        assertNull(result.getValidationResults());
        assertNull(result.getExtractedFields());
    }

    // --- ValidationResult ---

    @Test
    void testValidationResultInitialState() {
        ValidationResult result = new ValidationResult("SCHEMA");
        assertEquals("SCHEMA", result.getLayer());
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void testValidationResultAddError() {
        ValidationResult result = new ValidationResult("BUSINESS");
        result.addError("CODE1", "message", "suggestion");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("CODE1", result.getErrors().get(0).getCode());
        assertEquals("message", result.getErrors().get(0).getMessage());
        assertEquals("suggestion", result.getErrors().get(0).getSuggestion());
        assertEquals("ERROR", result.getErrors().get(0).getSeverity());
    }

    @Test
    void testValidationResultAddWarning() {
        ValidationResult result = new ValidationResult("REFERENCE");
        result.addWarning("WARN1", "warning msg", "warning suggestion");

        assertTrue(result.isValid()); // Warnings don't affect validity
        assertEquals(1, result.getWarnings().size());
        assertEquals("WARN1", result.getWarnings().get(0).getCode());
        assertEquals("WARNING", result.getWarnings().get(0).getSeverity());
    }

    @Test
    void testValidationResultMultipleErrors() {
        ValidationResult result = new ValidationResult("SCHEMA");
        result.addError("E1", "error1", "s1");
        result.addError("E2", "error2", "s2");
        result.addWarning("W1", "warn1", "sw1");

        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertEquals(1, result.getWarnings().size());
    }

    // --- ValidationError ---

    @Test
    void testValidationErrorConstructor() {
        ValidationError error = new ValidationError("C1", "msg", "sug", "ERROR");
        assertEquals("C1", error.getCode());
        assertEquals("msg", error.getMessage());
        assertEquals("sug", error.getSuggestion());
        assertEquals("ERROR", error.getSeverity());
    }

    @Test
    void testValidationErrorDefaultConstructor() {
        ValidationError error = new ValidationError();
        assertNull(error.getCode());
        assertNull(error.getMessage());
        assertNull(error.getSuggestion());
        assertNull(error.getSeverity());
    }

    @Test
    void testValidationErrorSetters() {
        ValidationError error = new ValidationError();
        error.setCode("C2");
        error.setMessage("msg2");
        error.setSuggestion("sug2");
        error.setSeverity("WARNING");

        assertEquals("C2", error.getCode());
        assertEquals("msg2", error.getMessage());
        assertEquals("sug2", error.getSuggestion());
        assertEquals("WARNING", error.getSeverity());
    }
}
