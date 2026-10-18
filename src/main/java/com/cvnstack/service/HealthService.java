package com.cvnstack.service;

import org.springframework.stereotype.Service;

/**
 * Liveness status for the waitlist process.
 *
 * <p>AI-assisted.
 */
@Service
public class HealthService {

  /**
   * Returns the current liveness status value.
   *
   * @return {@code UP} when this process is serving requests
   */
  public String status() {
    return "UP";
  }
}
