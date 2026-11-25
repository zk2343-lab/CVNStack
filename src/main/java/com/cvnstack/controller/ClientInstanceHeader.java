package com.cvnstack.controller;

import com.cvnstack.error.ApiException;

/**
 * Reads the required {@code X-Client-Instance} header on mutating routes.
 *
 * <p>AI-assisted.
 */
public final class ClientInstanceHeader {

  /** HTTP header that identifies one running copy of a client. */
  public static final String NAME = "X-Client-Instance";

  private ClientInstanceHeader() {}

  /**
   * Returns a non-blank instance id, or rejects the request.
   *
   * @param value header value, possibly missing
   * @return trimmed instance id
   */
  public static String require(String value) {
    if (value == null || value.isBlank()) {
      throw ApiException.invalidRequest("X-Client-Instance must not be blank");
    }
    return value.trim();
  }
}
