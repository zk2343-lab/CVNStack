package com.cvnstack.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
 * Checks serve-next HTTP responses with mock repository and cache ports.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServeNextControllerTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
  private static final Waitlist WAITLIST = new Waitlist("wl-1", "Support", 10, 5);

  @LocalServerPort
  private int port;

  @MockitoBean
  private WaitlistStorage storage;

  @MockitoBean
  private CacheStorage cache;

  @AfterEach
  void doesNotUseAnUnconditionalSave() {
    verify(storage, never()).saveEntry(any(Entry.class));
    verify(storage, never()).saveWaitlist(any(Waitlist.class));
    verify(storage, never()).deleteEntry(anyString(), anyString());
  }

  @Test
  void selectedEntryReturns200() {
    Entry first = new Entry("e1", "wl-1", "Desktop", 5, EntryStatus.WAITING, T0);
    givenEntries(List.of(first));
    givenClaimSucceeds(first);

    given().port(port)
        .when().post("/waitlists/wl-1/serve-next")
        .then().statusCode(200)
        .body("waitlistId", equalTo("wl-1"))
        .body("entryId", equalTo("e1"))
        .body("status", equalTo("CALLED"))
        .body("priority", equalTo(5));

    verify(cache).invalidateWaitlist("wl-1");
  }

  @Test
  void emptyWaitlistReturns204WithoutBody() {
    givenEntries(List.of());

    given().port(port)
        .when().post("/waitlists/wl-1/serve-next")
        .then().statusCode(204)
        .body(equalTo(""));

    verify(storage, never()).callEntryIfWaiting(anyString(), anyString(), any());
    verifyNoInteractions(cache);
  }

  @Test
  void missingWaitlistReturns404() {
    when(storage.findWaitlistById("missing")).thenReturn(Optional.empty());

    given().port(port)
        .when().post("/waitlists/missing/serve-next")
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"))
        .body("message", equalTo("Waitlist does not exist"));

    verify(storage, never()).callEntryIfWaiting(anyString(), anyString(), any());
    verifyNoInteractions(cache);
  }

  @Test
  void repeatedPostsSelectDifferentEntriesThenReturn204() {
    Entry first = new Entry("e1", "wl-1", "Desktop", 5, EntryStatus.WAITING, T0);
    Entry second = new Entry("e2", "wl-1", "ROBOT", 1, EntryStatus.WAITING, T0);
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntriesByWaitlistId("wl-1"))
        .thenReturn(List.of(second, first))
        .thenReturn(List.of(second))
        .thenReturn(List.of());
    givenClaimSucceeds(first);
    givenClaimSucceeds(second);

    given().port(port).when().post("/waitlists/wl-1/serve-next")
        .then().statusCode(200).body("entryId", equalTo("e1"));
    given().port(port).when().post("/waitlists/wl-1/serve-next")
        .then().statusCode(200).body("entryId", equalTo("e2"));
    given().port(port).when().post("/waitlists/wl-1/serve-next")
        .then().statusCode(204).body(equalTo(""));

    verify(cache, times(2)).invalidateWaitlist("wl-1");
  }

  private void givenEntries(List<Entry> entries) {
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntriesByWaitlistId("wl-1")).thenReturn(entries);
  }

  private void givenClaimSucceeds(Entry entry) {
    when(storage.callEntryIfWaiting(eq("wl-1"), eq(entry.id()), any()))
        .thenAnswer(invocation -> Optional.of(new Entry(
            entry.id(), entry.waitlistId(), entry.name(), entry.priority(),
            EntryStatus.CALLED, entry.insertedAt(), invocation.getArgument(2))));
  }
}
