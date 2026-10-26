package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.response.DeleteEntryResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Removes an entry using the authoritative PostgreSQL delete result.
 *
 * <p>AI-assisted.
 */
@Service
public class DeleteEntryService {

  private final WaitlistStorage storage;
  private final CacheConsistencyService consistency;

  /**
   * Creates the entry removal service.
   *
   * @param storage persistent waitlist and entry storage
   * @param consistency PostgreSQL-then-Redis coordinator
   */
  public DeleteEntryService(@Lazy WaitlistStorage storage, CacheConsistencyService consistency) {
    this.storage = storage;
    this.consistency = consistency;
  }

  /**
   * Removes an entry or reports an unknown waitlist or entry.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry to remove
   * @return removal confirmation
   */
  public DeleteEntryResponse deleteEntry(String waitlistId, String entryId) {
    storage.findWaitlistById(waitlistId)
        .orElseThrow(() -> ApiException.notFound("Waitlist does not exist"));
    if (!storage.deleteEntry(waitlistId, entryId)) {
      throw ApiException.notFound("Entry does not exist");
    }
    consistency.dropEntryAfterCommit(waitlistId, entryId);
    return new DeleteEntryResponse("Entry successfully removed", entryId, waitlistId);
  }
}
