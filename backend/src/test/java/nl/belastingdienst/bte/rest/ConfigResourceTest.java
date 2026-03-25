package nl.belastingdienst.bte.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class ConfigResourceTest {
    @Test
    void testGetConfigWithoutUpload() {
        given()
          .when().get("/api/config")
          .then()
             .statusCode(404);
    }
}
