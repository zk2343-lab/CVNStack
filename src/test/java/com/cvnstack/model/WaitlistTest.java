package com.cvnstack.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Checks positive capacity and processing-time invariants on waitlists.
 *
 * <p>AI-assisted.
 */
class WaitlistTest {

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void rejectsNonPositiveProcessingTime(int minutes) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> new Waitlist("id", "Support", 10, minutes));
    assertEquals("processingTimePerEntryMinutes must be positive", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void rejectsNonPositiveCapacity(int capacity) {
    assertThrows(IllegalArgumentException.class,
        () -> new Waitlist("id", "Support", capacity, 5));
  }

  @Test
  void acceptsPositiveSettings() {
    Waitlist waitlist = new Waitlist("id", "Support", 1, 1);
    assertEquals(1, waitlist.maxLength());
    assertEquals(1, waitlist.processingTimePerEntryMinutes());
  }
}
