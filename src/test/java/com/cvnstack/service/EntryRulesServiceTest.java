package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cvnstack.error.ApiException;
import com.cvnstack.error.ErrorCode;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks that entry rules use model classes and shared error codes.
 *
 * <p>AI-assisted.
 */
class EntryRulesServiceTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant T1 = Instant.parse("2026-01-01T00:01:00Z");

  private EntryRulesService service;

  @BeforeEach
  void setUp() {
    service = new EntryRulesService();
  }

  @Test
  void ranksAndEstimatesUsingModel() {
    final Entry high = entry("b", 5, T1);
    final Entry low = entry("a", 1, T0);

    assertEquals(List.of(high, low), service.rankedWaiting(List.of(low, high)));
    assertEquals(OptionalInt.of(2), service.positionOf(low, List.of(low, high)));
    assertEquals(OptionalInt.of(5), service.estimateMinutes(low, List.of(low, high), 5));
  }

  @Test
  void illegalTransitionReferencesInvalidStateTransitionCode() {
    final ApiException exception =
        assertThrows(
            ApiException.class,
            () -> service.requireTransition(EntryStatus.SERVED, EntryStatus.WAITING));

    assertEquals(ErrorCode.INVALID_STATE_TRANSITION, exception.errorCode());
  }

  @Test
  void illegalPriorityChangeReferencesInvalidRequestCode() {
    final ApiException exception =
        assertThrows(
            ApiException.class,
            () -> service.requirePriorityChangeAllowed(EntryStatus.CANCELLED));

    assertEquals(ErrorCode.INVALID_REQUEST, exception.errorCode());
  }

  private static Entry entry(final String id, final int priority, final Instant insertedAt) {
    return new Entry(id, "wl-1", id, priority, EntryStatus.WAITING, insertedAt);
  }
}
