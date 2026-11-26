package com.cvnstack.mockedclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks the local host stand proxy and its session page.
 *
 * <p>AI-assisted.
 */
class RestaurantServerTest {

  private FakeUpstream upstream;

  private RestaurantServer server;

  private HttpClient httpClient;

  @TempDir
  Path bookDirectory;

  @BeforeEach
  void start() throws Exception {
    upstream = FakeUpstream.start();
    WaitlistClient client = new WaitlistClient(URI.create(upstream.baseUrl()), "stand-1");
    server = new RestaurantServer(client, UpstreamSelector.LOCAL_MODE, 0, false,
        new StandBookStore(bookDirectory));
    server.start();
    httpClient = HttpClient.newHttpClient();
  }

  @AfterEach
  void stop() {
    server.close();
    upstream.close();
  }

  @Test
  void pageAndSessionDescribeThisStand() throws Exception {
    HttpResponse<String> page = send("GET", "/", null);
    HttpResponse<String> session = send("GET", "/api/session", null);
    assertEquals(200, page.statusCode());
    assertTrue(page.body().contains("Host stand"));
    assertTrue(session.body().contains("stand-1"));
    assertTrue(session.body().contains(upstream.baseUrl()));
    assertTrue(session.body().contains("\"mode\":\"local\""));
  }

  @Test
  void proxyForwardsTheInstanceHeader() throws Exception {
    HttpResponse<String> created = send(
        "POST", "/api/proxy/waitlists", "{\"name\":\"Dining room\"}");
    assertEquals(200, created.statusCode());
    assertEquals(java.util.List.of("stand-1"), upstream.instanceIds());
    assertTrue(created.body().contains("wl-1"));
  }

  @Test
  void toggleSwitchesLocalAndCloud() throws Exception {
    HttpResponse<String> cloud = send(
        "POST", "/api/upstream", "{\"mode\":\"gcp\"}");
    assertEquals(200, cloud.statusCode());
    assertTrue(cloud.body().contains("\"mode\":\"gcp\""));
    assertTrue(cloud.body().contains(UpstreamSelector.CLOUD_BASE));
    HttpResponse<String> local = send(
        "POST", "/api/upstream", "{\"mode\":\"local\"}");
    assertEquals(200, local.statusCode());
    assertTrue(local.body().contains("\"mode\":\"local\""));
    assertTrue(local.body().contains(UpstreamSelector.LOCAL_BASE));
    HttpResponse<String> rejected = send(
        "POST", "/api/upstream", "{\"mode\":\"lab\"}");
    assertEquals(400, rejected.statusCode());
  }

  @Test
  void bookIsSavedPerServiceAndSurvivesRestart() throws Exception {
    assertEquals("{}", send("GET", "/api/book/gcp", null).body());
    String saved = "{\"activeId\":\"wl-9\",\"rooms\":[{\"id\":\"wl-9\",\"name\":\"Bar\"}]}";
    assertEquals(204, send("PUT", "/api/book/gcp", saved).statusCode());
    assertEquals("{}", send("GET", "/api/book/local", null).body());
    assertEquals(400, send("PUT", "/api/book/gcp", "[1]").statusCode());
    assertEquals(404, send("GET", "/api/book/lab", null).statusCode());

    server.close();
    WaitlistClient client = new WaitlistClient(URI.create(upstream.baseUrl()), "stand-1");
    server = new RestaurantServer(client, UpstreamSelector.CLOUD_MODE, 0, false,
        new StandBookStore(bookDirectory));
    server.start();
    assertEquals(saved, send("GET", "/api/book/gcp", null).body());
  }

  @Test
  void unreachableUpstreamReturns502() throws Exception {
    upstream.close();
    HttpResponse<String> health = send("GET", "/api/proxy/health", null);
    assertEquals(502, health.statusCode());
    assertTrue(health.body().contains("UPSTREAM_UNAVAILABLE"));
  }

  private HttpResponse<String> send(String method, String path, String body) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(origin() + path));
    if (body == null) {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      builder.header("Content-Type", "application/json");
      builder.method(method, HttpRequest.BodyPublishers.ofString(body));
    }
    return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private String origin() {
    return "http://127.0.0.1:" + server.port();
  }
}
