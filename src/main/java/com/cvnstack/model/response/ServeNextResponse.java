package com.cvnstack.model.response;

import com.cvnstack.model.EntryStatus;

/**
 * Identifies the entry selected by serve-next.
 *
 * <p>AI-assisted.
 *
 * @param waitlistId owning waitlist id
 * @param entryId selected entry id
 * @param status selected entry status
 * @param priority selected entry priority
 */
public record ServeNextResponse(
    String waitlistId,
    String entryId,
    EntryStatus status,
    int priority) {}
