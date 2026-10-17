package com.cvnstack.error;

import org.springframework.http.HttpStatus;

/**
 * Application exception mapped to an HTTP status and API error code.
 *
 * <p>AI-assisted.
 */
public final class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final String errorCode;

  private ApiException(
      final HttpStatus status, final String errorCode, final String message) {
    super(message);
    this.status = status;
    this.errorCode = errorCode;
  }

  /**
   * Bad field, empty update, or other invalid request content.
   *
   * @param message explanation
   * @return exception mapped to HTTP 400
   */
  public static ApiException invalidRequest(final String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, message);
  }

  /**
   * Illegal status change.
   *
   * @param message explanation
   * @return exception mapped to HTTP 400
   */
  public static ApiException invalidStateTransition(final String message) {
    return new ApiException(
        HttpStatus.BAD_REQUEST, ErrorCode.INVALID_STATE_TRANSITION, message);
  }

  /**
   * Missing waitlist or entry.
   *
   * @param message explanation
   * @return exception mapped to HTTP 404
   */
  public static ApiException notFound(final String message) {
    return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message);
  }

  /**
   * Waitlist name already exists.
   *
   * @param message explanation
   * @return exception mapped to HTTP 409
   */
  public static ApiException conflict(final String message) {
    return new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message);
  }

  /**
   * Waitlist is full.
   *
   * @param message explanation
   * @return exception mapped to HTTP 422
   */
  public static ApiException capacityExceeded(final String message) {
    return new ApiException(
        HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.CAPACITY_EXCEEDED, message);
  }

  /**
   * Returns the HTTP status for this failure.
   *
   * @return status
   */
  public HttpStatus status() {
    return status;
  }

  /**
   * Returns the machine-readable error code.
   *
   * @return error code
   */
  public String errorCode() {
    return errorCode;
  }
}
