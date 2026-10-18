package com.cvnstack.error;

import com.cvnstack.model.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps {@link ApiException} to the shared error JSON body.
 *
 * <p>AI-assisted.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Writes {@link ErrorResponse} for a known API failure.
   *
   * @param exception failure to map
   * @return status and body
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(final ApiException exception) {
    return ResponseEntity.status(exception.status())
        .body(new ErrorResponse(exception.errorCode(), exception.getMessage()));
  }
}
