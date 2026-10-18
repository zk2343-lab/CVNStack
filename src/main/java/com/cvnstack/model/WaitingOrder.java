package com.cvnstack.model;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Orders waiting entries by higher priority, then earlier insertion time.
 *
 * <p>AI-assisted.
 */
public final class WaitingOrder {

  private WaitingOrder() {}

  /**
   * Comparator for {@link EntryStatus#WAITING} ranking.
   *
   * @return comparator that puts higher priority first, then earlier insertion
   */
  public static Comparator<Entry> comparator() {
    return Comparator.comparingInt(Entry::priority)
        .reversed()
        .thenComparing(Entry::insertedAt);
  }

  /**
   * Returns waiting entries sorted for serve-next and position calculation.
   *
   * @param entries entries that may include non-waiting statuses
   * @return only {@code WAITING} entries in rank order
   */
  public static List<Entry> rankedWaiting(final List<Entry> entries) {
    Objects.requireNonNull(entries, "entries");
    return entries.stream()
        .filter(entry -> entry.status() == EntryStatus.WAITING)
        .sorted(comparator())
        .toList();
  }

  /**
   * One-based position among waiting entries, or empty when not waiting.
   *
   * @param target entry whose position is requested
   * @param entries all entries on the same waitlist
   * @return position starting at 1, or empty
   */
  public static OptionalInt positionOf(final Entry target, final List<Entry> entries) {
    Objects.requireNonNull(target, "target");
    if (target.status() != EntryStatus.WAITING) {
      return OptionalInt.empty();
    }
    final List<Entry> ranked = rankedWaiting(entries);
    for (int index = 0; index < ranked.size(); index++) {
      if (ranked.get(index).id().equals(target.id())) {
        return OptionalInt.of(index + 1);
      }
    }
    return OptionalInt.empty();
  }

  /**
   * Count of waiting entries ahead of {@code target}, or empty when not waiting.
   *
   * @param target entry whose ahead-count is requested
   * @param entries all entries on the same waitlist
   * @return entries ahead, or empty
   */
  public static OptionalInt entriesAhead(final Entry target, final List<Entry> entries) {
    final OptionalInt position = positionOf(target, entries);
    if (position.isEmpty()) {
      return OptionalInt.empty();
    }
    return OptionalInt.of(position.getAsInt() - 1);
  }
}
