package com.cvnstack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Checks priority bounds and preservation of the stored call timestamp.
 *
 * <p>AI-assisted.
 */
class EntryTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

  @Test
  void acceptsPriorityBounds() {
    final Entry low =
        new Entry("e1", "w1", "a", Entry.MIN_PRIORITY, EntryStatus.WAITING, T0);
    final Entry high =
        new Entry("e2", "w1", "b", Entry.MAX_PRIORITY, EntryStatus.WAITING, T0);

    assertEquals(Entry.MIN_PRIORITY, low.priority());
    assertEquals(Entry.MAX_PRIORITY, high.priority());
  }

  @Test
  void rejectsPriorityOutsideRange() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Entry("e1", "w1", "a", 0, EntryStatus.WAITING, T0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Entry("e1", "w1", "a", 6, EntryStatus.WAITING, T0));
  }

  @Test
  void existingConstructorLeavesCallTimeUnset() {
    Entry entry = new Entry("e1", "w1", "a", 1, EntryStatus.WAITING, T0);

    assertNull(entry.calledAt());
  }

  @Test
  void statusAndPriorityChangesPreserveCallTime() {
    Instant calledAt = T0.plusSeconds(60);
    Entry entry = new Entry("e1", "w1", "a", 1, EntryStatus.CALLED, T0, calledAt);

    assertEquals(calledAt, entry.calledAt());
    assertEquals(calledAt, entry.withPriority(5).calledAt());
    assertEquals(calledAt, entry.withStatus(EntryStatus.SERVED).calledAt());
  }

  @Test
  void equalityIncludesCallTime() {
    Entry first = new Entry("e1", "w1", "a", 1, EntryStatus.CALLED, T0, T0.plusSeconds(60));
    Entry same = new Entry("e1", "w1", "a", 1, EntryStatus.CALLED, T0, T0.plusSeconds(60));
    Entry later = new Entry("e1", "w1", "a", 1, EntryStatus.CALLED, T0, T0.plusSeconds(90));

    assertEquals(first, same);
    assertEquals(first.hashCode(), same.hashCode());
    assertNotEquals(first, later);
  }
}
