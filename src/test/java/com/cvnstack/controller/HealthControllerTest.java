package com.cvnstack.controller;

import static io.restassured.RestAssured.given;

import com.cvnstack.service.WaitlistStorage;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Checks that {@code GET /health} answers before waitlist routes exist.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerTest {

  @MockitoBean
  private WaitlistStorage storage;

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
