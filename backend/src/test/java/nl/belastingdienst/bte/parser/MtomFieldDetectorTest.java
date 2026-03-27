package nl.belastingdienst.bte.parser;

import nl.belastingdienst.bte.model.DetectedField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MtomFieldDetectorTest {

    private MtomFieldDetector detector;

    @BeforeEach
    void setUp() {
        detector = new MtomFieldDetector();
        // Manually inject the parser since we're not using CDI
        try {
            var field = MtomFieldDetector.class.getDeclaredField("mtomParser");
            field.setAccessible(true);
            field.set(detector, new MtomParser());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDetectSimpleFields() throws Exception {
        String xml = "<root><name>John</name><age>30</age></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        assertTrue(fields.stream().anyMatch(f -> f.getFieldName().equals("name") && f.getSampleValue().equals("John")));
        assertTrue(fields.stream().anyMatch(f -> f.getFieldName().equals("age") && f.getSampleValue().equals("30")));
    }

    @Test
    void testDetectKeyValuePattern() throws Exception {
        String xml = "<root><value key=\"BBM1\">Data1</value></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        assertTrue(fields.stream().anyMatch(f ->
            f.getFieldName().equals("BBM1") &&
            f.getXpathExpression().equals("//value[@key='BBM1']") &&
            f.getSampleValue().equals("Data1")
        ));
    }

    @Test
    void testDetectAttributes() throws Exception {
        String xml = "<root><item id=\"123\">text</item></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        assertTrue(fields.stream().anyMatch(f ->
            f.getFieldName().equals("item@id") &&
            f.getXpathExpression().equals("//item/@id") &&
            f.getSampleValue().equals("123")
        ));
    }

    @Test
    void testDetectDataTypeString() throws Exception {
        String xml = "<root><val>hello world</val></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        DetectedField f = fields.stream().filter(d -> d.getFieldName().equals("val")).findFirst().orElse(null);
        assertNotNull(f);
        assertEquals("string", f.getDetectedType());
    }

    @Test
    void testDetectDataTypeInteger() throws Exception {
        String xml = "<root><count>42</count></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        DetectedField f = fields.stream().filter(d -> d.getFieldName().equals("count")).findFirst().orElse(null);
        assertNotNull(f);
        assertEquals("integer", f.getDetectedType());
    }

    @Test
    void testDetectDataTypeBoolean() throws Exception {
        String xml = "<root><flag>true</flag></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        DetectedField f = fields.stream().filter(d -> d.getFieldName().equals("flag")).findFirst().orElse(null);
        assertNotNull(f);
        assertEquals("boolean", f.getDetectedType());
    }

    @Test
    void testDetectDataTypeBooleanFalse() throws Exception {
        String xml = "<root><flag>FALSE</flag></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        DetectedField f = fields.stream().filter(d -> d.getFieldName().equals("flag")).findFirst().orElse(null);
        assertNotNull(f);
        assertEquals("boolean", f.getDetectedType());
    }

    @Test
    void testDetectDataTypeDateIso() throws Exception {
        String xml = "<root><date>2026-01-15</date></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        DetectedField f = fields.stream().filter(d -> d.getFieldName().equals("date")).findFirst().orElse(null);
        assertNotNull(f);
        assertEquals("date", f.getDetectedType());
    }

    @Test
    void testDetectDataTypeDateWithTime() throws Exception {
        String xml = "<root><date>2026-01-15T10:30:00Z</date></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        DetectedField f = fields.stream().filter(d -> d.getFieldName().equals("date")).findFirst().orElse(null);
        assertNotNull(f);
        assertEquals("date", f.getDetectedType());
    }

    @Test
    void testDetectDataTypeDateDutchFormat() throws Exception {
        String xml = "<root><date>15-01-2026</date></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        DetectedField f = fields.stream().filter(d -> d.getFieldName().equals("date")).findFirst().orElse(null);
        assertNotNull(f);
        assertEquals("date", f.getDetectedType());
    }

    @Test
    void testNestedElements() throws Exception {
        String xml = "<root><parent><child>val</child></parent></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        assertTrue(fields.stream().anyMatch(f -> f.getFieldName().equals("child") && f.getSampleValue().equals("val")));
    }

    @Test
    void testSkipXmlnsAttributes() throws Exception {
        String xml = "<root xmlns:ns=\"http://example.com\"><ns:item>val</ns:item></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        assertFalse(fields.stream().anyMatch(f -> f.getFieldName().contains("xmlns")));
    }

    @Test
    void testSkipKeyAttribute() throws Exception {
        String xml = "<root><value key=\"BBM1\">Data1</value></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        // Key attribute should be used for field name, not listed as separate attribute
        assertFalse(fields.stream().anyMatch(f -> f.getFieldName().equals("value@key")));
    }

    @Test
    void testElementWithChildrenNotLeaf() throws Exception {
        String xml = "<root><parent><child>val</child></parent></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        // parent has child elements, so it should NOT be detected as a leaf field
        assertFalse(fields.stream().anyMatch(f -> f.getFieldName().equals("parent")));
    }

    @Test
    void testEmptyTextContentSkipped() throws Exception {
        String xml = "<root><empty></empty></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);

        assertFalse(fields.stream().anyMatch(f -> f.getFieldName().equals("empty")));
    }

    @Test
    void testSampleMtomXml() throws Exception {
        byte[] content = getClass().getResourceAsStream("/sample-mtom.xml").readAllBytes();
        List<DetectedField> fields = detector.detectFields(content, null);

        assertFalse(fields.isEmpty());
        // Should detect ECM_ID
        assertTrue(fields.stream().anyMatch(f -> f.getFieldName().equals("ECM_ID")));
        // Should detect Berichtklasse
        assertTrue(fields.stream().anyMatch(f -> f.getFieldName().equals("Berichtklasse")));
    }

    @Test
    void testDetectDataTypeNullOrEmpty() throws Exception {
        // Test the detectDataType method with edge cases via attribute with empty value
        String xml = "<root><item attr=\"\">text</item></root>";
        List<DetectedField> fields = detector.detectFields(xml.getBytes(StandardCharsets.UTF_8), null);
        // Empty attribute value should be detected as "string"
        DetectedField attrField = fields.stream()
            .filter(f -> f.getFieldName().equals("item@attr"))
            .findFirst().orElse(null);
        assertNotNull(attrField);
        assertEquals("string", attrField.getDetectedType());
    }
}
