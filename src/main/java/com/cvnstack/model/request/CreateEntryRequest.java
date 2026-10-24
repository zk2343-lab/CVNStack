package com.cvnstack.model.request;

/**
 * JSON body for adding an entry to the waitlist identified by the path.
 *
 * <p>AI-assisted.
 *
 * @param name entry display name
 * @param priority urgency tier, or null to use the default
 */
public record CreateEntryRequest(String name, Integer priority) {}
