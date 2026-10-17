package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import org.springframework.stereotype.Service;

/**
 * Application service for waitlist capacity and conflict rules.
 *
 * <p>AI-assisted.
 */
@Service
public class WaitlistRulesService {

  /**
   * Ensures adding one more waiting entry would not exceed capacity.
   *
   * @param waitingCount current waiting entries
   * @param maxLength waitlist capacity
   */
  public void requireCapacity(final long waitingCount, final int maxLength) {
    if (waitingCount >= maxLength) {
      throw ApiException.capacityExceeded("Waitlist is full");
    }
  }

  /**
   * Ensures a waitlist name is not already taken.
   *
   * @param nameAlreadyExists true when the name is present in storage
   */
  public void requireUniqueName(final boolean nameAlreadyExists) {
    if (nameAlreadyExists) {
      throw ApiException.conflict("Waitlist name already exists");
    }
  }

  /**
   * Ensures a waitlist or entry was found; otherwise raises {@code NOT_FOUND}.
   *
   * @param present true when the lookup succeeded
   * @param message explanation for the client
   */
  public void requireFound(final boolean present, final String message) {
    if (!present) {
      throw ApiException.notFound(message);
    }
  }
}
