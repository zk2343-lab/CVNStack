package com.cvnstack.model.request;

/**
 * Carries client settings for a new waitlist without a caller-assigned identifier.
 *
 * <p>AI-assisted.
 *
 * @param name unique waitlist name
 * @param maxLength maximum waiting capacity
 * @param processingTimePerEntryMinutes positive minutes per entry
 */
public record CreateWaitlistRequest(
    String name, int maxLength, int processingTimePerEntryMinutes) {}
