package com.cvnstack.model.response;

import java.util.List;

/**
 * Read-only dump of PostgreSQL rows and Redis keys for the request-flow visualizer.
 *
 * <p>AI-assisted.
 *
 * @param waitlists persisted waitlist rows
 * @param entries persisted entry rows
 * @param redis Redis keys used by the waitlist cache
 */
public record StoreSnapshotResponse(
    List<WaitlistSnapshot> waitlists,
    List<EntrySnapshot> entries,
    List<RedisSnapshot> redis) {

  /**
   * One PostgreSQL waitlist row.
   *
   * @param id waitlist id
   * @param name unique name
   * @param maxLength capacity
   * @param processingTimePerEntryMinutes minutes per entry
   */
  public record WaitlistSnapshot(
      String id, String name, int maxLength, int processingTimePerEntryMinutes) {}

  /**
   * One PostgreSQL entry row.
   *
   * @param id entry id
   * @param waitlistId owning waitlist
   * @param name display name
   * @param priority urgency
   * @param status lifecycle status
   * @param insertedAt insertion time
   * @param calledAt call time, or null
   * @param updatedAt last write time
   */
  public record EntrySnapshot(
      String id,
      String waitlistId,
      String name,
      int priority,
      String status,
      String insertedAt,
      String calledAt,
      String updatedAt) {}

  /**
   * One Redis key and its printable value.
   *
   * @param key Redis key
   * @param type Redis type name
   * @param value printable contents
   */
  public record RedisSnapshot(String key, String type, String value) {}
}
