package nl.belastingdienst.bte.converter;

import nl.belastingdienst.bte.model.*;
import nl.belastingdienst.bte.parser.MtomParser;
import nl.belastingdienst.bte.validation.BusinessRuleValidator;
import nl.belastingdienst.bte.validation.ReferenceValidator;
import nl.belastingdienst.bte.validation.SchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MtomToJsonConverterTest {

    private MtomToJsonConverter converter;

    @BeforeEach
    void setUp() throws Exception {
        converter = new MtomToJsonConverter();
        // Manually inject dependencies
        setField(converter, "mtomParser", new MtomParser());
        setField(converter, "schemaValidator", new SchemaValidator());
        setField(converter, "businessRuleValidator", new BusinessRuleValidator());
        setField(converter, "referenceValidator", new ReferenceValidator());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private ClientConfig createConfig(List<FieldMapping> fields) {
        ClientConfig config = new ClientConfig();
        config.setClientId("test-id");
        config.setClientName("Test Name");

        MetadataMapping mapping = new MetadataMapping();
        mapping.setObjectStore("TestStore");
        mapping.setDocumentClass("TestDoc");
        mapping.setFields(fields);
        config.setMetadataMapping(mapping);

        BusinessInfo biz = new BusinessInfo();
        biz.setContactEmail("test@example.com");
        biz.setSupportGroup("TestGroup");
        config.setBusinessInfo(biz);

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
    void testConvertSimpleXml() {
        String xml = "<root><name>TestValue</name><count>42</count></root>";
        FieldMapping f1 = createField("//name", "docName", "string", true);
        FieldMapping f2 = createField("//count", "docCount", "integer", false);

        // Need 11 fields to avoid warning
        var fields = new java.util.ArrayList<>(List.of(f1, f2));
        for (int i = 0; i < 9; i++) {
            fields.add(createField("//extra" + i, "extra" + i, "string", false));
        }

        ClientConfig config = createConfig(fields);

        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);

        assertNotNull(result);
        assertNotNull(result.getJsonOutput());
        assertEquals("test-id", result.getJsonOutput().get("clientId"));
        assertEquals("Test Name", result.getJsonOutput().get("clientName"));
        assertEquals("TestStore", result.getJsonOutput().get("objectStore"));
        assertEquals("TestDoc", result.getJsonOutput().get("documentClass"));

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getJsonOutput().get("metadata");
        assertNotNull(metadata);
        assertEquals("TestValue", metadata.get("docName"));
        assertEquals(42L, metadata.get("docCount"));
    }

    @Test
    void testConvertWithExtractedFields() {
        String xml = "<root><name>TestValue</name></root>";
        FieldMapping f = createField("//name", "docName", "string", true);

        var fields = new java.util.ArrayList<>(List.of(f));
        for (int i = 0; i < 10; i++) {
            fields.add(createField("//f" + i, "p" + i, "string", false));
        }

        ClientConfig config = createConfig(fields);
        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);

        assertNotNull(result.getExtractedFields());
        assertEquals("TestValue", result.getExtractedFields().get("//name"));
    }

    @Test
    void testConvertFieldNotFound() {
        String xml = "<root><other>val</other></root>";
        FieldMapping f = createField("//missing", "missingProp", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);
        assertEquals("(niet gevonden)", result.getExtractedFields().get("//missing"));
    }

    @Test
    void testConvertFieldNotFoundWithDefault() {
        String xml = "<root><other>val</other></root>";
        FieldMapping f = createField("//missing", "missingProp", "string", false);
        f.setDefaultValue("defaultVal");
        ClientConfig config = createConfig(List.of(f));

        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);
        assertEquals("(default) defaultVal", result.getExtractedFields().get("//missing"));

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getJsonOutput().get("metadata");
        assertEquals("defaultVal", metadata.get("missingProp"));
    }

    @Test
    void testConvertValueTypes() {
        String xml = "<root><str>text</str><num>123</num><bool>true</bool><date>2026-01-01</date></root>";
        FieldMapping f1 = createField("//str", "strProp", "string", false);
        FieldMapping f2 = createField("//num", "numProp", "integer", false);
        FieldMapping f3 = createField("//bool", "boolProp", "boolean", false);
        FieldMapping f4 = createField("//date", "dateProp", "date", false);

        ClientConfig config = createConfig(List.of(f1, f2, f3, f4));
        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getJsonOutput().get("metadata");
        assertEquals("text", metadata.get("strProp"));
        assertEquals(123L, metadata.get("numProp"));
        assertEquals(true, metadata.get("boolProp"));
        assertEquals("2026-01-01", metadata.get("dateProp"));
    }

    @Test
    void testConvertIntegerParseError() {
        String xml = "<root><num>not-a-number</num></root>";
        FieldMapping f = createField("//num", "numProp", "integer", false);
        ClientConfig config = createConfig(List.of(f));

        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getJsonOutput().get("metadata");
        // Should fall back to string value
        assertEquals("not-a-number", metadata.get("numProp"));
    }

    @Test
    void testConvertNullDataType() {
        String xml = "<root><val>text</val></root>";
        FieldMapping f = createField("//val", "valProp", null, false);
        ClientConfig config = createConfig(List.of(f));

        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getJsonOutput().get("metadata");
        assertEquals("text", metadata.get("valProp"));
    }

    @Test
    void testConvertHasValidationResults() {
        String xml = "<root><name>test</name></root>";
        FieldMapping f = createField("//name", "docName", "string", true);
        ClientConfig config = createConfig(List.of(f));

        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);

        assertNotNull(result.getValidationResults());
        assertEquals(3, result.getValidationResults().size());
        assertEquals("SCHEMA", result.getValidationResults().get(0).getLayer());
        assertEquals("BUSINESS", result.getValidationResults().get(1).getLayer());
        assertEquals("REFERENCE", result.getValidationResults().get(2).getLayer());
    }

    @Test
    void testConvertSuccessWhenAllValid() {
        String xml = "<root><name>test</name></root>";

        var fields = new java.util.ArrayList<FieldMapping>();
        FieldMapping f = createField("//name", "docName", "string", true);
        fields.add(f);
        for (int i = 0; i < 10; i++) {
            fields.add(createField("//f" + i, "p" + i, "string", false));
        }

        ClientConfig config = createConfig(fields);
        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);

        assertTrue(result.isSuccess());
    }

    @Test
    void testConvertProcessedAtPresent() {
        String xml = "<root><name>test</name></root>";
        FieldMapping f = createField("//name", "docName", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);

        assertTrue(result.getJsonOutput().containsKey("processedAt"));
        assertNotNull(result.getJsonOutput().get("processedAt"));
    }

    @Test
    void testConvertInvalidXmlReturnsError() {
        byte[] invalidXml = "not xml at all".getBytes(StandardCharsets.UTF_8);
        FieldMapping f = createField("//name", "docName", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ConversionResult result = converter.convert(invalidXml, null, config);

        assertFalse(result.isSuccess());
        assertNotNull(result.getValidationResults());
        assertTrue(result.getValidationResults().stream()
            .anyMatch(v -> v.getErrors().stream()
                .anyMatch(e -> e.getCode().equals("PARSE_001"))));
    }

    @Test
    void testConvertWithSampleMtomXml() {
        byte[] content;
        try {
            content = getClass().getResourceAsStream("/sample-mtom.xml").readAllBytes();
        } catch (Exception e) {
            fail("Could not read sample-mtom.xml");
            return;
        }

        FieldMapping f1 = createField("//ECM_ID", "ecmId", "string", true);
        FieldMapping f2 = createField("//Berichtklasse", "berichtklasse", "string", false);

        var fields = new java.util.ArrayList<>(List.of(f1, f2));
        for (int i = 0; i < 9; i++) {
            fields.add(createField("//f" + i, "p" + i, "string", false));
        }

        ClientConfig config = createConfig(fields);
        ConversionResult result = converter.convert(content, null, config);

        assertNotNull(result.getJsonOutput());
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getJsonOutput().get("metadata");
        assertEquals("48b8d098c1fd4b80927309e72dd41ef6", metadata.get("ecmId"));
        assertEquals("ARC", metadata.get("berichtklasse"));
    }

    @Test
    void testConvertBooleanFalseValue() {
        String xml = "<root><flag>false</flag></root>";
        FieldMapping f = createField("//flag", "flagProp", "boolean", false);
        ClientConfig config = createConfig(List.of(f));

        ConversionResult result = converter.convert(xml.getBytes(StandardCharsets.UTF_8), null, config);

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) result.getJsonOutput().get("metadata");
        assertEquals(false, metadata.get("flagProp"));
    }
}
