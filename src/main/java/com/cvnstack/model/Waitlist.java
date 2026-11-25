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
  private final String clientInstance;

  /**
   * Creates a waitlist value with a placeholder instance id for tests.
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
    this(id, name, maxLength, processingTimePerEntryMinutes, "test-instance");
  }

  /**
   * Creates a waitlist value including its creating client instance.
   *
   * @param id assigned waitlist identifier
   * @param name unique display name
   * @param maxLength maximum number of waiting entries allowed
   * @param processingTimePerEntryMinutes minutes used for wait estimates
   * @param clientInstance creating client instance id
   */
  public Waitlist(
      final String id,
      final String name,
      final int maxLength,
      final int processingTimePerEntryMinutes,
      final String clientInstance) {
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
    this.clientInstance = Objects.requireNonNull(clientInstance, "clientInstance");
    if (clientInstance.isBlank()) {
      throw new IllegalArgumentException("clientInstance must not be blank");
    }
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

  /**
   * Returns the client instance that created this waitlist.
   *
   * @return client instance id
   */
  public String clientInstance() {
    return clientInstance;
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
        && name.equals(waitlist.name)
        && clientInstance.equals(waitlist.clientInstance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, maxLength, processingTimePerEntryMinutes, clientInstance);
  }
}
