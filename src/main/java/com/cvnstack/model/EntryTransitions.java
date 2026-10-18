package com.cvnstack.model;

import java.util.Objects;

/**
 * Allowed status changes and when priority may change.
 *
 * <p>AI-assisted.
 */
public final class EntryTransitions {

  private EntryTransitions() {}

  /**
   * Whether {@code from} may move to {@code to}.
   *
   * <p>{@code WAITING} → {@code CALLED} → {@code SERVED}. {@code WAITING} or
   * {@code CALLED} → {@code CANCELLED}. {@code SERVED} and {@code CANCELLED} are
   * terminal.
   *
   * @param from current status
   * @param to requested status
   * @return true when the transition is allowed
   */
  public static boolean isAllowed(final EntryStatus from, final EntryStatus to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (from == to) {
      return false;
    }
    return switch (from) {
      case WAITING -> to == EntryStatus.CALLED || to == EntryStatus.CANCELLED;
      case CALLED -> to == EntryStatus.SERVED || to == EntryStatus.CANCELLED;
      case SERVED, CANCELLED -> false;
    };
  }

  /**
   * Whether priority may change while the entry is in {@code status}.
   *
   * @param status current status
   * @return true for {@code WAITING} or {@code CALLED}
   */
  public static boolean canChangePriority(final EntryStatus status) {
    Objects.requireNonNull(status, "status");
    return status == EntryStatus.WAITING || status == EntryStatus.CALLED;
  }
}
