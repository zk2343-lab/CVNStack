package com.cvnstack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * Checks wait estimates from entries ahead and processing time.
 *
 * <p>AI-assisted.
 */
class WaitEstimateTest {

  @Test
  void multipliesEntriesAheadByProcessingTime() {
    assertEquals(OptionalInt.of(15), WaitEstimate.minutes(OptionalInt.of(3), 5));
    assertEquals(OptionalInt.of(0), WaitEstimate.minutes(OptionalInt.of(0), 5));
  }

  @Test
  void emptyWhenNotWaiting() {
    assertTrue(WaitEstimate.minutes(OptionalInt.empty(), 5).isEmpty());
  }

  @Test
  void rejectsNegativeProcessingTime() {
    assertThrows(
        IllegalArgumentException.class, () -> WaitEstimate.minutes(OptionalInt.of(1), -1));
  }
}
