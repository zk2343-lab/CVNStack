package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Checks Redis write-through, invalidation on failure, and PostgreSQL rebuilds.
 *
 * <p>AI-assisted.
 */
@ExtendWith(MockitoExtension.class)
class CacheConsistencyServiceTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

  @Mock
  private WaitlistStorage storage;

  @Mock
  private CacheStorage cache;

  private CacheConsistencyService consistency;

  @BeforeEach
  void setUp() {
    consistency = new CacheConsistencyService(storage, cache, new EntryRulesService());
  }

  @Test
  void replaceCacheAfterCommitWritesEntryThenWaitingOrder() {
    Entry entry = entry("e1", 1, T0.plusSeconds(1));
    consistency.replaceCacheAfterCommit("wl-1", entry, List.of("e1"));

    verify(cache).putEntry(entry);
    verify(cache).replaceWaitingOrderIfNewer("wl-1", List.of("e1"), T0.plusSeconds(1));
    verify(cache, never()).replaceWaitingOrder(anyString(), any());
    verify(cache, never()).invalidateWaitlist(anyString());
  }

  @Test
  void replaceCacheAfterCommitKeepsTheNewerCachedEntryAndRank() {
    Entry older = entry("e1", 2, T0.plusSeconds(1));
    Entry newer = entry("e1", 4, T0.plusSeconds(2));
    consistency.replaceCacheAfterCommit("wl-1", newer, List.of("e1"));
    consistency.replaceCacheAfterCommit("wl-1", older, List.of("e2", "e1"));

    verify(cache).putEntry(newer);
    verify(cache).putEntry(older);
    verify(cache).replaceWaitingOrderIfNewer("wl-1", List.of("e1"), T0.plusSeconds(2));
    verify(cache).replaceWaitingOrderIfNewer("wl-1", List.of("e2", "e1"), T0.plusSeconds(1));
    verify(cache, never()).invalidateWaitlist(anyString());
  }

  @Test
  void replaceCacheAfterCommitInvalidatesWhenRedisWriteFails() {
    doThrow(new IllegalStateException("Redis unavailable")).when(cache).putEntry(any());

    consistency.replaceCacheAfterCommit("wl-1", entry("e1", 1), List.of("e1"));

    verify(cache).invalidateWaitlist("wl-1");
  }

  @Test
  void dropEntryAfterCommitRemovesPayloadAndRankMember() {
    consistency.dropEntryAfterCommit("wl-1", "e1");

    verify(cache).deleteEntry("wl-1", "e1");
    verify(cache).removeWaitingMember("wl-1", "e1");
  }

  @Test
  void dropEntryAfterCommitInvalidatesWhenMemberRemovalFails() {
    doThrow(new IllegalStateException("Redis unavailable"))
        .when(cache).removeWaitingMember("wl-1", "e1");

    consistency.dropEntryAfterCommit("wl-1", "e1");

    verify(cache).invalidateWaitlist("wl-1");
  }

  @Test
  void writeThroughCalledEntryPutsPayloadThenRemovesWaitingMember() {
    Entry called = new Entry("e1", "wl-1", "e1", 5, EntryStatus.CALLED, T0, T0, T0);

    consistency.writeThroughCalledEntry("wl-1", called);

    verify(cache).putEntry(called);
    verify(cache).removeWaitingMember("wl-1", "e1");
    verify(cache, never()).deleteEntry(anyString(), anyString());
    verify(cache, never()).invalidateWaitlist(anyString());
  }

  @Test
  void writeThroughCalledEntryInvalidatesWhenRedisWriteFails() {
    Entry called = new Entry("e1", "wl-1", "e1", 5, EntryStatus.CALLED, T0, T0, T0);
    doThrow(new IllegalStateException("Redis unavailable")).when(cache).putEntry(called);

    consistency.writeThroughCalledEntry("wl-1", called);

    verify(cache).invalidateWaitlist("wl-1");
    verify(cache, never()).deleteEntry(anyString(), anyString());
  }

  @Test
  void readEntryRebuildsAMissingPayloadFromPostgres() {
    Entry stored = entry("e1", 3);
    when(cache.findEntry("wl-1", "e1")).thenReturn(Optional.empty());
    when(storage.findEntry("wl-1", "e1")).thenReturn(Optional.of(stored));

    assertEquals(stored, consistency.readEntry("wl-1", "e1"));
    verify(cache).putEntry(stored);
  }

  @Test
  void readEntryUsesAWarmCacheWithoutReadingPostgres() {
    Entry cached = entry("e1", 2);
    when(cache.findEntry("wl-1", "e1")).thenReturn(Optional.of(cached));

    assertEquals(cached, consistency.readEntry("wl-1", "e1"));
    verify(storage, never()).findEntry(anyString(), anyString());
  }

  @Test
  void readEntryRejectsAMissingPostgresRow() {
    when(cache.findEntry("wl-1", "missing")).thenReturn(Optional.empty());
    when(storage.findEntry("wl-1", "missing")).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class,
        () -> consistency.readEntry("wl-1", "missing"));
    assertEquals(HttpStatus.NOT_FOUND, exception.status());
  }

  @Test
  void readWaitingOrderRebuildsAMissingRankKey() {
    Entry urgent = entry("urgent", 5);
    Entry normal = entry("normal", 1);
    when(cache.findWaitingOrder("wl-1")).thenReturn(Optional.empty());
    when(storage.findEntriesByWaitlistId("wl-1")).thenReturn(List.of(normal, urgent));

    assertEquals(List.of("urgent", "normal"), consistency.readWaitingOrder("wl-1"));
    verify(cache).replaceWaitingOrderIfNewer("wl-1", List.of("urgent", "normal"), Instant.EPOCH);
  }

  @Test
  void readEntryBypassesAStaleCacheUntilInvalidationSucceeds() {
    Entry committed = entry("e1", 2, T0.plusSeconds(3));
    doThrow(new IllegalStateException("Redis write failed")).when(cache).putEntry(any());
    doThrow(new IllegalStateException("Redis invalidate failed"))
        .when(cache).invalidateWaitlist("wl-1");
    when(storage.findEntry("wl-1", "e1")).thenReturn(Optional.of(committed));

    consistency.replaceCacheAfterCommit("wl-1", committed, List.of("e1"));

    assertEquals(committed, consistency.readEntry("wl-1", "e1"));
    verify(cache, never()).findEntry("wl-1", "e1");

    reset(cache);
    when(storage.findEntry("wl-1", "e1")).thenReturn(Optional.of(committed));
    when(cache.findEntry("wl-1", "e1")).thenReturn(Optional.empty());

    assertEquals(committed, consistency.readEntry("wl-1", "e1"));
    verify(cache).invalidateWaitlist("wl-1");
    verify(cache).putEntry(committed);
  }

  @Test
  void readWaitingOrderBypassesAStaleRankUntilInvalidationSucceeds() {
    Entry committed = entry("e1", 2, T0.plusSeconds(3));
    doThrow(new IllegalStateException("Redis write failed")).when(cache).putEntry(any());
    doThrow(new IllegalStateException("Redis invalidate failed"))
        .when(cache).invalidateWaitlist("wl-1");
    when(storage.findEntriesByWaitlistId("wl-1")).thenReturn(List.of(committed));

    consistency.replaceCacheAfterCommit("wl-1", committed, List.of("e1"));

    assertEquals(List.of("e1"), consistency.readWaitingOrder("wl-1"));
    verify(cache, never()).findWaitingOrder("wl-1");
  }

  private static Entry entry(String id, int priority) {
    return entry(id, priority, null);
  }

  private static Entry entry(String id, int priority, Instant updatedAt) {
    return new Entry(id, "wl-1", id, priority, EntryStatus.WAITING, T0, null, updatedAt);
  }
}
