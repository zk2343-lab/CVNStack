package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.request.PatchEntryRequest;
import com.cvnstack.model.response.PatchEntryResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Applies validated partial entry updates using PostgreSQL optimistic concurrency checks.
 *
 * <p>AI-assisted.
 */
@Service
public class PatchEntryService {

  private final WaitlistStorage storage;
  private final EntryRulesService rules;

  /**
   * Creates the service using persisted entries and shared rules.
   *
   * @param storage PostgreSQL storage port
   * @param rules entry validation and ranking rules
   */
  public PatchEntryService(@Lazy WaitlistStorage storage, EntryRulesService rules) {
    this.storage = storage;
    this.rules = rules;
  }

  /**
   * Updates supplied fields, re-reading and re-validating after a competing write.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry id
   * @param request requested fields
   * @return persisted entry with derived waiting information
   */
  public PatchEntryResponse patchEntry(
      String waitlistId, String entryId, PatchEntryRequest request) {
    if (request == null || (request.status() == null && request.priority() == null)) {
      throw ApiException.badRequest("At least one of status or priority must be provided");
    }
    if (request.priority() != null
        && (request.priority() < Entry.MIN_PRIORITY || request.priority() > Entry.MAX_PRIORITY)) {
      throw ApiException.badRequest("Priority must be between 1 and 5");
    }
    Waitlist waitlist = storage.findWaitlistById(waitlistId)
        .orElseThrow(() -> ApiException.notFound("Waitlist does not exist"));
    Entry current = findEntry(waitlistId, entryId);
    while (true) {
      boolean statusChanged = request.status() != null && request.status() != current.status();
      boolean priorityChanged = request.priority() != null
          && request.priority() != current.priority();
      if (!statusChanged && !priorityChanged) {
        return response(current, waitlist);
      }
      if (statusChanged) {
        rules.requireTransition(current.status(), request.status());
      }
      if (priorityChanged) {
        rules.requirePriorityChangeAllowed(current.status());
      }
      Instant calledAt = statusChanged && request.status() == EntryStatus.CALLED
          ? Instant.now() : current.calledAt();
      Entry desired = new Entry(current.id(), current.waitlistId(), current.name(),
          priorityChanged ? request.priority() : current.priority(),
          statusChanged ? request.status() : current.status(), current.insertedAt(),
          calledAt, current.updatedAt());
      Optional<Entry> updated = storage.updateEntryIfUnchanged(desired, current);
      if (updated.isPresent()) {
        return response(updated.get(), waitlist);
      }
      // A failed compare-and-set must never reuse validation against the stale row.
      current = findEntry(waitlistId, entryId);
    }
  }

  private Entry findEntry(String waitlistId, String entryId) {
    return storage.findEntry(waitlistId, entryId)
        .orElseThrow(() -> ApiException.notFound("Entry does not exist"));
  }

  private PatchEntryResponse response(Entry entry, Waitlist waitlist) {
    Integer position = null;
    Integer estimate = null;
    if (entry.status() == EntryStatus.WAITING) {
      List<Entry> entries = storage.findEntriesByWaitlistId(waitlist.id());
      position = rules.positionOf(entry, entries)
          .orElseThrow(() -> ApiException.notFound("Entry is no longer waiting"));
      estimate = rules.estimateMinutes(entry, entries, waitlist.processingTimePerEntryMinutes())
          .orElseThrow(() -> ApiException.notFound("Entry is no longer waiting"));
    }
    return new PatchEntryResponse(entry.waitlistId(), entry.id(), entry.status(), entry.priority(),
        entry.updatedAt(), position, estimate);
  }
}
