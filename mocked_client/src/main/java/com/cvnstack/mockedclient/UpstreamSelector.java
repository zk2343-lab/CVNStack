package com.cvnstack.mockedclient;

import java.net.URI;

/**
 * Chooses the waitlist origin for a local port or the hosted service.
 *
 * <p>AI-assisted.
 */
public final class UpstreamSelector {

  public static final String LOCAL_MODE = "local";

  public static final String CLOUD_MODE = "gcp";

  public static final String LOCAL_BASE = "http://localhost:8080";

  public static final String CLOUD_BASE =
      "http://waitlist-management-service-5khm3u4xr0cvdjxqkvcf52uv.34-70-77-140.sslip.io";

  /**
   * Resolves an upstream origin.
   *
   * @param mode {@code local} or {@code gcp} when {@code baseUrl} is blank
   * @param baseUrl explicit origin, or null to use the mode default
   * @return origin with no trailing slash
   */
  public URI select(String mode, String baseUrl) {
    if (baseUrl != null && !baseUrl.isBlank()) {
      return origin(baseUrl);
    }
    if (CLOUD_MODE.equals(mode)) {
      return URI.create(CLOUD_BASE);
    }
    if (LOCAL_MODE.equals(mode)) {
      return URI.create(LOCAL_BASE);
    }
    throw new IllegalArgumentException("mode must be local or gcp");
  }

  private URI origin(String baseUrl) {
    String trimmed = baseUrl.trim();
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return URI.create(trimmed);
  }
}
