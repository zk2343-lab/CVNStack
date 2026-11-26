package com.cvnstack.mockedclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Checks command-line parsing for local and hosted modes.
 *
 * <p>AI-assisted.
 */
class ClientOptionsTest {

  @Test
  void defaultsToLocalPort() {
    ClientOptions options = ClientOptions.parse(new String[] {"health"});
    assertEquals("health", options.command());
    assertEquals(UpstreamSelector.LOCAL_MODE, options.mode());
    assertEquals(RestaurantServer.DEFAULT_PORT, options.listenPort());
    assertNull(options.baseUrl());
  }

  @Test
  void readsGcpModeAndCheckInPriority() {
    ClientOptions options = ClientOptions.parse(new String[] {
        "--mode", "gcp",
        "check-in",
        "--waitlist", "wl-1",
        "--name", "Rush",
        "--priority", "5"
    });
    assertEquals("gcp", options.mode());
    assertEquals("Rush", options.name());
    assertEquals(5, options.priority());
  }

  @Test
  void missingCommandFails() {
    assertThrows(IllegalArgumentException.class, () -> ClientOptions.parse(new String[] {}));
  }
}
