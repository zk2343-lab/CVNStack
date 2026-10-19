package com.cvnstack.model.response;

import com.cvnstack.model.EntryStatus;

/**
 * JSON body of an entry lookup with its current position and wait estimate.
 *
 * <p>AI-assisted.
 *
 * @param waitlistId owning waitlist id
 * @param entryId entry id
 * @param status entry status
 * @param priority urgency tier
 * @param position one-based position, or null when not waiting
 * @param estimatedWaitTimeMinutes wait estimate, or null when not waiting
 */
public record EntryResponse(
    String waitlistId,
    String entryId,
    EntryStatus status,
    int priority,
    Integer position,
    Integer estimatedWaitTimeMinutes) {}
