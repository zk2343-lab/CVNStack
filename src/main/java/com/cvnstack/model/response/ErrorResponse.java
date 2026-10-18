package com.cvnstack.model.response;

import java.util.Objects;

/**
 * JSON error body {@code { "error": "...", "message": "..." }}.
 *
 * <p>AI-assisted.
 *
 * @param error machine-readable code from {@code ErrorCode}
 * @param message human-readable explanation
 */
public record ErrorResponse(String error, String message) {

  /**
   * Creates an error response.
   *
   * @param error machine-readable code
   * @param message human-readable explanation
   */
  public ErrorResponse {
    Objects.requireNonNull(error, "error");
    Objects.requireNonNull(message, "message");
  }
}
