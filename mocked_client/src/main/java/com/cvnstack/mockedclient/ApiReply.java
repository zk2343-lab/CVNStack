package com.cvnstack.mockedclient;

/**
 * Status and body returned by one waitlist HTTP call.
 *
 * <p>AI-assisted.
 *
 * @param status HTTP status code
 * @param body response body, empty when the upstream sent none
 */
public record ApiReply(int status, String body) {}
