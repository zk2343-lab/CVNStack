package com.cvnstack.error;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.cvnstack.service.WaitlistStorage;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Checks each HTTP status mapped by {@link GlobalExceptionHandler}.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ErrorProbeController.class)
class GlobalExceptionHandlerTest {

  @MockitoBean
  private WaitlistStorage storage;

  @LocalServerPort
  private int port;

  @BeforeEach
  void bindRestAssured() {
    RestAssured.port = port;
  }

  @Test
  void invalidRequestReturns400() {
    given()
        .when()
        .get("/__test__/errors/invalid-request")
        .then()
        .statusCode(400)
        .body("error", equalTo(ErrorCode.INVALID_REQUEST))
        .body("message", equalTo("bad field"));
  }

  @Test
  void invalidStateTransitionReturns400() {
    given()
        .when()
        .get("/__test__/errors/invalid-state")
        .then()
        .statusCode(400)
        .body("error", equalTo(ErrorCode.INVALID_STATE_TRANSITION));
  }

  @Test
  void notFoundReturns404() {
    given()
        .when()
        .get("/__test__/errors/not-found")
        .then()
        .statusCode(404)
        .body("error", equalTo(ErrorCode.NOT_FOUND));
  }

  @Test
  void conflictReturns409() {
    given()
        .when()
        .get("/__test__/errors/conflict")
        .then()
        .statusCode(409)
        .body("error", equalTo(ErrorCode.CONFLICT));
  }

  @Test
  void capacityExceededReturns422() {
    given()
        .when()
        .get("/__test__/errors/capacity")
        .then()
        .statusCode(422)
        .body("error", equalTo(ErrorCode.CAPACITY_EXCEEDED));
  }
}
