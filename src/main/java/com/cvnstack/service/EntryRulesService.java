package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.EntryTransitions;
import com.cvnstack.model.WaitEstimate;
import com.cvnstack.model.WaitingOrder;
import java.util.List;
import java.util.OptionalInt;
import org.springframework.stereotype.Service;

/**
 * Application service for entry ranking, estimates, and status rules.
 *
 * <p>AI-assisted.
 */
@Service
public class EntryRulesService {

  /**
   * Returns waiting entries in serve order.
   *
   * @param entries entries on a waitlist
   * @return ranked waiting entries
   */
  public List<Entry> rankedWaiting(final List<Entry> entries) {
    return WaitingOrder.rankedWaiting(entries);
  }

  /**
   * One-based position among waiting entries, or empty when not waiting.
   *
   * @param target entry to locate
   * @param entries entries on the same waitlist
   * @return position or empty
   */
  public OptionalInt positionOf(final Entry target, final List<Entry> entries) {
    return WaitingOrder.positionOf(target, entries);
  }

  /**
   * Wait estimate in minutes for a waiting entry.
   *
   * @param target entry to estimate
   * @param entries entries on the same waitlist
   * @param processingTimePerEntryMinutes waitlist processing time
   * @return minutes or empty when not waiting
   */
  public OptionalInt estimateMinutes(
      final Entry target,
      final List<Entry> entries,
      final int processingTimePerEntryMinutes) {
    return WaitEstimate.minutes(
        WaitingOrder.entriesAhead(target, entries), processingTimePerEntryMinutes);
  }

  /**
   * Ensures a status change is allowed; otherwise raises {@code
   * INVALID_STATE_TRANSITION}.
   *
   * @param from current status
   * @param to requested status
   */
  public void requireTransition(final EntryStatus from, final EntryStatus to) {
    if (!EntryTransitions.isAllowed(from, to)) {
      throw ApiException.invalidStateTransition(
          "Cannot transition state from " + from + " to " + to);
    }
  }

  /**
   * Ensures priority may change in {@code status}; otherwise raises {@code
   * INVALID_REQUEST}.
   *
   * @param status current status
   */
  public void requirePriorityChangeAllowed(final EntryStatus status) {
    if (!EntryTransitions.canChangePriority(status)) {
      throw ApiException.invalidRequest(
          "Priority can only change while WAITING or CALLED");
    }
  }
}
