package com.cvnstack.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Waitlist;
import com.cvnstack.service.CacheStorage;
import com.cvnstack.service.WaitlistStorage;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Checks create-waitlist HTTP responses through the real service with mocked storage.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateWaitlistControllerTest {

  @LocalServerPort
  private int port;

  @MockitoBean
  private WaitlistStorage storage;

  @MockitoBean
  private CacheStorage cache;

  @AfterEach
  void doesNotUseCache() {
    verifyNoInteractions(cache);
  }

  @Test
  void validPostReturns200WithGeneratedIdAndSettings() {
    when(storage.saveWaitlist(any())).thenAnswer(invocation -> invocation.getArgument(0));

    String id = given().port(port).contentType("application/json")
        .body("""
            {"name":"Support","maxLength":10,"processingTimePerEntryMinutes":5}
            """)
        .when().post("/waitlists")
        .then().statusCode(200)
        .body("name", equalTo("Support"))
        .body("maxLength", equalTo(10))
        .body("processingTimePerEntryMinutes", equalTo(5))
        .body("$", not(hasKey("updatedAt")))
        .extract().path("id");

    assertEquals(id, UUID.fromString(id).toString());
    verify(storage).saveWaitlist(new Waitlist(id, "Support", 10, 5));
    verifyNoMoreInteractions(storage);
  }

  @Test
  void responseUsesPersistedSettings() {
    when(storage.saveWaitlist(any())).thenReturn(new Waitlist("stored-id", "Stored", 20, 7));

    given().port(port).contentType("application/json")
        .body("""
            {"name":"Support","maxLength":10,"processingTimePerEntryMinutes":5}
            """)
        .when().post("/waitlists")
        .then().statusCode(200)
        .body("id", equalTo("stored-id"))
        .body("name", equalTo("Stored"))
        .body("maxLength", equalTo(20))
        .body("processingTimePerEntryMinutes", equalTo(7));

    verify(storage).saveWaitlist(any());
    verifyNoMoreInteractions(storage);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{}",
      "{\"name\":null,\"maxLength\":10,\"processingTimePerEntryMinutes\":5}",
      "{\"name\":\" \",\"maxLength\":10,\"processingTimePerEntryMinutes\":5}",
      "{\"name\":\"Support\",\"maxLength\":0,\"processingTimePerEntryMinutes\":5}",
      "{\"name\":\"Support\",\"maxLength\":-1,\"processingTimePerEntryMinutes\":5}",
      "{\"name\":\"Support\",\"maxLength\":10,\"processingTimePerEntryMinutes\":0}",
      "{\"name\":\"Support\",\"maxLength\":10,\"processingTimePerEntryMinutes\":-1}"
  })
  void invalidSettingsReturn400WithoutPersistence(String body) {
    given().port(port).contentType("application/json").body(body)
        .when().post("/waitlists")
        .then().statusCode(400).body("error", equalTo("INVALID_REQUEST"));

    verifyNoInteractions(storage);
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "{", ""})
  void malformedOrMissingBodyReturns400WithoutPersistence(String body) {
    given().port(port).contentType("application/json").body(body)
        .when().post("/waitlists")
        .then().statusCode(400);

    verifyNoInteractions(storage);
  }

  @Test
  void duplicateNameReturns409() {
    when(storage.saveWaitlist(any()))
        .thenThrow(ApiException.conflict("Waitlist name already exists"));

    given().port(port).contentType("application/json")
        .body("""
            {"name":"Support","maxLength":10,"processingTimePerEntryMinutes":5}
            """)
        .when().post("/waitlists")
        .then().statusCode(409)
        .body("error", equalTo("CONFLICT"));

    verify(storage).saveWaitlist(any());
    verifyNoMoreInteractions(storage);
  }
}
