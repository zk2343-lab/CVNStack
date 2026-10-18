package com.cvnstack.model;

import java.util.OptionalInt;

/**
 * Wait-time estimate from entries ahead and configured processing time.
 *
 * <p>AI-assisted.
 */
public final class WaitEstimate {

  private WaitEstimate() {}

  /**
   * Minutes until service for a waiting entry.
   *
   * <p>Returns empty when the entry is not waiting. This iteration does not use
   * completion history.
   *
   * @param entriesAhead waiting entries ranked ahead of the target
   * @param processingTimePerEntryMinutes configured minutes per entry
   * @return estimated minutes, or empty when not applicable
   */
  public static OptionalInt minutes(
      final OptionalInt entriesAhead, final int processingTimePerEntryMinutes) {
    if (entriesAhead.isEmpty()) {
      return OptionalInt.empty();
    }
    if (processingTimePerEntryMinutes < 0) {
      throw new IllegalArgumentException("processingTimePerEntryMinutes must not be negative");
    }
    return OptionalInt.of(entriesAhead.getAsInt() * processingTimePerEntryMinutes);
  }
}
