package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.request.PatchEntryRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/**
 * Checks PATCH validation, ranking, timestamps, and concurrent changes with mocked storage.
 *
 * <p>AI-assisted.
 */
class PatchEntryServiceTest {

  private final WaitlistStorage storage = mock(WaitlistStorage.class);
  private final CacheStorage cache = mock(CacheStorage.class);
  private final PatchEntryService service = new PatchEntryService(
      storage, new CacheConsistencyService(storage, cache, new EntryRulesService()),
      new EntryRulesService());
  private final Instant inserted = Instant.parse("2020-01-01T00:00:00Z");
  private final Instant updated = inserted.plusSeconds(10);
  private Entry current;

  @BeforeEach
  void setup() {
    current = entry(EntryStatus.WAITING, 1, null);
    when(storage.findWaitlistById("wl")).thenReturn(
        Optional.of(new Waitlist("wl", "Support", 10, 5)));
    when(storage.findEntry("wl", "e")).thenReturn(Optional.of(current));
  }

  private Entry entry(EntryStatus status, int priority, Instant calledAt) {
    return new Entry("e", "wl", "Alice", priority, status, inserted, calledAt, updated);
  }

  private void persist() {
    when(storage.updateEntryIfUnchanged(any(), any())).thenAnswer(call -> {
      Entry desired = call.getArgument(0);
      return Optional.of(new Entry(desired.id(), desired.waitlistId(), desired.name(),
          desired.priority(), desired.status(), desired.insertedAt(), desired.calledAt(),
          updated.plusSeconds(20)));
    });
  }

  @Test
  void allowedTransitionSetsCalledAtAndUsesDatabaseTimestamp() {
    persist();
    Instant before = Instant.now();
    var response = service.patchEntry("wl", "e", new PatchEntryRequest(EntryStatus.CALLED, null));
    assertEquals(EntryStatus.CALLED, response.status());
    assertEquals(updated.plusSeconds(20), response.updatedAt());
    assertNull(response.position());
    assertNull(response.estimatedWaitTimeMinutes());
    ArgumentCaptor<Entry> desired = ArgumentCaptor.forClass(Entry.class);
    verify(storage).updateEntryIfUnchanged(desired.capture(), eq(current));
    assertFalse(desired.getValue().calledAt().isBefore(before));
    assertFalse(desired.getValue().calledAt().isAfter(Instant.now()));
    assertEquals(updated, desired.getValue().updatedAt());
    verify(storage, never()).saveEntry(any());
    verify(cache).putEntry(any(Entry.class));
    verify(cache).replaceWaitingOrder(eq("wl"), any());
  }

  @Test
  void rejectsTransitionAndTerminalSecondPatch() {
    when(storage.findEntry("wl", "e"))
        .thenReturn(Optional.of(entry(EntryStatus.SERVED, 1, inserted)));
    assertBadRequest(new PatchEntryRequest(EntryStatus.WAITING, null));
    verify(storage, never()).updateEntryIfUnchanged(any(), any());
  }

  @Test
  void priorityReorderWithSameStatusRecalculatesWaitingInformation() {
    persist();
    Entry high = new Entry("high", "wl", "High", 5, EntryStatus.WAITING, inserted);
    Entry middle = new Entry("middle", "wl", "Middle", 3, EntryStatus.WAITING, inserted);
    when(storage.findEntriesByWaitlistId("wl"))
        .thenReturn(List.of(high, middle, current.withPriority(4)));
    var response = service.patchEntry("wl", "e", new PatchEntryRequest(EntryStatus.WAITING, 4));
    assertEquals(4, response.priority());
    assertEquals(2, response.position());
    assertEquals(5, response.estimatedWaitTimeMinutes());
  }

  @Test
  void priorityOnlyRecalculatesAndPreservesCalledAt() {
    persist();
    when(storage.findEntriesByWaitlistId("wl")).thenReturn(List.of(current.withPriority(5)));
    var response = service.patchEntry("wl", "e", new PatchEntryRequest(null, 5));
    assertEquals(1, response.position());
    assertEquals(0, response.estimatedWaitTimeMinutes());
    ArgumentCaptor<Entry> desired = ArgumentCaptor.forClass(Entry.class);
    verify(storage).updateEntryIfUnchanged(desired.capture(), eq(current));
    assertNull(desired.getValue().calledAt());
  }

