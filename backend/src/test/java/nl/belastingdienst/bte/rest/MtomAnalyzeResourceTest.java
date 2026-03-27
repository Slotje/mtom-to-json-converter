package nl.belastingdienst.bte.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class MtomAnalyzeResourceTest {

    @Test
    void testAnalyzeSimpleXml() {
        String xml = "<root><name>John</name><age>30</age></root>";
        given()
            .contentType("application/octet-stream")
            .body(xml.getBytes())
            .when().post("/api/mtom/analyze")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("fields", hasSize(greaterThan(0)))
            .body("totalFields", greaterThan(0));
    }

    @Test
    void testAnalyzeInvalidContent() {
        given()
            .contentType("application/octet-stream")
            .body("not xml at all".getBytes())
            .when().post("/api/mtom/analyze")
            .then()
            .statusCode(400)
            .body("success", is(false))
            .body("error", containsString("Fout bij het analyseren"))
            .body("suggestion", containsString("geldig MTOM"));
    }

    @Test
    void testAnalyzeXmlWithAttributes() {
        String xml = "<root><item id=\"123\">text</item></root>";
        given()
            .contentType("application/octet-stream")
            .body(xml.getBytes())
            .when().post("/api/mtom/analyze")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("fields", hasSize(greaterThan(0)));
    }

    @Test
    void testAnalyzeXmlWithKeyValuePattern() {
        String xml = "<root><value key=\"BBM1\">Data1</value></root>";
        given()
            .contentType("application/octet-stream")
            .body(xml.getBytes())
            .when().post("/api/mtom/analyze")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("fields.fieldName", hasItem("BBM1"));
    }

    @Test
    void testAnalyzeNestedXml() {
        String xml = "<root><parent><child>val</child></parent></root>";
        given()
            .contentType("application/octet-stream")
            .body(xml.getBytes())
            .when().post("/api/mtom/analyze")
            .then()
            .statusCode(200)
            .body("success", is(true))
            .body("fields.fieldName", hasItem("child"));
    }
}
