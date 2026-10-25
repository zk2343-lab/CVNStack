package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.WaitEstimate;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.response.EntryResponse;
import java.util.List;
import java.util.OptionalInt;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Reads an entry and calculates its current position and wait estimate.
 *
 * <p>AI-assisted.
 */
@Service
public class EntryQueryService {

  private final WaitlistStorage storage;
  private final CacheConsistencyService consistency;

  /**
   * Creates the entry lookup service.
   *
   * @param storage source of waitlists
   * @param consistency cache rebuild from PostgreSQL
   */
  public EntryQueryService(@Lazy WaitlistStorage storage, CacheConsistencyService consistency) {
    this.storage = storage;
    this.consistency = consistency;
  }

  /**
   * Returns an entry without changing stored data.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry id
   * @return entry details and waiting information
   */
  public EntryResponse getEntry(String waitlistId, String entryId) {
    Waitlist waitlist = storage.findWaitlistById(waitlistId)
        .orElseThrow(() -> ApiException.notFound("Waitlist does not exist"));
    Entry entry = consistency.readEntry(waitlistId, entryId);

    Integer position = null;
    Integer estimatedWaitTimeMinutes = null;
    if (entry.status() == EntryStatus.WAITING) {
      List<String> waitingIds = consistency.readWaitingOrder(waitlistId);
      int index = waitingIds.indexOf(entry.id());
      if (index < 0) {
        waitingIds = consistency.rebuildWaitingOrder(waitlistId);
        index = waitingIds.indexOf(entry.id());
      }
      if (index < 0) {
        throw ApiException.notFound("Entry is no longer waiting");
      }
      position = index + 1;
      estimatedWaitTimeMinutes = WaitEstimate.minutes(
          OptionalInt.of(position - 1), waitlist.processingTimePerEntryMinutes()).getAsInt();
    }

    return new EntryResponse(waitlistId, entry.id(), entry.status(), entry.priority(),
        position, estimatedWaitTimeMinutes);
  }
}
