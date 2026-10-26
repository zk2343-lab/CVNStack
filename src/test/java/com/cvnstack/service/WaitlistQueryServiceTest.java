package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.error.ErrorCode;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.response.EntryResponse;
import com.cvnstack.model.response.WaitlistDetailResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Checks waitlist lookups return entries in waiting order.
 *
 * <p>AI-assisted.
 */
@ExtendWith(MockitoExtension.class)
class WaitlistQueryServiceTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
  private static final Waitlist WAITLIST = new Waitlist("wl-1", "Support", 10, 5);

  @Mock
  private WaitlistStorage storage;

  private WaitlistQueryService service;

  @BeforeEach
  void setUp() {
    service = new WaitlistQueryService(storage, new EntryRulesService());
  }

  @AfterEach
  void doesNotChangeStoredData() {
    verify(storage, never()).saveEntry(any(Entry.class));
    verify(storage, never()).saveWaitlist(any(Waitlist.class));
    verify(storage, never()).deleteEntry(anyString(), anyString());
  }

  @Test
  void returnsWaitingEntriesFirstThenTheRest() {
    Entry alice = entry("alice", 1, EntryStatus.WAITING, T0);
    Entry bob = entry("bob", 5, EntryStatus.WAITING, T0.plusSeconds(1));
    Entry carol = entry("carol", 3, EntryStatus.CALLED, T0.plusSeconds(2));
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntriesByWaitlistId("wl-1")).thenReturn(List.of(alice, bob, carol));

    WaitlistDetailResponse response = service.getWaitlist("wl-1");

    assertEquals("wl-1", response.id());
    assertEquals("Support", response.name());
    assertEquals(10, response.maxLength());
    assertEquals(5, response.processingTimePerEntryMinutes());
    List<EntryResponse> entries = response.entries();
    assertEquals(3, entries.size());
    assertEquals("bob", entries.get(0).entryId());
    assertEquals(1, entries.get(0).position());
    assertEquals(0, entries.get(0).estimatedWaitTimeMinutes());
    assertEquals("alice", entries.get(1).entryId());
    assertEquals(2, entries.get(1).position());
    assertEquals(5, entries.get(1).estimatedWaitTimeMinutes());
    assertEquals("carol", entries.get(2).entryId());
    assertEquals(EntryStatus.CALLED, entries.get(2).status());
    assertNull(entries.get(2).position());
    assertNull(entries.get(2).estimatedWaitTimeMinutes());
  }

  @Test
  void emptyWaitlistReturnsNoEntries() {
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntriesByWaitlistId("wl-1")).thenReturn(List.of());

    WaitlistDetailResponse response = service.getWaitlist("wl-1");

    assertEquals(List.of(), response.entries());
  }

  @Test
  void missingWaitlistReturnsNotFound() {
    when(storage.findWaitlistById("missing")).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class,
        () -> service.getWaitlist("missing"));

    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals(ErrorCode.NOT_FOUND, exception.errorCode());
    verify(storage, never()).findEntriesByWaitlistId(anyString());
  }

  private static Entry entry(
      String id, int priority, EntryStatus status, Instant insertedAt) {
    return new Entry(id, "wl-1", id, priority, status, insertedAt);
  }
}
