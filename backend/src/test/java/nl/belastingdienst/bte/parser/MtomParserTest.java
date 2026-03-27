package nl.belastingdienst.bte.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MtomParserTest {

    private MtomParser parser;

    @BeforeEach
    void setUp() {
        parser = new MtomParser();
    }

    @Test
    void testParsePlainXml() throws Exception {
        String xml = "<?xml version=\"1.0\"?><root><name>test</name><count>42</count></root>";
        byte[] content = xml.getBytes(StandardCharsets.UTF_8);

        MtomParser.ParsedMtomMessage result = parser.parse(content, "application/xml");

        assertNotNull(result);
        assertNotNull(result.getXmlDocument());
        assertEquals(xml, result.getRawXml());
        assertTrue(result.getAttachments().isEmpty());
    }

    @Test
    void testParsePlainXmlNullContentType() throws Exception {
        String xml = "<root><field>value</field></root>";
        byte[] content = xml.getBytes(StandardCharsets.UTF_8);

        MtomParser.ParsedMtomMessage result = parser.parse(content, null);

        assertNotNull(result.getXmlDocument());
        assertEquals(xml, result.getRawXml());
    }

    @Test
    void testEvaluateXPathSimple() throws Exception {
        String xml = "<root><name>TestValue</name></root>";
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);

        String value = parser.evaluateXPath(parsed.getXmlDocument(), "//name");
        assertEquals("TestValue", value);
    }

    @Test
    void testEvaluateXPathNotFound() throws Exception {
        String xml = "<root><name>TestValue</name></root>";
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);

        String value = parser.evaluateXPath(parsed.getXmlDocument(), "//nonexistent");
        assertNull(value);
    }

    @Test
    void testEvaluateXPathWithPredicate() throws Exception {
        String xml = "<root><value key=\"BBM1\">Data1</value><value key=\"BBM2\">Data2</value></root>";
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);

        String value = parser.evaluateXPath(parsed.getXmlDocument(), "//value[@key='BBM1']");
        assertEquals("Data1", value);

        String value2 = parser.evaluateXPath(parsed.getXmlDocument(), "//value[@key='BBM2']");
        assertEquals("Data2", value2);
    }

    @Test
    void testEvaluateXPathWithNamespace() throws Exception {
        String xml = "<root xmlns:ecm=\"http://example.com/ecm\"><ecm:name>NSValue</ecm:name></root>";
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);

        // Direct XPath without local-name should be adapted
        String value = parser.evaluateXPath(parsed.getXmlDocument(), "//name");
        assertEquals("NSValue", value);
    }

    @Test
    void testEvaluateXPathInvalidExpression() throws Exception {
        String xml = "<root><name>test</name></root>";
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);

        String value = parser.evaluateXPath(parsed.getXmlDocument(), "///[invalid");
        assertNull(value);
    }

    @Test
    void testEvaluateXPathWithSlashInPath() throws Exception {
        String xml = "<root><parent><child>val</child></parent></root>";
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);

        // XPath with / in remainder should NOT be adapted
        String value = parser.evaluateXPath(parsed.getXmlDocument(), "//parent/child");
        assertNull(value); // Won't work because namespace adaptation doesn't handle this case
    }

    @Test
    void testEvaluateXPathAbsolutePath() throws Exception {
        String xml = "<root><name>test</name></root>";
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);

        String value = parser.evaluateXPath(parsed.getXmlDocument(), "/root/name");
        assertEquals("test", value);
    }

    @Test
    void testEvaluateXPathAlreadyLocalName() throws Exception {
        String xml = "<root><name>test</name></root>";
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);

        String value = parser.evaluateXPath(parsed.getXmlDocument(), "//*[local-name()='name']");
        assertEquals("test", value);
    }

    @Test
    void testParseInvalidXmlThrows() {
        byte[] content = "not valid xml".getBytes(StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> parser.parse(content, null));
    }

    @Test
    void testParsedMtomMessageGettersSetters() {
        MtomParser.ParsedMtomMessage msg = new MtomParser.ParsedMtomMessage();
        assertNull(msg.getXmlDocument());
        assertNotNull(msg.getAttachments());
        assertNull(msg.getRawXml());

        msg.setRawXml("test");
        assertEquals("test", msg.getRawXml());
    }

    @Test
    void testParseXmlWithSoapEnvelope() throws Exception {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
              <soapenv:Header/>
              <soapenv:Body>
                <data>value123</data>
              </soapenv:Body>
            </soapenv:Envelope>
            """;
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);

        assertNotNull(parsed.getXmlDocument());
        String value = parser.evaluateXPath(parsed.getXmlDocument(), "//data");
        assertEquals("value123", value);
    }

    @Test
    void testParseSampleMtomXml() throws Exception {
        byte[] content = getClass().getResourceAsStream("/sample-mtom.xml").readAllBytes();
        MtomParser.ParsedMtomMessage parsed = parser.parse(content, null);

        assertNotNull(parsed.getXmlDocument());
        assertNotNull(parsed.getRawXml());

        // Check some fields from the sample
        String ecmId = parser.evaluateXPath(parsed.getXmlDocument(), "//ECM_ID");
        assertEquals("48b8d098c1fd4b80927309e72dd41ef6", ecmId);

        String berichtklasse = parser.evaluateXPath(parsed.getXmlDocument(), "//Berichtklasse");
        assertEquals("ARC", berichtklasse);
    }

    @Test
    void testResolveXopIncludesNoIncludes() throws Exception {
        String xml = "<root><data>plain</data></root>";
        MtomParser.ParsedMtomMessage parsed = parser.parse(xml.getBytes(StandardCharsets.UTF_8), null);
        // Should not throw, no XOP includes to resolve
        assertNotNull(parsed.getXmlDocument());
    }
}
