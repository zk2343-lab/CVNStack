package com.cvnstack.controller;

import com.cvnstack.model.response.DeleteEntryResponse;
import com.cvnstack.service.DeleteEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for removing an entry from a waitlist.
 *
 * <p>AI-assisted.
 */
@RestController
public class DeleteEntryController {

  private final DeleteEntryService service;

  /**
   * Creates the entry removal controller.
   *
   * @param service entry removal service
   */
  public DeleteEntryController(DeleteEntryService service) {
    this.service = service;
  }

  /**
   * Removes an entry identified by the path variables.
   *
   * @param waitlistId owning waitlist id
   * @param entryId entry to remove
   * @return HTTP 200 with removal confirmation
   */
  @DeleteMapping("/waitlists/{waitlistId}/entries/{entryId}")
  public ResponseEntity<DeleteEntryResponse> deleteEntry(
      @PathVariable("waitlistId") String waitlistId, @PathVariable("entryId") String entryId) {
    return ResponseEntity.ok(service.deleteEntry(waitlistId, entryId));
  }
}
