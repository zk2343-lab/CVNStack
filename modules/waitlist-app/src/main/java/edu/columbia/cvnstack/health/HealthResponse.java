package edu.columbia.cvnstack.health;

/**
 * JSON body of {@code GET /health}.
 *
 * <p>AI-assisted: Cursor (Grok 4.7), 2026-10-16.
 *
 * @param status UP when this process is serving requests
 */
public record HealthResponse(String status) {}
