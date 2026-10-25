package com.cvnstack.controller;

import com.cvnstack.model.request.PatchEntryRequest;
import com.cvnstack.model.response.PatchEntryResponse;
import com.cvnstack.service.PatchEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for partially updating an entry.
 *
 * <p>AI-assisted.
 */
@RestController
public class PatchEntryController {

  private final PatchEntryService service;

  /**
   * Creates the PATCH controller.
   *
   * @param service entry update service
   */
  public PatchEntryController(PatchEntryService service) {
    this.service = service;
  }

  /**
   * Updates supplied entry fields.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry id
   * @param request optional status and priority
   * @return updated entry and waiting information
   */
  @PatchMapping("/waitlists/{waitlistId}/entries/{entryId}")
  public ResponseEntity<PatchEntryResponse> patchEntry(
      @PathVariable("waitlistId") String waitlistId,
      @PathVariable("entryId") String entryId,
      @RequestBody PatchEntryRequest request) {
    return ResponseEntity.ok(service.patchEntry(waitlistId, entryId, request));
  }
}
