package com.cvnstack.service;

import com.cvnstack.model.Entry;
import com.cvnstack.model.Waitlist;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Storage port for waitlists and entries backed by PostgreSQL.
 *
 * <p>AI-assisted.
 */
public interface WaitlistStorage {

  /**
   * Inserts or updates a waitlist by id; a name owned by another id causes a conflict.
   *
   * @param waitlist waitlist to store
   * @return stored waitlist
   */
  Waitlist saveWaitlist(Waitlist waitlist);

  /**
   * Finds a waitlist by id.
   *
   * @param id waitlist id
   * @return waitlist when present
   */
  Optional<Waitlist> findWaitlistById(String id);

  /**
   * Finds a waitlist by unique name.
   *
   * @param name waitlist name
   * @return waitlist when present
   */
  Optional<Waitlist> findWaitlistByName(String name);

  /**
   * Inserts a new entry, rejecting an existing id; never overwrites an entry.
   *
   * <p>This operation does not enforce capacity or perform lifecycle transitions.
   *
   * @param entry entry to store
   * @return stored entry
   */
  Entry saveEntry(Entry entry);

  /**
   * Locks the owning waitlist and inserts only when WAITING plus CALLED is below capacity.
   *
   * <p>The lock, capacity read, active count, and insert share one transaction.
   * A missing waitlist raises NOT_FOUND; a full waitlist raises CAPACITY_EXCEEDED.
   *
   * @param entry new entry to insert
   * @return stored entry with its database write timestamp
   */
  Entry insertEntryIfCapacityAvailable(Entry entry);

  /**
   * Changes an entry to CALLED and records its call time only while it is WAITING.
   *
   * <p>The database implementation must check the waitlist id, entry id, and current
   * status in the same update. Return only after that update commits. Do not implement
   * this as a separate read followed by an unconditional save.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry to claim
   * @param calledAt call timestamp
   * @return updated entry, or empty when the entry is missing or no longer WAITING
   */
  Optional<Entry> callEntryIfWaiting(String waitlistId, String entryId, Instant calledAt);

  /**
   * Finds an entry that belongs to the given waitlist.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry id
   * @return entry when present on that waitlist
   */
  Optional<Entry> findEntry(String waitlistId, String entryId);

  /**
   * Lists every entry on a waitlist without guaranteeing business ranking order.
   *
   * @param waitlistId waitlist id
   * @return entries, possibly empty
   */
  List<Entry> findEntriesByWaitlistId(String waitlistId);

  /**
   * Removes an entry from a waitlist.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry id
   * @return true when exactly one matching entry was deleted, false otherwise
   */
  boolean deleteEntry(String waitlistId, String entryId);

  /**
   * Counts entries currently {@code WAITING} on a waitlist.
   *
   * @param waitlistId waitlist id
   * @return waiting count
   */
  long countWaitingEntries(String waitlistId);
}
