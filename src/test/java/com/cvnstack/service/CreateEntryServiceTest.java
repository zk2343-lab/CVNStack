package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.request.CreateEntryRequest;
import com.cvnstack.model.response.EntryResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

/**
 * Checks entry creation, validation, atomic insertion, and shared waiting rules.
 *
 * <p>AI-assisted.
 */
class CreateEntryServiceTest {

  private final WaitlistStorage storage = mock(WaitlistStorage.class);
  private final CacheStorage cache = mock(CacheStorage.class);
  private final CreateEntryService service = new CreateEntryService(
      storage, new CacheConsistencyService(storage, cache, new EntryRulesService()),
      new EntryRulesService());
  private final List<Entry> entries = new ArrayList<>();

  @BeforeEach
  void prepareWaitlist() {
    when(storage.findWaitlistById("wl-1"))
        .thenReturn(Optional.of(new Waitlist("wl-1", "Support", 10, 5)));
  }

  private void allowInsert() {
    when(storage.insertEntryIfCapacityAvailable(any())).thenAnswer(call -> {
      Entry entry = call.getArgument(0);
      assertNull(entry.calledAt());
      assertNull(entry.updatedAt());
      assertNotNull(entry.insertedAt());
      Entry saved = new Entry(entry.id(), entry.waitlistId(), entry.name(), entry.priority(),
          entry.status(), entry.insertedAt(), null, Instant.now());
      entries.add(saved);
      return saved;
    });
    when(storage.findEntriesByWaitlistId("wl-1")).thenAnswer(call -> List.copyOf(entries));
  }

  @Test
  void validCreateUsesPersistedEntryAndUpdatesBothCacheRepresentations() {
    allowInsert();
    EntryResponse response = service.createEntry("wl-1", new CreateEntryRequest("Alice", 3));
    assertEquals(response.entryId(), UUID.fromString(response.entryId()).toString());
    assertEquals("wl-1", response.waitlistId());
    assertEquals(EntryStatus.WAITING, response.status());
    assertEquals(3, response.priority());
    assertEquals(1, response.position());
    assertEquals(0, response.estimatedWaitTimeMinutes());
    assertEquals(1, entries.size());
    assertEquals("Alice", entries.getFirst().name());
    assertNotNull(entries.getFirst().updatedAt());
    verify(storage).insertEntryIfCapacityAvailable(any(Entry.class));
    verify(storage).findWaitlistById("wl-1");
    verify(storage).findEntriesByWaitlistId("wl-1");
    verifyNoMoreInteractions(storage);
    verify(cache).putEntry(entries.getFirst());
    verify(cache).replaceWaitingOrderIfNewer(
        eq("wl-1"), eq(List.of(response.entryId())), any());
    verifyNoMoreInteractions(cache);
  }

  @Test
  void omittedPriorityDefaultsToOne() {
    allowInsert();
    assertEquals(1, service.createEntry("wl-1", new CreateEntryRequest("Alice", null)).priority());
    assertEquals(1, entries.getFirst().priority());
  }

  static List<CreateEntryRequest> invalidRequests() {
    return java.util.Arrays.asList(null, new CreateEntryRequest(null, 1),
        new CreateEntryRequest("", 1), new CreateEntryRequest(" \t\n", 1),
        new CreateEntryRequest("Alice", 0), new CreateEntryRequest("Alice", 6),
        new CreateEntryRequest("Alice", Integer.MIN_VALUE),
        new CreateEntryRequest("Alice", Integer.MAX_VALUE));
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  void invalidRequestsNeverAccessStorage(CreateEntryRequest request) {
    ApiException exception = assertThrows(ApiException.class,
        () -> service.createEntry("wl-1", request));
    assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    assertEquals("INVALID_REQUEST", exception.errorCode());
    verifyNoInteractions(storage, cache);
  }

  @Test
  void missingWaitlistDoesNotInsert() {
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.empty());
    ApiException exception = assertThrows(ApiException.class,
        () -> service.createEntry("wl-1", new CreateEntryRequest("Alice", 1)));
    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals("Waitlist does not exist", exception.getMessage());
    verify(storage).findWaitlistById("wl-1");
    verifyNoMoreInteractions(storage);
    verifyNoInteractions(cache);
  }

  @Test
  void capacityFailurePropagatesWithoutFurtherWrites() {
    ApiException full = ApiException.capacityExceeded("Waitlist is full");
    when(storage.insertEntryIfCapacityAvailable(any())).thenThrow(full);
    assertSame(full, assertThrows(ApiException.class,
        () -> service.createEntry("wl-1", new CreateEntryRequest("Alice", 1))));
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, full.status());
    verify(storage).findWaitlistById("wl-1");
    verify(storage).insertEntryIfCapacityAvailable(any());
    verifyNoMoreInteractions(storage);
    verifyNoInteractions(cache);
  }

  @Test
  void duplicateNamesCreateDistinctEntries() {
    allowInsert();
    EntryResponse first = service.createEntry("wl-1", new CreateEntryRequest("Alice", 1));
    EntryResponse second = service.createEntry("wl-1", new CreateEntryRequest("Alice", 1));
    assertNotEquals(first.entryId(), second.entryId());
    assertEquals(2, entries.size());
    verify(storage, never()).saveEntry(any());
  }

  @Test
  void redisFailureAfterInsertInvalidatesAndStillReturnsTheCommittedEntry() {
    allowInsert();
    doThrow(new IllegalStateException("Redis unavailable")).when(cache).putEntry(any());
    EntryResponse response = service.createEntry("wl-1", new CreateEntryRequest("Alice", 1));
    assertEquals(1, response.position());
    verify(cache).invalidateWaitlist("wl-1");
  }

  @Test
  void higherPriorityJumpsAheadOfOlderEntry() {
    allowInsert();
    entries.add(new Entry("older", "wl-1", "Bob", 1, EntryStatus.WAITING, Instant.EPOCH));
    EntryResponse response = service.createEntry("wl-1", new CreateEntryRequest("Alice", 5));
    assertEquals(1, response.position());
    assertEquals(0, response.estimatedWaitTimeMinutes());
    verify(cache).replaceWaitingOrderIfNewer(
        eq("wl-1"), eq(List.of(response.entryId(), "older")), any());
  }

  @Test
  void samePriorityPreservesInsertionOrderAndEstimateCountsOnlyWaitingEntries() {
    allowInsert();
    entries.add(new Entry("older", "wl-1", "Bob", 3, EntryStatus.WAITING, Instant.EPOCH));
    entries.add(new Entry("urgent", "wl-1", "Carol", 5, EntryStatus.WAITING, Instant.EPOCH));
    entries.add(new Entry("called", "wl-1", "Dan", 5, EntryStatus.CALLED, Instant.EPOCH));
    EntryResponse response = service.createEntry("wl-1", new CreateEntryRequest("Alice", 3));
    assertEquals(3, response.position());
    assertEquals(10, response.estimatedWaitTimeMinutes());
    verify(cache).replaceWaitingOrderIfNewer(
        eq("wl-1"), eq(List.of("urgent", "older", response.entryId())), any());
  }
}
