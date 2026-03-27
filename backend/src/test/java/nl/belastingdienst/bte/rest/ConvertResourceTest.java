package nl.belastingdienst.bte.rest;

import io.quarkus.test.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConvertResourceTest {

    @Test
    @Order(1)
    void testConvertWithoutConfig() {
        given()
            .contentType("application/octet-stream")
            .body("<root/>".getBytes())
            .when().post("/api/convert")
            .then()
            .statusCode(400)
            .body("error", containsString("Geen configuratie geladen"));
    }

    @Test
    @Order(2)
    void testValidateWithoutConfig() {
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/validate")
            .then()
            .statusCode(400)
            .body("error", containsString("Geen configuratie geladen"));
    }

    @Test
    @Order(3)
    void testConvertAfterConfigUpload() {
        // First upload config
        String yaml = """
            clientId: test-id
            clientName: Test Client
            metadataMapping:
              objectStore: TestStore
              documentClass: TestDoc
              fields:
                - sourceField: //name
                  targetProperty: docName
                  dataType: string
                  required: true
            businessInfo:
              contactEmail: test@example.com
              supportGroup: TestGroup
            """;

        given()
            .contentType("text/plain")
            .body(yaml)
            .when().post("/api/config/upload-text")
            .then()
            .statusCode(200);

        // Then convert
        String xml = "<root><name>TestValue</name></root>";
        given()
            .contentType("application/octet-stream")
            .body(xml.getBytes())
            .when().post("/api/convert")
            .then()
            .statusCode(200)
            .body("jsonOutput.clientId", equalTo("test-id"))
            .body("jsonOutput.metadata.docName", equalTo("TestValue"))
            .body("validationResults", hasSize(3));
    }

    @Test
    @Order(4)
    void testValidateAfterConfigUpload() {
        // Config is already loaded from previous test
        given()
            .contentType(ContentType.JSON)
            .when().post("/api/validate")
            .then()
            .statusCode(200)
            .body("validationResults", hasSize(2))
            .body("valid", notNullValue());
    }
}
