package com.cvnstack.controller;

import com.cvnstack.model.response.WaitlistDetailResponse;
import com.cvnstack.service.WaitlistQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for reading a waitlist and its entries in rank order.
 *
 * <p>AI-assisted.
 */
@RestController
public class WaitlistQueryController {

  private final WaitlistQueryService service;

  /**
   * Creates the waitlist query controller.
   *
   * @param service waitlist lookup service
   */
  public WaitlistQueryController(WaitlistQueryService service) {
    this.service = service;
  }

  /**
   * Returns the stored waitlist with every entry in waiting order.
   *
   * @param waitlistId waitlist id from the path
   * @return HTTP 200 with the waitlist, or HTTP 404 when it is missing
   */
  @GetMapping("/waitlists/{id}")
  public ResponseEntity<WaitlistDetailResponse> getWaitlist(
      @PathVariable("id") String waitlistId) {
    return ResponseEntity.ok(service.getWaitlist(waitlistId));
  }
}
