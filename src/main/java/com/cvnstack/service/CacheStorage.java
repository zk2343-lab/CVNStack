package com.cvnstack.service;

import com.cvnstack.model.Entry;
import java.util.List;
import java.util.Optional;

/**
 * Cache storage service for waiting order, serve locks, and entry payloads.
 * Redis impl arrives in issue 19.
 *
 * <p>AI-assisted.
 */
public interface CacheStorage {

  /**
   * Replaces the ranked waiting entry ids for a waitlist.
   *
   * @param waitlistId waitlist id
   * @param entryIdsInOrder waiting entry ids, highest rank first
   */
  void replaceWaitingOrder(String waitlistId, List<String> entryIdsInOrder);

  /**
   * Reads the cached waiting order when present.
   *
   * @param waitlistId waitlist id
   * @return ordered entry ids when cached
   */
  Optional<List<String>> findWaitingOrder(String waitlistId);

  /**
   * Stores a read-through entry payload.
   *
   * @param entry entry to cache
   */
  void putEntry(Entry entry);

  /**
   * Reads a cached entry payload.
   *
   * @param waitlistId waitlist id
   * @param entryId entry id
   * @return entry when cached
   */
  Optional<Entry> findEntry(String waitlistId, String entryId);

  /**
   * Drops the rank key and entry cache keys for a waitlist so the next read
   * rebuilds from PostgreSQL.
   *
   * @param waitlistId waitlist id
   */
  void invalidateWaitlist(String waitlistId);

  /**
   * Removes one entry payload from the cache.
   *
   * @param waitlistId waitlist id
   * @param entryId entry id
   */
  void deleteEntry(String waitlistId, String entryId);

  /**
   * Attempts to take the serve-next lock for a waitlist.
   *
   * @param waitlistId waitlist id
   * @return true when this caller holds the lock
   */
  boolean tryAcquireServeLock(String waitlistId);

  /**
   * Releases the serve-next lock for a waitlist.
   *
   * @param waitlistId waitlist id
   */
  void releaseServeLock(String waitlistId);
}
