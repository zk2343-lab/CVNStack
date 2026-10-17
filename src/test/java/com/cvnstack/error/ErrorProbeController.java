package com.cvnstack.error;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only routes that throw each mapped {@link ApiException}.
 *
 * <p>AI-assisted.
 */
@RestController
@RequestMapping("/__test__/errors")
class ErrorProbeController {

  @GetMapping("/invalid-request")
  void invalidRequest() {
    throw ApiException.invalidRequest("bad field");
  }

  @GetMapping("/invalid-state")
  void invalidState() {
    throw ApiException.invalidStateTransition("cannot move from SERVED to WAITING");
  }

  @GetMapping("/not-found")
  void notFound() {
    throw ApiException.notFound("Waitlist or entry does not exist");
  }

  @GetMapping("/conflict")
  void conflict() {
    throw ApiException.conflict("Waitlist name already exists");
  }

  @GetMapping("/capacity")
  void capacity() {
    throw ApiException.capacityExceeded("Waitlist is full");
  }
}
