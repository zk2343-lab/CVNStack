package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.response.ServeNextResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Selects a waiting entry and asks storage to claim it without an unconditional save.
 *
 * <p>AI-assisted.
 */
@Service
public class ServeNextService {

  private final WaitlistStorage storage;
  private final CacheStorage cache;
  private final EntryRulesService rules;

  /**
   * Creates the service with the shared storage ports and ranking rules.
   *
   * @param storage waitlist and entry storage
   * @param cache waiting order and entry cache
   * @param rules shared ranking rules
   */
  public ServeNextService(
      @Lazy WaitlistStorage storage, CacheStorage cache, EntryRulesService rules) {
    this.storage = storage;
    this.cache = cache;
    this.rules = rules;
  }

  /**
   * Calls the first available waiting entry in priority and insertion order.
   *
   * @param waitlistId waitlist to serve
   * @return selected entry, or empty when no waiting candidate can be claimed
   */
  public Optional<ServeNextResponse> serveNext(String waitlistId) {
    storage.findWaitlistById(waitlistId)
        .orElseThrow(() -> ApiException.notFound("Waitlist does not exist"));

    List<Entry> waiting = rules.rankedWaiting(storage.findEntriesByWaitlistId(waitlistId));
    for (Entry candidate : waiting) {
      // A competing request may already have claimed this candidate.
      Optional<Entry> claimed = storage.callEntryIfWaiting(
          waitlistId, candidate.id(), Instant.now());
      if (claimed.isPresent()) {
        Entry called = claimed.get();
        // Invalidate after the committed change; positions are rebuilt from storage.
        cache.invalidateWaitlist(waitlistId);
        return Optional.of(new ServeNextResponse(
            called.waitlistId(), called.id(), called.status(), called.priority()));
      }
    }
    return Optional.empty();
  }
}
