package nl.belastingdienst.bte.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfigResourceTest {

    @Test
    @Order(1)
    void testGetConfigWithoutUpload() {
        given()
            .when().get("/api/config")
            .then()
            .statusCode(404)
            .body("error", containsString("Geen configuratie geladen"))
            .body("suggestion", notNullValue());
    }

    @Test
    @Order(2)
    void testUploadConfigText() {
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
            .statusCode(200)
            .body("success", is(true))
            .body("config.clientId", equalTo("test-id"))
            .body("config.clientName", equalTo("Test Client"))
            .body("validation.layer", equalTo("SCHEMA"))
            .body("validation.valid", is(true));
    }

    @Test
    @Order(3)
    void testGetConfigAfterUpload() {
        given()
            .when().get("/api/config")
            .then()
            .statusCode(200)
            .body("clientId", equalTo("test-id"))
            .body("clientName", equalTo("Test Client"));
    }

    @Test
    @Order(4)
    void testUploadConfigBinary() {
        String yaml = """
            clientId: binary-id
            clientName: Binary Client
            metadataMapping:
              objectStore: BinStore
              documentClass: BinDoc
              fields:
                - sourceField: //field
                  targetProperty: prop
                  dataType: string
                  required: false
            businessInfo:
              contactEmail: bin@example.com
              supportGroup: BinGroup
            """;

        given()
            .contentType("application/octet-stream")
            .body(yaml.getBytes())
            .when().post("/api/config/upload")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("config.clientId", equalTo("binary-id"));
    }

    @Test
    @Order(5)
    void testUploadConfigTextInvalidYaml() {
        String invalidYaml = "{ invalid: yaml: [broken";

        given()
            .contentType("text/plain")
            .body(invalidYaml)
            .when().post("/api/config/upload-text")
            .then()
            .statusCode(400)
            .body("success", is(false))
            .body("error", containsString("Fout bij het laden"))
            .body("suggestion", containsString("YAML syntax"));
    }

    @Test
    @Order(6)
    void testUploadConfigBinaryInvalidYaml() {
        String invalidYaml = "{ invalid: yaml: [broken";

        given()
            .contentType("application/octet-stream")
            .body(invalidYaml.getBytes())
            .when().post("/api/config/upload")
            .then()
            .statusCode(400)
            .body("success", is(false))
            .body("error", containsString("Fout bij het laden"))
            .body("suggestion", containsString("YAML syntax"));
    }

    @Test
    @Order(7)
    void testUploadConfigWithValidationErrors() {
        String yaml = """
            clientId: ""
            clientName: ""
            """;

        given()
            .contentType("text/plain")
            .body(yaml)
            .when().post("/api/config/upload-text")
            .then()
            .statusCode(200)
            .body("success", is(false))
            .body("validation.valid", is(false))
            .body("validation.errors", hasSize(greaterThan(0)));
    }
}
