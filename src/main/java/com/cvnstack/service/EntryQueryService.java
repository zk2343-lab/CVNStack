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
  private final EntryRulesService rules;

  /**
   * Creates the entry lookup service.
   *
   * @param storage source of waitlists and entries
   * @param rules shared ranking rules
   */
  public EntryQueryService(@Lazy WaitlistStorage storage, EntryRulesService rules) {
    this.storage = storage;
    this.rules = rules;
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
    Entry entry = storage.findEntry(waitlistId, entryId)
        .orElseThrow(() -> ApiException.notFound("Entry does not exist"));
    if (!waitlistId.equals(entry.waitlistId())) {
      throw ApiException.notFound("Entry does not exist on this waitlist");
    }

    Integer position = null;
    Integer estimatedWaitTimeMinutes = null;
    if (entry.status() == EntryStatus.WAITING) {
      List<Entry> entries = storage.findEntriesByWaitlistId(waitlistId);
      position = rules.positionOf(entry, entries)
          .orElseThrow(() -> ApiException.notFound("Entry is no longer waiting"));
      estimatedWaitTimeMinutes = WaitEstimate.minutes(
          OptionalInt.of(position - 1), waitlist.processingTimePerEntryMinutes()).getAsInt();
    }

    return new EntryResponse(waitlistId, entry.id(), entry.status(), entry.priority(),
        position, estimatedWaitTimeMinutes);
  }
}
