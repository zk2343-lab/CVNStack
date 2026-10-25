package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.request.CreateWaitlistRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Validates and persists new waitlists with server-generated identifiers.
 *
 * <p>AI-assisted.
 */
@Service
public class CreateWaitlistService {

  private final WaitlistStorage storage;

  /**
   * Creates the service with the persistence port.
   *
   * @param storage waitlist persistence
   */
  public CreateWaitlistService(WaitlistStorage storage) {
    this.storage = storage;
  }

  /**
   * Creates a waitlist and returns the persisted value.
   *
   * @param request client settings
   * @return stored waitlist
   */
  public Waitlist createWaitlist(CreateWaitlistRequest request) {
    if (request == null || request.name() == null || request.name().isBlank()) {
      throw ApiException.badRequest("name must not be blank");
    }
    if (request.maxLength() <= 0) {
      throw ApiException.badRequest("maxLength must be positive");
    }
    if (request.processingTimePerEntryMinutes() <= 0) {
      throw ApiException.badRequest("processingTimePerEntryMinutes must be positive");
    }
    return storage.saveWaitlist(new Waitlist(
        UUID.randomUUID().toString(), request.name(), request.maxLength(),
        request.processingTimePerEntryMinutes()));
  }
}
