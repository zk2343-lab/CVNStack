package edu.columbia.cvnstack.health;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Checks that {@code GET /health} answers before waitlist routes exist.
 *
 * <p>AI-assisted: Cursor (Grok 4.7), 2026-10-16.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointTest {

  @LocalServerPort
  private int port;

  @BeforeEach
  void bindRestAssured() {
    RestAssured.port = port;
  }

  @Test
  void getHealthReturnsOk() {
    given().when().get("/health").then().statusCode(200);
  }
}
