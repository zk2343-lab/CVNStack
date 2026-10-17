package com.cvnstack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Checks entry construction bounds for priority.
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
}
