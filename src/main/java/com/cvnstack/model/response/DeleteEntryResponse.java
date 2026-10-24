package com.cvnstack.model.response;

/**
 * Confirms removal of an entry from its waitlist.
 *
 * <p>AI-assisted.
 *
 * @param message removal confirmation
 * @param entryId removed entry id
 * @param waitlistId owning waitlist id
 */
public record DeleteEntryResponse(String message, String entryId, String waitlistId) {}
