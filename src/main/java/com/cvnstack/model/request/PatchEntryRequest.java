package com.cvnstack.model.request;

import com.cvnstack.model.EntryStatus;

/**
 * Optional entry fields supplied by a PATCH request without defaults.
 *
 * <p>AI-assisted.
 *
 * @param status requested status, or null to preserve it
 * @param priority requested priority, or null to preserve it
 */
public record PatchEntryRequest(EntryStatus status, Integer priority) {}
