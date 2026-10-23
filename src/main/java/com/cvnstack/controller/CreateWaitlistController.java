package com.cvnstack.controller;

import com.cvnstack.model.Waitlist;
import com.cvnstack.model.request.CreateWaitlistRequest;
import com.cvnstack.model.response.WaitlistResponse;
import com.cvnstack.service.CreateWaitlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles requests to create a waitlist.
 *
 * <p>AI-assisted.
 */
@RestController
public class CreateWaitlistController {

  private final CreateWaitlistService service;

  /**
   * Creates the controller with the creation service.
   *
   * @param service waitlist creation service
   */
  public CreateWaitlistController(CreateWaitlistService service) {
    this.service = service;
  }

  /**
   * Creates a waitlist from client settings.
   *
   * @param request settings to validate and store
   * @return 200 with the stored identifier and settings
   */
  @PostMapping("/waitlists")
  public ResponseEntity<WaitlistResponse> createWaitlist(
      @RequestBody CreateWaitlistRequest request) {
    Waitlist saved = service.createWaitlist(request);
    return ResponseEntity.ok(new WaitlistResponse(
        saved.id(), saved.name(), saved.maxLength(), saved.processingTimePerEntryMinutes()));
  }
}
