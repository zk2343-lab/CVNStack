package edu.columbia.cvnstack.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness probe used before any waitlist route exists.
 *
 * <p>AI-assisted: Cursor (Grok 4.7), 2026-10-16.
 */
@RestController
public class HealthController {

  /**
   * Reports that this process is serving requests.
   *
   * @return HTTP 200 and a status of UP
   */
  @GetMapping("/health")
  public HealthResponse health() {
    return new HealthResponse("UP");
  }
}
