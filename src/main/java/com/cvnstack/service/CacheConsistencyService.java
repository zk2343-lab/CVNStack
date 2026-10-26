package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Write-through coordinator: PostgreSQL commits first, then Redis is updated to match.
 *
 * <p>AI-assisted.
 */
@Service
public class CacheConsistencyService {

  private final WaitlistStorage storage;
  private final CacheStorage cache;
  private final EntryRulesService rules;
  private final Set<String> staleWaitlists = ConcurrentHashMap.newKeySet();

  /**
   * Creates the coordinator for the two storage ports.
   *
   * @param storage PostgreSQL source of truth
   * @param cache Redis rank, entry, and lock adapter
   * @param rules waiting-order rules used to rebuild ranks
   */
  public CacheConsistencyService(
      @Lazy WaitlistStorage storage, CacheStorage cache, EntryRulesService rules) {
    this.storage = storage;
    this.cache = cache;
    this.rules = rules;
  }

  /**
   * Writes the committed entry and waiting order through to Redis.
   *
   * <p>Older write-throughs do not replace a newer cached payload or rank. A Redis
   * failure deletes the waitlist rank key and entry cache keys so the next read
   * rebuilds from PostgreSQL. The committed row is never rolled back.
   *
   * @param waitlistId waitlist whose cache should match the database
   * @param entry committed entry to cache, or {@code null} when only the rank changes
   * @param waitingEntryIds current waiting ids, highest rank first
   */
  public void replaceCacheAfterCommit(
      String waitlistId, Entry entry, List<String> waitingEntryIds) {
    afterCommit(waitlistId, () -> {
      if (entry != null) {
        cache.putEntry(entry);
      }
      writeWaitingOrder(waitlistId, entry, waitingEntryIds);
    });
  }

  /**
   * Drops one committed deletion from Redis, or invalidates the waitlist on failure.
   *
   * @param waitlistId owning waitlist
   * @param entryId removed entry
   */
  public void dropEntryAfterCommit(String waitlistId, String entryId) {
    afterCommit(waitlistId, () -> {
      cache.deleteEntry(waitlistId, entryId);
      cache.removeWaitingMember(waitlistId, entryId);
    });
  }

  /**
   * Writes the called entry through to Redis and drops it from the waiting order.
   *
   * @param waitlistId owning waitlist
   * @param called committed {@code CALLED} entry
   */
  public void writeThroughCalledEntry(String waitlistId, Entry called) {
    afterCommit(waitlistId, () -> {
      cache.putEntry(called);
      cache.removeWaitingMember(waitlistId, called.id());
    });
  }

  /**
   * Returns a cached entry, or loads it from PostgreSQL and stores the rebuilt payload.
   *
   * @param waitlistId owning waitlist
   * @param entryId entry to read
   * @return persisted entry
   */
  public Entry readEntry(String waitlistId, String entryId) {
    if (mustBypassCache(waitlistId)) {
      return requireEntry(waitlistId, entryId);
    }
    Optional<Entry> cached = cache.findEntry(waitlistId, entryId);
    if (cached != null && cached.isPresent() && waitlistId.equals(cached.get().waitlistId())) {
      return cached.get();
    }
    Entry entry = requireEntry(waitlistId, entryId);
    try {
      cache.putEntry(entry);
    } catch (RuntimeException ignored) {
      // The response still uses the PostgreSQL row.
    }
    return entry;
  }

  /**
   * Returns the cached waiting order, or rebuilds it from PostgreSQL before responding.
   *
   * @param waitlistId waitlist to rank
   * @return waiting entry ids, highest rank first
   */
  public List<String> readWaitingOrder(String waitlistId) {
    if (mustBypassCache(waitlistId)) {
      return rankedIdsFromStorage(waitlistId);
    }
    Optional<List<String>> cached = cache.findWaitingOrder(waitlistId);
    if (cached != null && cached.isPresent()) {
      return cached.get();
    }
    return rebuildWaitingOrder(waitlistId);
  }

  /**
   * Rebuilds the rank key from PostgreSQL and replaces the cached order when Redis allows it.
   *
   * @param waitlistId waitlist to rebuild
   * @return waiting entry ids, highest rank first
   */
  public List<String> rebuildWaitingOrder(String waitlistId) {
    List<Entry> waiting = rules.rankedWaiting(storage.findEntriesByWaitlistId(waitlistId));
    List<String> waitingIds = waiting.stream().map(Entry::id).toList();
    Instant revision = waiting.stream()
        .map(Entry::updatedAt)
        .filter(Objects::nonNull)
        .max(Instant::compareTo)
        .orElse(Instant.EPOCH);
    try {
      cache.replaceWaitingOrderIfNewer(waitlistId, waitingIds, revision);
    } catch (RuntimeException ignored) {
      // Position still comes from the rebuilt PostgreSQL order.
    }
    return waitingIds;
  }

  private void afterCommit(String waitlistId, Runnable update) {
    try {
      update.run();
      staleWaitlists.remove(waitlistId);
    } catch (RuntimeException failedUpdate) {
      invalidateQuietly(waitlistId);
    }
  }

  private void writeWaitingOrder(String waitlistId, Entry entry, List<String> waitingEntryIds) {
    Instant revision = entry == null ? null : entry.updatedAt();
    if (revision != null) {
      cache.replaceWaitingOrderIfNewer(waitlistId, waitingEntryIds, revision);
      return;
    }
    cache.replaceWaitingOrder(waitlistId, waitingEntryIds);
  }

  private Entry requireEntry(String waitlistId, String entryId) {
    Entry entry = storage.findEntry(waitlistId, entryId)
        .orElseThrow(() -> ApiException.notFound("Entry does not exist"));
    if (!waitlistId.equals(entry.waitlistId())) {
      throw ApiException.notFound("Entry does not exist on this waitlist");
    }
    return entry;
  }

  private List<String> rankedIdsFromStorage(String waitlistId) {
    return rules.rankedWaiting(storage.findEntriesByWaitlistId(waitlistId))
        .stream()
        .map(Entry::id)
        .toList();
  }

  private boolean mustBypassCache(String waitlistId) {
    return staleWaitlists.contains(waitlistId) && !clearStaleCache(waitlistId);
  }

  private boolean clearStaleCache(String waitlistId) {
    try {
      cache.invalidateWaitlist(waitlistId);
      staleWaitlists.remove(waitlistId);
      return true;
    } catch (RuntimeException ignored) {
      return false;
    }
  }

  private void invalidateQuietly(String waitlistId) {
    if (!clearStaleCache(waitlistId)) {
      staleWaitlists.add(waitlistId);
    }
  }
}