  @Test
  void rejectsEmptyAndNullRequestsBeforeReading() {
    assertBadRequest(null);
    assertBadRequest(new PatchEntryRequest(null, null));
    verify(storage, never()).findWaitlistById(any());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 6, -1})
  void rejectsInvalidPriority(int priority) {
    assertBadRequest(new PatchEntryRequest(null, priority));
    verify(storage, never()).findWaitlistById(any());
  }

  @Test
  void missingWaitlist() {
    when(storage.findWaitlistById("wl")).thenReturn(Optional.empty());
    assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class,
        () -> service.patchEntry("wl", "e", new PatchEntryRequest(null, 5))).status());
    verify(storage, never()).findEntry(any(), any());
  }

  @Test
  void missingEntry() {
    when(storage.findEntry("wl", "e")).thenReturn(Optional.empty());
    assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class,
        () -> service.patchEntry("wl", "e", new PatchEntryRequest(null, 5))).status());
  }

  @ParameterizedTest
  @ValueSource(strings = {"status", "priority", "both"})
  void repeatedValuesNeverWrite(String field) {
    when(storage.findEntriesByWaitlistId("wl")).thenReturn(List.of(current));
    var response = service.patchEntry("wl", "e", new PatchEntryRequest(
        field.equals("priority") ? null : EntryStatus.WAITING,
        field.equals("status") ? null : 1));
    assertEquals(updated, response.updatedAt());
    assertEquals(1, response.position());
    verify(storage, never()).updateEntryIfUnchanged(any(), any());
  }

  @ParameterizedTest
  @EnumSource(value = EntryStatus.class, names = {"SERVED", "CANCELLED"})
  void terminalPriorityChangesRejected(EntryStatus status) {
    when(storage.findEntry("wl", "e")).thenReturn(Optional.of(entry(status, 1, inserted)));
    assertBadRequest(new PatchEntryRequest(null, 5));
    verify(storage, never()).updateEntryIfUnchanged(any(), any());
  }

  @ParameterizedTest
  @EnumSource(value = EntryStatus.class, names = {"SERVED", "CANCELLED"})
  void calledTransitionsPreserveCalledAtAndValidatePriorityAgainstCurrentState(EntryStatus status) {
    current = entry(EntryStatus.CALLED, 1, inserted);
    when(storage.findEntry("wl", "e")).thenReturn(Optional.of(current));
    persist();
    service.patchEntry("wl", "e", new PatchEntryRequest(status, 5));
    ArgumentCaptor<Entry> desired = ArgumentCaptor.forClass(Entry.class);
    verify(storage).updateEntryIfUnchanged(desired.capture(), eq(current));
    assertEquals(inserted, desired.getValue().calledAt());
    assertEquals(5, desired.getValue().priority());
  }

  @Test
  void waitingCancellationDoesNotInventCalledAt() {
    persist();
    service.patchEntry("wl", "e", new PatchEntryRequest(EntryStatus.CANCELLED, null));
    ArgumentCaptor<Entry> desired = ArgumentCaptor.forClass(Entry.class);
    verify(storage).updateEntryIfUnchanged(desired.capture(), eq(current));
    assertNull(desired.getValue().calledAt());
  }

  @Test
  void calledPriorityOnlyPreservesCalledAt() {
    current = entry(EntryStatus.CALLED, 1, inserted);
    when(storage.findEntry("wl", "e")).thenReturn(Optional.of(current));
    persist();
    service.patchEntry("wl", "e", new PatchEntryRequest(null, 5));
    ArgumentCaptor<Entry> desired = ArgumentCaptor.forClass(Entry.class);
    verify(storage).updateEntryIfUnchanged(desired.capture(), eq(current));
    assertEquals(inserted, desired.getValue().calledAt());
  }

  @Test
  void concurrentTerminalChangeRejectsStaleTransitionWithoutOverwriting() {
    when(storage.findEntry("wl", "e")).thenReturn(Optional.of(current),
        Optional.of(entry(EntryStatus.CANCELLED, 1, null)));
    when(storage.updateEntryIfUnchanged(any(), any())).thenReturn(Optional.empty());
    assertBadRequest(new PatchEntryRequest(EntryStatus.CALLED, null));
    verify(storage, times(1)).updateEntryIfUnchanged(any(), eq(current));
  }

  @Test
  void concurrentDeletionReturns404() {
    when(storage.findEntry("wl", "e")).thenReturn(Optional.of(current), Optional.empty());
    when(storage.updateEntryIfUnchanged(any(), any())).thenReturn(Optional.empty());
    assertEquals(HttpStatus.NOT_FOUND, assertThrows(ApiException.class,
        () -> service.patchEntry("wl", "e", new PatchEntryRequest(null, 5))).status());
  }

  @Test
  void retryUsesLatestPriorityAndCalledAt() {
    Entry latest = entry(EntryStatus.CALLED, 4, inserted);
    when(storage.findEntry("wl", "e")).thenReturn(Optional.of(current), Optional.of(latest));
    when(storage.updateEntryIfUnchanged(any(), any())).thenReturn(Optional.empty(),
        Optional.of(latest.withPriority(5)));
    service.patchEntry("wl", "e", new PatchEntryRequest(null, 5));
    verify(storage).updateEntryIfUnchanged(latest.withPriority(5), latest);
  }

  private void assertBadRequest(PatchEntryRequest request) {
    assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ApiException.class,
        () -> service.patchEntry("wl", "e", request)).status());
  }
}

