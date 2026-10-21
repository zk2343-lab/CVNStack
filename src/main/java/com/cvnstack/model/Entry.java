package com.cvnstack.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Single person or item on a waitlist.
 *
 * <p>AI-assisted.
 */
public final class Entry {

  /** Lowest urgency; used when the caller omits priority. */
  public static final int DEFAULT_PRIORITY = 1;

  /** Inclusive lower bound for priority. */
  public static final int MIN_PRIORITY = 1;

  /** Inclusive upper bound; most urgent. */
  public static final int MAX_PRIORITY = 5;

  private final String id;
  private final String waitlistId;
  private final String name;
  private final int priority;
  private final EntryStatus status;
  private final Instant insertedAt;
  private final Instant calledAt;

  /**
   * Creates an entry value.
   *
   * @param id assigned entry identifier
   * @param waitlistId owning waitlist identifier
   * @param name entry display name
   * @param priority urgency from {@link #MIN_PRIORITY} to {@link #MAX_PRIORITY}
   * @param status current lifecycle status
   * @param insertedAt insertion time used for tie-breaking
   */
  public Entry(
      final String id,
      final String waitlistId,
      final String name,
      final int priority,
      final EntryStatus status,
      final Instant insertedAt) {
    this(id, waitlistId, name, priority, status, insertedAt, null);
  }

  /**
   * Creates an entry value including its stored call timestamp.
   *
   * @param id assigned entry identifier
   * @param waitlistId owning waitlist identifier
   * @param name entry display name
   * @param priority urgency from MIN_PRIORITY to MAX_PRIORITY
   * @param status current lifecycle status
   * @param insertedAt insertion time used for tie-breaking
   * @param calledAt call time, or null when no call time is recorded
   */
  public Entry(
      final String id,
      final String waitlistId,
      final String name,
      final int priority,
      final EntryStatus status,
      final Instant insertedAt,
      final Instant calledAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.waitlistId = Objects.requireNonNull(waitlistId, "waitlistId");
    this.name = Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
      throw new IllegalArgumentException(
          "priority must be between " + MIN_PRIORITY + " and " + MAX_PRIORITY);
    }
    this.priority = priority;
    this.status = Objects.requireNonNull(status, "status");
    this.insertedAt = Objects.requireNonNull(insertedAt, "insertedAt");
    this.calledAt = calledAt;
  }

  /**
   * Returns the entry identifier.
   *
   * @return id
   */
  public String id() {
    return id;
  }

  /**
   * Returns the owning waitlist identifier.
   *
   * @return waitlist id
   */
  public String waitlistId() {
    return waitlistId;
  }

  /**
   * Returns the entry display name.
   *
   * @return name
   */
  public String name() {
    return name;
  }

  /**
   * Returns the urgency tier; higher values are served sooner.
   *
   * @return priority
   */
  public int priority() {
    return priority;
  }

  /**
   * Returns the current lifecycle status.
   *
   * @return status
   */
  public EntryStatus status() {
    return status;
  }

  /**
   * Returns the insertion timestamp used when priorities tie.
   *
   * @return inserted at
   */
  public Instant insertedAt() {
    return insertedAt;
  }

  /**
   * Returns the stored call timestamp.
   *
   * @return call time, or null when no call time is recorded
   */
  public Instant calledAt() {
    return calledAt;
  }

  /**
   * Returns a copy with a different status.
   *
   * @param newStatus replacement status
   * @return new entry value
   */
  public Entry withStatus(final EntryStatus newStatus) {
    return new Entry(id, waitlistId, name, priority, newStatus, insertedAt, calledAt);
  }

  /**
   * Returns a copy with a different priority.
   *
   * @param newPriority replacement priority
   * @return new entry value
   */
  public Entry withPriority(final int newPriority) {
    return new Entry(id, waitlistId, name, newPriority, status, insertedAt, calledAt);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Entry entry)) {
      return false;
    }
    return priority == entry.priority
        && id.equals(entry.id)
        && waitlistId.equals(entry.waitlistId)
        && name.equals(entry.name)
        && status == entry.status
        && insertedAt.equals(entry.insertedAt)
        && Objects.equals(calledAt, entry.calledAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, waitlistId, name, priority, status, insertedAt, calledAt);
  }
}
