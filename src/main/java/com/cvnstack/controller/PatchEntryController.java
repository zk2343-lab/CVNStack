package com.cvnstack.controller;

import com.cvnstack.model.request.PatchEntryRequest;
import com.cvnstack.model.response.PatchEntryResponse;
import com.cvnstack.service.PatchEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
   * @param clientInstance calling client instance
   * @param waitlistId owning waitlist id
   * @param entryId entry id
   * @param request optional status and priority
   * @return updated entry and waiting information
   */
  @PatchMapping("/waitlists/{waitlistId}/entries/{entryId}")
  public ResponseEntity<PatchEntryResponse> patchEntry(
      @RequestHeader(value = ClientInstanceHeader.NAME, required = false) String clientInstance,
      @PathVariable("waitlistId") String waitlistId,
      @PathVariable("entryId") String entryId,
      @RequestBody PatchEntryRequest request) {
    ClientInstanceHeader.require(clientInstance);
    return ResponseEntity.ok(service.patchEntry(waitlistId, entryId, request));
  }
}
