package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.error.ErrorCode;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.response.ServeNextResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Checks serve order and conditional claims with mock repository and cache ports.
 *
 * <p>AI-assisted.
 */
@ExtendWith(MockitoExtension.class)
class ServeNextServiceTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
  private static final Waitlist WAITLIST = new Waitlist("wl-1", "Support", 10, 5);

  @Mock
  private WaitlistStorage storage;

  @Mock
  private CacheStorage cache;

  private ServeNextService service;

  @BeforeEach
  void setUp() {
    service = new ServeNextService(storage, cache, new EntryRulesService());
  }

  @AfterEach
  void doesNotUseAnUnconditionalSave() {
    verify(storage, never()).saveEntry(any(Entry.class));
    verify(storage, never()).saveWaitlist(any(Waitlist.class));
    verify(storage, never()).deleteEntry(anyString(), anyString());
  }

  @Test
  void higherPriorityIsCalledFirstAndRecordsCallTime() {
    Entry normal = entry("normal", 1, EntryStatus.WAITING, T0);
    Entry urgent = entry("urgent", 5, EntryStatus.WAITING, T0.plusSeconds(1));
    Entry alreadyCalled = entry("called", 5, EntryStatus.CALLED, T0);
    givenEntries(List.of(normal, alreadyCalled, urgent));
    givenClaimSucceeds(urgent);
    Instant before = Instant.now();

    ServeNextResponse response = service.serveNext("wl-1").orElseThrow();

    Instant after = Instant.now();
    assertEquals("wl-1", response.waitlistId());
    assertEquals("urgent", response.entryId());
    assertEquals(EntryStatus.CALLED, response.status());
    assertEquals(5, response.priority());
    ArgumentCaptor<Instant> calledAt = ArgumentCaptor.forClass(Instant.class);
    InOrder order = inOrder(storage, cache);
    order.verify(storage).callEntryIfWaiting(eq("wl-1"), eq("urgent"), calledAt.capture());
    order.verify(cache).invalidateWaitlist("wl-1");
    assertTrue(!calledAt.getValue().isBefore(before) && !calledAt.getValue().isAfter(after));
    verify(storage, never()).callEntryIfWaiting(eq("wl-1"), eq("normal"), any());
    verify(storage, never()).callEntryIfWaiting(eq("wl-1"), eq("called"), any());
  }

  @Test
  void equalPriorityUsesInsertionOrder() {
    Entry early = entry("early", 3, EntryStatus.WAITING, T0);
    Entry late = entry("late", 3, EntryStatus.WAITING, T0.plusSeconds(1));
    givenEntries(List.of(late, early));
    givenClaimSucceeds(early);

    ServeNextResponse response = service.serveNext("wl-1").orElseThrow();

    assertEquals("early", response.entryId());
    verify(storage, never()).callEntryIfWaiting(eq("wl-1"), eq("late"), any());
  }

  @Test
  void emptyWaitlistReturnsEmptyWithoutWrites() {
    givenEntries(List.of());

    assertTrue(service.serveNext("wl-1").isEmpty());

    verify(storage, never()).callEntryIfWaiting(anyString(), anyString(), any());
    verifyNoInteractions(cache);
  }

  @Test
  void nonWaitingEntriesAreNotSelected() {
    givenEntries(List.of(
        entry("called", 5, EntryStatus.CALLED, T0),
        entry("served", 5, EntryStatus.SERVED, T0),
        entry("cancelled", 5, EntryStatus.CANCELLED, T0)));

    assertTrue(service.serveNext("wl-1").isEmpty());

    verify(storage, never()).callEntryIfWaiting(anyString(), anyString(), any());
    verifyNoInteractions(cache);
  }

  @Test
  void missingWaitlistReturnsNotFound() {
    when(storage.findWaitlistById("missing")).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class,
        () -> service.serveNext("missing"));

    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals(ErrorCode.NOT_FOUND, exception.errorCode());
    verify(storage, never()).findEntriesByWaitlistId(anyString());
    verify(storage, never()).callEntryIfWaiting(anyString(), anyString(), any());
    verifyNoInteractions(cache);
  }

  @Test
  void skipsAnEntryClaimedByAnotherRequest() {
    Entry first = entry("first", 5, EntryStatus.WAITING, T0);
    Entry second = entry("second", 1, EntryStatus.WAITING, T0);
    givenEntries(List.of(second, first));
    when(storage.callEntryIfWaiting(eq("wl-1"), eq("first"), any()))
        .thenReturn(Optional.empty());
    givenClaimSucceeds(second);

    ServeNextResponse response = service.serveNext("wl-1").orElseThrow();

    assertEquals("second", response.entryId());
    InOrder order = inOrder(storage, cache);
    order.verify(storage).callEntryIfWaiting(eq("wl-1"), eq("first"), any());
    order.verify(storage).callEntryIfWaiting(eq("wl-1"), eq("second"), any());
    order.verify(cache).invalidateWaitlist("wl-1");
  }

  @Test
  void allCandidatesAlreadyClaimedReturnsEmpty() {
    Entry first = entry("first", 5, EntryStatus.WAITING, T0);
    givenEntries(List.of(first));
    when(storage.callEntryIfWaiting(eq("wl-1"), eq("first"), any()))
        .thenReturn(Optional.empty());

    assertTrue(service.serveNext("wl-1").isEmpty());

    verifyNoInteractions(cache);
  }

  @Test
  void databaseFailureDoesNotChangeCache() {
    Entry first = entry("first", 5, EntryStatus.WAITING, T0);
    givenEntries(List.of(first));
    when(storage.callEntryIfWaiting(eq("wl-1"), eq("first"), any()))
        .thenThrow(new IllegalStateException("Database unavailable"));

    assertThrows(IllegalStateException.class, () -> service.serveNext("wl-1"));

    verifyNoInteractions(cache);
  }

  @Test
  void cacheFailureIsNotReportedAsSuccessful() {
    Entry first = entry("first", 5, EntryStatus.WAITING, T0);
    givenEntries(List.of(first));
    givenClaimSucceeds(first);
    doThrow(new IllegalStateException("Redis unavailable"))
        .when(cache).invalidateWaitlist("wl-1");

    assertThrows(IllegalStateException.class, () -> service.serveNext("wl-1"));

    InOrder order = inOrder(storage, cache);
    order.verify(storage).callEntryIfWaiting(eq("wl-1"), eq("first"), any());
    order.verify(cache).invalidateWaitlist("wl-1");
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

  private static Entry entry(String id, int priority, EntryStatus status, Instant insertedAt) {
    return new Entry(id, "wl-1", id, priority, status, insertedAt);
  }
}
