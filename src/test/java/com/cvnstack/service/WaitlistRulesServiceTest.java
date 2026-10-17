package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cvnstack.error.ApiException;
import com.cvnstack.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks that waitlist rules reference shared error codes.
 *
 * <p>AI-assisted.
 */
class WaitlistRulesServiceTest {

  private WaitlistRulesService service;

  @BeforeEach
  void setUp() {
    service = new WaitlistRulesService();
  }

  @Test
  void capacityExceededReferencesCapacityCode() {
    final ApiException exception =
        assertThrows(ApiException.class, () -> service.requireCapacity(3, 3));

    assertEquals(ErrorCode.CAPACITY_EXCEEDED, exception.errorCode());
  }

  @Test
  void allowsAddWhenBelowCapacity() {
    assertDoesNotThrow(() -> service.requireCapacity(2, 3));
  }

  @Test
  void duplicateNameReferencesConflictCode() {
    final ApiException exception =
        assertThrows(ApiException.class, () -> service.requireUniqueName(true));

    assertEquals(ErrorCode.CONFLICT, exception.errorCode());
  }

  @Test
  void missingResourceReferencesNotFoundCode() {
    final ApiException exception =
        assertThrows(
            ApiException.class, () -> service.requireFound(false, "Waitlist does not exist"));

    assertEquals(ErrorCode.NOT_FOUND, exception.errorCode());
  }
}
