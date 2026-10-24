package com.cvnstack.controller;

import com.cvnstack.model.request.CreateEntryRequest;
import com.cvnstack.model.response.EntryResponse;
import com.cvnstack.service.CreateEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for adding an entry to a waitlist.
 *
 * <p>AI-assisted.
 */
@RestController
public class CreateEntryController {

  private final CreateEntryService service;

  /**
   * Creates the entry creation controller.
   *
   * @param service entry creation service
   */
  public CreateEntryController(CreateEntryService service) {
    this.service = service;
  }

  /**
   * Adds a waiting entry and returns its waiting information.
   *
   * @param waitlistId owning waitlist id
   * @param request entry name and optional priority
   * @return HTTP 200 with the created entry
   */
  @PostMapping("/waitlists/{waitlistId}/entries")
  public ResponseEntity<EntryResponse> createEntry(
      @PathVariable("waitlistId") String waitlistId, @RequestBody CreateEntryRequest request) {
    return ResponseEntity.ok(service.createEntry(waitlistId, request));
  }
}
