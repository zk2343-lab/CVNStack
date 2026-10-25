package com.cvnstack.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cvnstack.model.Waitlist;
import com.cvnstack.service.CacheStorage;
import com.cvnstack.service.WaitlistStorage;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Checks DELETE requests through the real controller and service with mocked storage.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeleteEntryControllerTest {

  @LocalServerPort
  private int port;

  @MockitoBean
  private WaitlistStorage storage;

  @MockitoBean
  private CacheStorage cache;

  @Test
  void successfulDeleteReturns200AndDedicatedResponse() {
    existingWaitlist();
    when(storage.deleteEntry("wl-1", "e-1")).thenReturn(true);

    given().port(port).when().delete("/waitlists/wl-1/entries/e-1")
        .then().statusCode(200)
        .body("message", equalTo("Entry successfully removed"))
        .body("entryId", equalTo("e-1"))
        .body("waitlistId", equalTo("wl-1"))
        .body("size()", equalTo(3));

    verify(storage).findWaitlistById("wl-1");
    verify(storage).deleteEntry("wl-1", "e-1");
  }

  @Test
  void unknownWaitlistReturns404WithoutDelete() {
    when(storage.findWaitlistById("missing")).thenReturn(Optional.empty());

    given().port(port).when().delete("/waitlists/missing/entries/e-1")
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"))
        .body("message", equalTo("Waitlist does not exist"));

    verify(storage, never()).deleteEntry(anyString(), anyString());
  }

  @Test
  void unknownEntryReturns404() {
    existingWaitlist();
    when(storage.deleteEntry("wl-1", "missing")).thenReturn(false);

    given().port(port).when().delete("/waitlists/wl-1/entries/missing")
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"))
        .body("message", equalTo("Entry does not exist"));

    verify(storage).deleteEntry("wl-1", "missing");
  }

  @Test
  void secondDeleteReturns404() {
    existingWaitlist();
    when(storage.deleteEntry("wl-1", "e-1")).thenReturn(true, false);

    given().port(port).when().delete("/waitlists/wl-1/entries/e-1")
        .then().statusCode(200);
    given().port(port).when().delete("/waitlists/wl-1/entries/e-1")
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"))
        .body("message", equalTo("Entry does not exist"));

    verify(storage, times(2)).deleteEntry("wl-1", "e-1");
  }

  private void existingWaitlist() {
    when(storage.findWaitlistById("wl-1"))
        .thenReturn(Optional.of(new Waitlist("wl-1", "Support", 10, 5)));
  }
}
