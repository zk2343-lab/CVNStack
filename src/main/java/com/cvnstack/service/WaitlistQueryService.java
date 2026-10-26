package com.cvnstack.service;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.response.EntryResponse;
import com.cvnstack.model.response.WaitlistDetailResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Reads one waitlist and returns every entry in waiting order.
 *
 * <p>AI-assisted.
 */
@Service
public class WaitlistQueryService {

  private final WaitlistStorage storage;
  private final EntryRulesService rules;

  /**
   * Creates the waitlist lookup service.
   *
   * @param storage source of waitlists and entries
   * @param rules waiting-order and estimate rules
   */
  public WaitlistQueryService(@Lazy WaitlistStorage storage, EntryRulesService rules) {
    this.storage = storage;
    this.rules = rules;
  }

  /**
   * Returns the waitlist and its entries, waiting guests first.
   *
   * @param waitlistId waitlist to read
   * @return waitlist settings plus ordered entries
   */
  public WaitlistDetailResponse getWaitlist(String waitlistId) {
    Waitlist waitlist = storage.findWaitlistById(waitlistId)
        .orElseThrow(() -> ApiException.notFound("Waitlist does not exist"));
    List<Entry> entries = storage.findEntriesByWaitlistId(waitlistId);
    List<Entry> waiting = rules.rankedWaiting(entries);
    Set<String> waitingIds = new LinkedHashSet<>();
    waiting.forEach(entry -> waitingIds.add(entry.id()));
    List<Entry> remainder = entries.stream()
        .filter(entry -> !waitingIds.contains(entry.id()))
        .sorted(Comparator.comparing(Entry::insertedAt).thenComparing(Entry::id))
        .toList();
    List<Entry> ordered = new ArrayList<>(waiting);
    ordered.addAll(remainder);

    List<EntryResponse> responses = new ArrayList<>(ordered.size());
    for (int index = 0; index < ordered.size(); index++) {
      Entry entry = ordered.get(index);
      Integer position = null;
      Integer estimatedWaitTimeMinutes = null;
      if (entry.status() == EntryStatus.WAITING) {
        position = index + 1;
        estimatedWaitTimeMinutes = rules.estimateMinutes(
            entry, entries, waitlist.processingTimePerEntryMinutes()).orElseThrow();
      }
      responses.add(new EntryResponse(
          waitlistId, entry.id(), entry.status(), entry.priority(),
          position, estimatedWaitTimeMinutes));
    }
    return new WaitlistDetailResponse(
        waitlist.id(), waitlist.name(), waitlist.maxLength(),
        waitlist.processingTimePerEntryMinutes(), List.copyOf(responses));
  }
}
