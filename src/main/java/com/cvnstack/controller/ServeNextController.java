package com.cvnstack.controller;

import com.cvnstack.model.response.ServeNextResponse;
import com.cvnstack.service.ServeNextService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles requests to call the next waiting entry.
 *
 * <p>AI-assisted.
 */
@RestController
public class ServeNextController {

  private final ServeNextService service;

  /**
   * Creates the controller with the serve-next service.
   *
   * @param service entry selection service
   */
  public ServeNextController(ServeNextService service) {
    this.service = service;
  }

  /**
   * Calls the next waiting entry, or reports an empty waitlist.
   *
   * @param clientInstance calling client instance
   * @param waitlistId waitlist to serve
   * @return 200 with the selected entry, or 204 without a body
   */
  @PostMapping("/waitlists/{id}/serve-next")
  public ResponseEntity<ServeNextResponse> serveNext(
      @RequestHeader(value = ClientInstanceHeader.NAME, required = false) String clientInstance,
      @PathVariable("id") String waitlistId) {
    ClientInstanceHeader.require(clientInstance);
    return service.serveNext(waitlistId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }
}
