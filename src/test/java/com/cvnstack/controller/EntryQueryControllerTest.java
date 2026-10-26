package com.cvnstack.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.service.CacheStorage;
import com.cvnstack.service.WaitlistStorage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Checks the entry lookup HTTP route with a mock repository.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EntryQueryControllerTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
  private static final Waitlist WAITLIST = new Waitlist("wl-1", "Support", 10, 5);

  @LocalServerPort
  private int port;

  @MockitoBean
  private WaitlistStorage storage;

  @MockitoBean
  private CacheStorage cache;

  @AfterEach
  void doesNotChangeStoredData() {
    verify(storage, never()).saveEntry(any(Entry.class));
    verify(storage, never()).saveWaitlist(any(Waitlist.class));
    verify(storage, never()).deleteEntry(anyString(), anyString());
  }

  @Test
  void waitingEntryReturns200AndRepeatedGetDoesNotChangeIt() {
    Entry target = new Entry("e1", "wl-1", "Desktop", 1, EntryStatus.WAITING, T0);
    Entry urgent = new Entry("e2", "wl-1", "ROBOT", 5, EntryStatus.WAITING, T0);
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntry("wl-1", "e1")).thenReturn(Optional.of(target));
    when(storage.findEntriesByWaitlistId("wl-1")).thenReturn(List.of(target, urgent));

    String firstResponse = given().port(port)
        .when().get("/waitlists/wl-1/entries/e1")
        .then().statusCode(200)
        .body("waitlistId", equalTo("wl-1"))
        .body("entryId", equalTo("e1"))
        .body("status", equalTo("WAITING"))
        .body("priority", equalTo(1))
        .body("position", equalTo(2))
        .body("estimatedWaitTimeMinutes", equalTo(5))
        .extract().asString();

    given().port(port)
        .when().get("/waitlists/wl-1/entries/e1")
        .then().statusCode(200)
        .body(equalTo(firstResponse));
  }

  @Test
  void calledEntryReturns200WithNullWaitingInformation() {
    Entry target = new Entry("e1", "wl-1", "Desktop", 1, EntryStatus.CALLED, T0);
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntry("wl-1", "e1")).thenReturn(Optional.of(target));

    given().port(port)
        .when().get("/waitlists/wl-1/entries/e1")
        .then().statusCode(200)
        .body("status", equalTo("CALLED"))
        .body("position", nullValue())
        .body("estimatedWaitTimeMinutes", nullValue());
  }

  @Test
  void missingWaitlistReturns404() {
    when(storage.findWaitlistById("missing")).thenReturn(Optional.empty());

    given().port(port)
        .when().get("/waitlists/missing/entries/e1")
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"))
        .body("message", equalTo("Waitlist does not exist"));
  }
}
