package com.cvnstack.mockedclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

/**
 * Checks local-port and hosted-service origins.
 *
 * <p>AI-assisted.
 */
class UpstreamSelectorTest {

  private final UpstreamSelector selector = new UpstreamSelector();

  @Test
  void localModeUsesTheServicePort() {
    URI uri = selector.select(UpstreamSelector.LOCAL_MODE, null);
    assertEquals(URI.create(UpstreamSelector.LOCAL_BASE), uri);
  }

  @Test
  void gcpModeUsesTheHostedService() {
    URI uri = selector.select(UpstreamSelector.CLOUD_MODE, "  ");
    assertEquals(URI.create(UpstreamSelector.CLOUD_BASE), uri);
  }

  @Test
  void explicitBaseUrlWins() {
    URI uri = selector.select(UpstreamSelector.CLOUD_MODE, "http://127.0.0.1:9090/");
    assertEquals(URI.create("http://127.0.0.1:9090"), uri);
  }

  @Test
  void unknownModeIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> selector.select("cloud", null));
  }
}
