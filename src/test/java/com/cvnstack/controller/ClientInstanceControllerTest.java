package com.cvnstack.controller;

import static com.cvnstack.ClientInstanceRequests.HEADER;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verifyNoInteractions;

import com.cvnstack.service.CacheStorage;
import com.cvnstack.service.WaitlistStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Checks that mutating routes reject a missing or blank {@code X-Client-Instance}.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClientInstanceControllerTest {

  @LocalServerPort
  private int port;

  @MockitoBean
  private WaitlistStorage storage;

  @MockitoBean
  private CacheStorage cache;

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void createWaitlistRejectsMissingOrBlankHeader(String header) {
    var request = given().port(port).contentType("application/json")
        .body("""
            {"name":"Support","maxLength":10,"processingTimePerEntryMinutes":5}
            """);
    if (header != null) {
      request.header(HEADER, header);
    }
    request.when().post("/waitlists")
        .then().statusCode(400)
        .body("error", equalTo("INVALID_REQUEST"));
    verifyNoInteractions(storage, cache);
  }

  @Test
  void createEntryRejectsMissingHeader() {
    given().port(port).contentType("application/json").body("{\"name\":\"Alice\"}")
        .when().post("/waitlists/wl-1/entries")
        .then().statusCode(400)
        .body("error", equalTo("INVALID_REQUEST"));
    verifyNoInteractions(storage, cache);
  }

  @Test
  void patchEntryRejectsMissingHeader() {
    given().port(port).contentType("application/json").body("{\"priority\":5}")
        .when().patch("/waitlists/wl/entries/e")
        .then().statusCode(400)
        .body("error", equalTo("INVALID_REQUEST"));
    verifyNoInteractions(storage, cache);
  }

  @Test
  void deleteEntryRejectsMissingHeader() {
    given().port(port).when().delete("/waitlists/wl-1/entries/e-1")
        .then().statusCode(400)
        .body("error", equalTo("INVALID_REQUEST"));
    verifyNoInteractions(storage, cache);
  }

  @Test
  void serveNextRejectsMissingHeader() {
    given().port(port).when().post("/waitlists/wl-1/serve-next")
        .then().statusCode(400)
        .body("error", equalTo("INVALID_REQUEST"));
    verifyNoInteractions(storage, cache);
  }
}
