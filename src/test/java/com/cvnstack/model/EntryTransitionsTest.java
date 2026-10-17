package com.cvnstack.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Checks allowed and rejected status transitions.
 *
 * <p>AI-assisted.
 */
class EntryTransitionsTest {

  @Test
  void allowsWaitingToCalledAndCancelled() {
    assertTrue(EntryTransitions.isAllowed(EntryStatus.WAITING, EntryStatus.CALLED));
    assertTrue(EntryTransitions.isAllowed(EntryStatus.WAITING, EntryStatus.CANCELLED));
  }

  @Test
  void allowsCalledToServedAndCancelled() {
    assertTrue(EntryTransitions.isAllowed(EntryStatus.CALLED, EntryStatus.SERVED));
    assertTrue(EntryTransitions.isAllowed(EntryStatus.CALLED, EntryStatus.CANCELLED));
  }

  @Test
  void rejectsIllegalTransitions() {
    assertFalse(EntryTransitions.isAllowed(EntryStatus.WAITING, EntryStatus.SERVED));
    assertFalse(EntryTransitions.isAllowed(EntryStatus.CALLED, EntryStatus.WAITING));
    assertFalse(EntryTransitions.isAllowed(EntryStatus.SERVED, EntryStatus.WAITING));
    assertFalse(EntryTransitions.isAllowed(EntryStatus.CANCELLED, EntryStatus.WAITING));
    assertFalse(EntryTransitions.isAllowed(EntryStatus.SERVED, EntryStatus.CANCELLED));
    assertFalse(EntryTransitions.isAllowed(EntryStatus.WAITING, EntryStatus.WAITING));
  }

  @Test
  void priorityChangeOnlyWhileActive() {
    assertTrue(EntryTransitions.canChangePriority(EntryStatus.WAITING));
    assertTrue(EntryTransitions.canChangePriority(EntryStatus.CALLED));
    assertFalse(EntryTransitions.canChangePriority(EntryStatus.SERVED));
    assertFalse(EntryTransitions.canChangePriority(EntryStatus.CANCELLED));
  }
}
