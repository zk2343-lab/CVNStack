package com.cvnstack.model;

import java.util.Objects;

/**
 * Named waitlist with a capacity and a fixed per-entry processing time.
 *
 * <p>AI-assisted.
 */
public final class Waitlist {

  private final String id;
  private final String name;
  private final int maxLength;
  private final int processingTimePerEntryMinutes;

  /**
   * Creates a waitlist value.
   *
   * @param id assigned waitlist identifier
   * @param name unique display name
   * @param maxLength maximum number of waiting entries allowed
   * @param processingTimePerEntryMinutes minutes used for wait estimates
   */
  public Waitlist(
      final String id,
      final String name,
      final int maxLength,
      final int processingTimePerEntryMinutes) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (maxLength < 1) {
      throw new IllegalArgumentException("maxLength must be at least 1");
    }
    if (processingTimePerEntryMinutes <= 0) {
      throw new IllegalArgumentException("processingTimePerEntryMinutes must be positive");
    }
    this.maxLength = maxLength;
    this.processingTimePerEntryMinutes = processingTimePerEntryMinutes;
  }

  /**
   * Returns the waitlist identifier.
   *
   * @return id
   */
  public String id() {
    return id;
  }

  /**
   * Returns the unique waitlist name.
   *
   * @return name
   */
  public String name() {
    return name;
  }

  /**
   * Returns the maximum waiting capacity.
   *
   * @return max length
   */
  public int maxLength() {
    return maxLength;
  }

  /**
   * Returns minutes assumed per waiting entry ahead.
   *
   * @return processing time per entry in minutes
   */
  public int processingTimePerEntryMinutes() {
    return processingTimePerEntryMinutes;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Waitlist waitlist)) {
      return false;
    }
    return maxLength == waitlist.maxLength
        && processingTimePerEntryMinutes == waitlist.processingTimePerEntryMinutes
        && id.equals(waitlist.id)
        && name.equals(waitlist.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, maxLength, processingTimePerEntryMinutes);
  }
}
