package com.cvnstack.model.response;

/**
 * Returns the stored identifier and settings of a waitlist.
 *
 * <p>AI-assisted.
 *
 * @param id server-assigned waitlist identifier
 * @param name unique waitlist name
 * @param maxLength maximum waiting capacity
 * @param processingTimePerEntryMinutes positive minutes per entry
 */
public record WaitlistResponse(
    String id, String name, int maxLength, int processingTimePerEntryMinutes) {}
