package com.cvnstack.service;

import com.cvnstack.model.Entry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Cache storage service for waiting order, serve locks, and entry payloads.
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
   * Replaces the waiting order when {@code revision} is at least as new as the
   * cached waitlist revision.
   *
   * @param waitlistId waitlist id
   * @param entryIdsInOrder waiting entry ids, highest rank first
   * @param revision database time of the mutation that produced this order
   * @return {@code true} when the order was written
   */
  boolean replaceWaitingOrderIfNewer(
      String waitlistId, List<String> entryIdsInOrder, Instant revision);

  /**
   * Reads the cached waiting order when present.
   *
   * @param waitlistId waitlist id
   * @return ordered entry ids when cached
   */
  Optional<List<String>> findWaitingOrder(String waitlistId);

  /**
   * Stores a read-through entry payload when it is at least as new as the cached one.
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
   * Drops one waiting member from the rank key without touching other entries.
   *
   * @param waitlistId waitlist id
   * @param entryId waiting entry id to remove
   */
  void removeWaitingMember(String waitlistId, String entryId);

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
