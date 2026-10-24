package com.cvnstack.model.response;

import com.cvnstack.model.EntryStatus;
import java.time.Instant;

/**
 * Persisted entry details and derived waiting information after a PATCH.
 *
 * <p>AI-assisted.
 *
 * @param waitlistId owning waitlist id
 * @param entryId entry id
 * @param status persisted status
 * @param priority persisted priority
 * @param updatedAt database write timestamp
 * @param position one-based waiting position, or null
 * @param estimatedWaitTimeMinutes waiting estimate, or null
 */
public record PatchEntryResponse(
    String waitlistId,
    String entryId,
    EntryStatus status,
    int priority,
    Instant updatedAt,
    Integer position,
    Integer estimatedWaitTimeMinutes) {}
