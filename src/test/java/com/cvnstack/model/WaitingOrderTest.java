package com.cvnstack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * Checks priority-then-insertion ranking and positions.
 *
 * <p>AI-assisted.
 */
class WaitingOrderTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant T1 = Instant.parse("2026-01-01T00:01:00Z");
  private static final Instant T2 = Instant.parse("2026-01-01T00:02:00Z");

  @Test
  void higherPriorityRanksBeforeLowerPriority() {
    final Entry low = entry("a", 1, EntryStatus.WAITING, T0);
    final Entry high = entry("b", 5, EntryStatus.WAITING, T1);

    assertEquals(List.of(high, low), WaitingOrder.rankedWaiting(List.of(low, high)));
  }

  @Test
  void earlierInsertionBreaksPriorityTies() {
    final Entry later = entry("later", 3, EntryStatus.WAITING, T2);
    final Entry earlier = entry("earlier", 3, EntryStatus.WAITING, T0);

    assertEquals(List.of(earlier, later), WaitingOrder.rankedWaiting(List.of(later, earlier)));
  }

  @Test
  void nonWaitingEntriesAreExcludedFromRank() {
    final Entry waiting = entry("w", 1, EntryStatus.WAITING, T0);
    final Entry called = entry("c", 5, EntryStatus.CALLED, T1);

    assertEquals(List.of(waiting), WaitingOrder.rankedWaiting(List.of(called, waiting)));
  }

  @Test
  void positionIsOneBasedAmongWaitingEntries() {
    final Entry first = entry("first", 5, EntryStatus.WAITING, T0);
    final Entry second = entry("second", 1, EntryStatus.WAITING, T1);

    assertEquals(OptionalInt.of(1), WaitingOrder.positionOf(first, List.of(first, second)));
    assertEquals(OptionalInt.of(2), WaitingOrder.positionOf(second, List.of(first, second)));
    assertEquals(OptionalInt.of(0), WaitingOrder.entriesAhead(first, List.of(first, second)));
    assertEquals(OptionalInt.of(1), WaitingOrder.entriesAhead(second, List.of(first, second)));
  }

  @Test
  void positionEmptyWhenNotWaiting() {
    final Entry served = entry("s", 5, EntryStatus.SERVED, T0);

    assertTrue(WaitingOrder.positionOf(served, List.of(served)).isEmpty());
    assertTrue(WaitingOrder.entriesAhead(served, List.of(served)).isEmpty());
  }

  private static Entry entry(
      final String id, final int priority, final EntryStatus status, final Instant insertedAt) {
    return new Entry(id, "wl-1", id, priority, status, insertedAt);
  }
}
