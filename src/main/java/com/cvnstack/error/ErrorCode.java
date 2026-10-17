package com.cvnstack.error;

/**
 * Stable {@code error} field values returned in API error bodies.
 *
 * <p>AI-assisted.
 */
public final class ErrorCode {

  /** Bad field, empty update, or other invalid request content. */
  public static final String INVALID_REQUEST = "INVALID_REQUEST";

  /** Status change that the domain state machine rejects. */
  public static final String INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION";

  /** Unknown waitlist or entry. */
  public static final String NOT_FOUND = "NOT_FOUND";

  /** Waitlist name already exists. */
  public static final String CONFLICT = "CONFLICT";

  /** Add would exceed the waitlist {@code maxLength}. */
  public static final String CAPACITY_EXCEEDED = "CAPACITY_EXCEEDED";

  private ErrorCode() {}
}
