package nl.belastingdienst.bte.validation;

import nl.belastingdienst.bte.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceValidatorTest {

    private ReferenceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ReferenceValidator();
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private ClientConfig createConfig(List<FieldMapping> fields) {
        ClientConfig config = new ClientConfig();
        MetadataMapping mapping = new MetadataMapping();
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
    void testValidXPathFindsValue() throws Exception {
        Document doc = parseXml("<root><name>test</name></root>");
        FieldMapping f = createField("//name", "nameField", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void testXPathNoResultRequiredNoDefault() throws Exception {
        Document doc = parseXml("<root><other>val</other></root>");
        FieldMapping f = createField("//missing", "missingField", "string", true);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        // Should get a warning
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.getMessage().contains("missing")));
    }

    @Test
    void testXPathNoResultRequiredWithDefault() throws Exception {
        Document doc = parseXml("<root><other>val</other></root>");
        FieldMapping f = createField("//missing", "missingField", "string", true);
        f.setDefaultValue("default");
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        // No warning because default exists
        assertTrue(result.getWarnings().stream().noneMatch(w -> w.getMessage().contains("missing")));
    }

    @Test
    void testXPathNoResultNotRequired() throws Exception {
        Document doc = parseXml("<root><other>val</other></root>");
        FieldMapping f = createField("//missing", "missingField", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.getWarnings().stream().noneMatch(w -> w.getMessage().contains("missing")));
    }

    @Test
    void testInvalidXPathSyntax() throws Exception {
        Document doc = parseXml("<root><name>test</name></root>");
        FieldMapping f = createField("///[invalid", "field", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("VAL_005")));
    }

    @Test
    void testTypeCompatibilityIntegerValid() throws Exception {
        Document doc = parseXml("<root><count>42</count></root>");
        FieldMapping f = createField("//count", "countField", "integer", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
    }

    @Test
    void testTypeCompatibilityIntegerInvalid() throws Exception {
        Document doc = parseXml("<root><count>not-a-number</count></root>");
        FieldMapping f = createField("//count", "countField", "integer", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("VAL_002")));
    }

    @Test
    void testTypeCompatibilityBooleanValid() throws Exception {
        Document doc = parseXml("<root><flag>true</flag></root>");
        FieldMapping f = createField("//flag", "flagField", "boolean", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
    }

    @Test
    void testTypeCompatibilityBooleanValidFalse() throws Exception {
        Document doc = parseXml("<root><flag>FALSE</flag></root>");
        FieldMapping f = createField("//flag", "flagField", "boolean", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
    }

    @Test
    void testTypeCompatibilityBooleanInvalid() throws Exception {
        Document doc = parseXml("<root><flag>yes</flag></root>");
        FieldMapping f = createField("//flag", "flagField", "boolean", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.getCode().equals("VAL_002")));
    }

    @Test
    void testTypeCompatibilityDateValidIso() throws Exception {
        Document doc = parseXml("<root><date>2026-01-15</date></root>");
        FieldMapping f = createField("//date", "dateField", "date", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.getWarnings().stream().noneMatch(w -> w.getMessage().contains("datum")));
    }

    @Test
    void testTypeCompatibilityDateValidDutch() throws Exception {
        Document doc = parseXml("<root><date>15-01-2026</date></root>");
        FieldMapping f = createField("//date", "dateField", "date", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.getWarnings().stream().noneMatch(w -> w.getMessage().contains("datum")));
    }

    @Test
    void testTypeCompatibilityDateWithTime() throws Exception {
        Document doc = parseXml("<root><date>2026-01-15T10:30:00Z</date></root>");
        FieldMapping f = createField("//date", "dateField", "date", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.getWarnings().stream().noneMatch(w -> w.getMessage().contains("datum")));
    }

    @Test
    void testTypeCompatibilityDateInvalid() throws Exception {
        Document doc = parseXml("<root><date>not-a-date</date></root>");
        FieldMapping f = createField("//date", "dateField", "date", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.getMessage().contains("datum")));
    }

    @Test
    void testTypeCompatibilityStringAlwaysOk() throws Exception {
        Document doc = parseXml("<root><val>anything</val></root>");
        FieldMapping f = createField("//val", "valField", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void testNullDocument() {
        FieldMapping f = createField("//field", "prop", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, null);
        assertTrue(result.isValid());
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.getMessage().contains("Geen XML document")));
    }

    @Test
    void testNullMetadataMapping() throws Exception {
        Document doc = parseXml("<root/>");
        ClientConfig config = new ClientConfig();
        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
    }

    @Test
    void testNullFields() throws Exception {
        Document doc = parseXml("<root/>");
        ClientConfig config = new ClientConfig();
        MetadataMapping m = new MetadataMapping();
        m.setFields(null);
        config.setMetadataMapping(m);

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
    }

    @Test
    void testNullSourceFieldSkipped() throws Exception {
        Document doc = parseXml("<root><val>x</val></root>");
        FieldMapping f = createField(null, "prop", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
    }

    @Test
    void testXPathWithPredicate() throws Exception {
        Document doc = parseXml("<root><value key=\"BBM1\">testval</value></root>");
        FieldMapping f = createField("//value[@key='BBM1']", "bbm1", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
    }

    @Test
    void testXPathWithSlash() throws Exception {
        Document doc = parseXml("<root><a><b>val</b></a></root>");
        FieldMapping f = createField("//a/b", "field", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        // XPath with slash is not adapted, used as-is
        assertTrue(result.isValid());
    }

    @Test
    void testXPathAlreadyUsingLocalName() throws Exception {
        Document doc = parseXml("<root><name>test</name></root>");
        FieldMapping f = createField("//*[local-name()='name']", "field", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
    }

    @Test
    void testXPathNotStartingWithDoubleSlash() throws Exception {
        Document doc = parseXml("<root><name>test</name></root>");
        FieldMapping f = createField("/root/name", "field", "string", false);
        ClientConfig config = createConfig(List.of(f));

        ValidationResult result = validator.validate(config, doc);
        assertTrue(result.isValid());
    }
}
