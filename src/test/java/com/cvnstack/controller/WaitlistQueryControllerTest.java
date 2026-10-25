package com.cvnstack.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
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
 * Checks the waitlist lookup HTTP route with a mock repository.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WaitlistQueryControllerTest {

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
  void existingWaitlistReturns200WithEntriesInOrder() {
    Entry alice = new Entry("alice", "wl-1", "Alice", 1, EntryStatus.WAITING, T0);
    Entry bob = new Entry("bob", "wl-1", "Bob", 5, EntryStatus.WAITING, T0.plusSeconds(1));
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntriesByWaitlistId("wl-1")).thenReturn(List.of(alice, bob));

    given().port(port)
        .when().get("/waitlists/wl-1")
        .then().statusCode(200)
        .body("id", equalTo("wl-1"))
        .body("name", equalTo("Support"))
        .body("entries.size()", equalTo(2))
        .body("entries[0].entryId", equalTo("bob"))
        .body("entries[0].position", equalTo(1))
        .body("entries[1].entryId", equalTo("alice"))
        .body("entries[1].position", equalTo(2))
        .body("entries[1].estimatedWaitTimeMinutes", equalTo(5));
  }

  @Test
  void missingWaitlistReturns404() {
    when(storage.findWaitlistById("missing")).thenReturn(Optional.empty());

    given().port(port)
        .when().get("/waitlists/missing")
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"))
        .body("message", equalTo("Waitlist does not exist"));
  }

  @Test
  void emptyWaitlistReturns200WithNoEntries() {
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntriesByWaitlistId("wl-1")).thenReturn(List.of());

    given().port(port)
        .when().get("/waitlists/wl-1")
        .then().statusCode(200)
        .body("entries.size()", equalTo(0));
  }
}
