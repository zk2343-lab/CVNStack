package com.cvnstack.model.response;

import java.util.List;

/**
 * JSON body of a waitlist lookup with every entry in rank order.
 *
 * <p>AI-assisted.
 *
 * @param id waitlist identifier
 * @param name unique waitlist name
 * @param maxLength maximum waiting capacity
 * @param processingTimePerEntryMinutes positive minutes per entry
 * @param entries waiting entries first, then the rest
 */
public record WaitlistDetailResponse(
    String id,
    String name,
    int maxLength,
    int processingTimePerEntryMinutes,
    List<EntryResponse> entries) {}
