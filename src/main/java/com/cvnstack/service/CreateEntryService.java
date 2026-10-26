package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.request.CreateEntryRequest;
import com.cvnstack.model.response.EntryResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Adds an entry with atomic capacity enforcement and calculates its waiting information.
 *
 * <p>AI-assisted.
 */
@Service
public class CreateEntryService {

  private final WaitlistStorage storage;
  private final CacheConsistencyService consistency;
  private final EntryRulesService rules;

  /**
   * Creates the service using shared storage ports and ranking rules.
   *
   * @param storage persistent waitlist and entry storage
   * @param consistency PostgreSQL-then-Redis coordinator
   * @param rules shared entry rules
   */
  public CreateEntryService(
      @Lazy WaitlistStorage storage, CacheConsistencyService consistency, EntryRulesService rules) {
    this.storage = storage;
    this.consistency = consistency;
    this.rules = rules;
  }

  /**
   * Validates and inserts one waiting entry, then returns its position and estimate.
   *
   * @param waitlistId owning waitlist id from the path
   * @param request entry name and optional priority
   * @return persisted entry details with waiting information
   */
  public EntryResponse createEntry(String waitlistId, CreateEntryRequest request) {
    if (request == null || request.name() == null || request.name().isBlank()) {
      throw ApiException.badRequest("Name must not be blank");
    }
    int priority = request.priority() == null ? Entry.DEFAULT_PRIORITY : request.priority();
    if (priority < Entry.MIN_PRIORITY || priority > Entry.MAX_PRIORITY) {
      throw ApiException.badRequest("Priority must be between 1 and 5");
    }
    Waitlist waitlist = storage.findWaitlistById(waitlistId)
        .orElseThrow(() -> ApiException.notFound("Waitlist does not exist"));
    Entry entry = new Entry(UUID.randomUUID().toString(), waitlistId, request.name(),
        priority, EntryStatus.WAITING, Instant.now());
    Entry savedEntry = storage.insertEntryIfCapacityAvailable(entry);
    List<Entry> entries = storage.findEntriesByWaitlistId(waitlistId);
    List<Entry> ranked = rules.rankedWaiting(entries);
    int position = rules.positionOf(savedEntry, ranked).orElseThrow();
    int estimate = rules.estimateMinutes(
        savedEntry, ranked, waitlist.processingTimePerEntryMinutes()).orElseThrow();
    consistency.replaceCacheAfterCommit(
        waitlistId, savedEntry, ranked.stream().map(Entry::id).toList());
    return new EntryResponse(savedEntry.waitlistId(), savedEntry.id(), savedEntry.status(),
        savedEntry.priority(), position, estimate);
  }
}
