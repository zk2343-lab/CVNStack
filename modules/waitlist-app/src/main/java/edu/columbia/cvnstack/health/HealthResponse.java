package edu.columbia.cvnstack.health;

/**
 * JSON body of {@code GET /health}.
 *
 * <p>AI-assisted.
 *
 * @param status UP when this process is serving requests
 */
public record HealthResponse(String status) {}
