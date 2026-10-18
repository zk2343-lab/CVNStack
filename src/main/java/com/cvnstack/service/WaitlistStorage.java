package com.cvnstack.service;

import com.cvnstack.model.Entry;
import com.cvnstack.model.Waitlist;
import java.util.List;
import java.util.Optional;

/**
 * Storage service for waitlists and entries. PostgreSQL impl arrives in issue 18.
 *
 * <p>AI-assisted.
 */
public interface WaitlistStorage {

  /**
   * Inserts or updates a waitlist.
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
   * Inserts or updates an entry.
   *
   * @param entry entry to store
   * @return stored entry
   */
  Entry saveEntry(Entry entry);

  /**
   * Finds an entry that belongs to the given waitlist.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry id
   * @return entry when present on that waitlist
   */
  Optional<Entry> findEntry(String waitlistId, String entryId);

  /**
   * Lists every entry on a waitlist.
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
   */
  void deleteEntry(String waitlistId, String entryId);

  /**
   * Counts entries currently {@code WAITING} on a waitlist.
   *
   * @param waitlistId waitlist id
   * @return waiting count
   */
  long countWaitingEntries(String waitlistId);
}
