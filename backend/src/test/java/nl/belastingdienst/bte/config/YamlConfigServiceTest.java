package nl.belastingdienst.bte.config;

import nl.belastingdienst.bte.model.ClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class YamlConfigServiceTest {

    private YamlConfigService service;

    @BeforeEach
    void setUp() {
        service = new YamlConfigService();
    }

    @Test
    void testHasConfigInitiallyFalse() {
        assertFalse(service.hasConfig());
        assertNull(service.getCurrentConfig());
    }

    @Test
    void testLoadConfigFromInputStream() {
        String yaml = """
            clientId: test-123
            clientName: Test Client
            metadataMapping:
              objectStore: TestStore
              documentClass: TestDoc
              fields:
                - sourceField: //field1
                  targetProperty: prop1
                  dataType: string
                  required: true
            businessInfo:
              contactEmail: test@example.com
              supportGroup: TestGroup
            """;
        InputStream stream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        ClientConfig config = service.loadConfig(stream);

        assertNotNull(config);
        assertEquals("test-123", config.getClientId());
        assertEquals("Test Client", config.getClientName());
        assertTrue(service.hasConfig());
        assertSame(config, service.getCurrentConfig());
    }

    @Test
    void testLoadConfigFromString() {
        String yaml = """
            clientId: string-123
            clientName: String Client
            metadataMapping:
              objectStore: Store1
              documentClass: Doc1
              fields: []
            businessInfo:
              contactEmail: a@b.com
              supportGroup: Group1
            """;
        ClientConfig config = service.loadConfigFromString(yaml);

        assertNotNull(config);
        assertEquals("string-123", config.getClientId());
        assertEquals("String Client", config.getClientName());
        assertTrue(service.hasConfig());
        assertSame(config, service.getCurrentConfig());
    }

    @Test
    void testLoadConfigOverwritesPrevious() {
        String yaml1 = "clientId: first\nclientName: First";
        String yaml2 = "clientId: second\nclientName: Second";

        service.loadConfigFromString(yaml1);
        assertEquals("first", service.getCurrentConfig().getClientId());

        service.loadConfigFromString(yaml2);
        assertEquals("second", service.getCurrentConfig().getClientId());
    }

    @Test
    void testLoadConfigWithMetadataMapping() {
        String yaml = """
            clientId: c1
            clientName: C1
            metadataMapping:
              objectStore: OS1
              documentClass: DC1
              fields:
                - sourceField: //test
                  targetProperty: testProp
                  dataType: integer
                  required: true
                  format: null
                  defaultValue: "42"
            businessInfo:
              contactEmail: x@y.com
              supportGroup: SG
            processingRules:
              retentionDays: 60
              autoRetryEnabled: false
              processingEnabled: true
            """;
        ClientConfig config = service.loadConfigFromString(yaml);

        assertNotNull(config.getMetadataMapping());
        assertEquals("OS1", config.getMetadataMapping().getObjectStore());
        assertEquals("DC1", config.getMetadataMapping().getDocumentClass());
        assertEquals(1, config.getMetadataMapping().getFields().size());
        assertEquals("//test", config.getMetadataMapping().getFields().get(0).getSourceField());
        assertEquals("testProp", config.getMetadataMapping().getFields().get(0).getTargetProperty());
        assertEquals("integer", config.getMetadataMapping().getFields().get(0).getDataType());
        assertTrue(config.getMetadataMapping().getFields().get(0).isRequired());
        assertEquals("42", config.getMetadataMapping().getFields().get(0).getDefaultValue());

        assertNotNull(config.getBusinessInfo());
        assertEquals("x@y.com", config.getBusinessInfo().getContactEmail());
        assertEquals("SG", config.getBusinessInfo().getSupportGroup());

        assertNotNull(config.getProcessingRules());
        assertEquals(60, config.getProcessingRules().getRetentionDays());
        assertFalse(config.getProcessingRules().isAutoRetryEnabled());
        assertTrue(config.getProcessingRules().isProcessingEnabled());
    }
}
