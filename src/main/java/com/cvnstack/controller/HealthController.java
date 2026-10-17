package com.cvnstack.controller;

import com.cvnstack.model.response.HealthResponse;
import com.cvnstack.service.HealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for process liveness.
 *
 * <p>AI-assisted.
 */
@RestController
public class HealthController {

  private final HealthService healthService;

  /**
   * Creates the health controller.
   *
   * @param healthService liveness service
   */
  public HealthController(final HealthService healthService) {
    this.healthService = healthService;
  }

  /**
   * Reports that this process is serving requests.
   *
   * @return HTTP 200 and a status of UP
   */
  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    return ResponseEntity.ok(new HealthResponse(healthService.status()));
  }
}
