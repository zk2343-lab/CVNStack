package com.cvnstack.controller;

import com.cvnstack.model.response.EntryResponse;
import com.cvnstack.service.EntryQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for reading an entry's current position and wait estimate.
 *
 * <p>AI-assisted.
 */
@RestController
public class EntryQueryController {

  private final EntryQueryService service;

  /**
   * Creates the entry query controller.
   *
   * @param service entry lookup service
   */
  public EntryQueryController(EntryQueryService service) {
    this.service = service;
  }

  /**
   * Returns the stored entry with its waiting information.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry id
   * @return HTTP 200 with entry details, or HTTP 404 when not found
   */
  @GetMapping("/waitlists/{id}/entries/{entryId}")
  public ResponseEntity<EntryResponse> getEntry(
      @PathVariable("id") String waitlistId, @PathVariable("entryId") String entryId) {
    return ResponseEntity.ok(service.getEntry(waitlistId, entryId));
  }
}
