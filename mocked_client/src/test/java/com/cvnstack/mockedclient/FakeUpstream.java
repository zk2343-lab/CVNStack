package com.cvnstack.mockedclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-process waitlist stand-in for client tests.
 *
 * <p>AI-assisted.
 */
final class FakeUpstream {

  private final ObjectMapper mapper = new ObjectMapper();

  private final List<String> instanceIds = Collections.synchronizedList(new ArrayList<>());

  private final List<String> bodies = Collections.synchronizedList(new ArrayList<>());

  private HttpServer server;

  /**
   * Starts the stand-in on an ephemeral port.
   *
   * @return running stand-in
   * @throws IOException when the port cannot be bound
   */
  static FakeUpstream start() throws IOException {
    FakeUpstream upstream = new FakeUpstream();
    upstream.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    upstream.server.createContext("/", upstream::handle);
    upstream.server.start();
    return upstream;
  }

  /**
   * Returns the origin, with no trailing slash.
   *
   * @return base URL
   */
  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  /**
   * Returns instance ids in call order.
   *
   * @return recorded {@code X-Client-Instance} values
   */
  List<String> instanceIds() {
    return List.copyOf(instanceIds);
  }

  /**
   * Returns request bodies in call order.
   *
   * @return recorded bodies
   */
  List<String> bodies() {
    return List.copyOf(bodies);
  }

  /**
   * Stops the stand-in.
   */
  void close() {
    if (server != null) {
      server.stop(0);
      server = null;
    }
  }

  private void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    String instanceId = exchange.getRequestHeaders().getFirst(WaitlistClient.INSTANCE_HEADER);
    instanceIds.add(instanceId == null ? "" : instanceId);
    bodies.add(body);
    if ("/waitlists/empty/serve-next".equals(path)) {
      exchange.sendResponseHeaders(204, -1);
      exchange.close();
      return;
    }
    int status = 200;
    String response = response(exchange.getRequestMethod(), path, body, instanceId);
    if (response == null) {
      status = 404;
      response = "{\"error\":\"NOT_FOUND\"}";
    }
    byte[] payload = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, payload.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(payload);
    }
  }

  private String response(String method, String path, String body, String instanceId)
      throws IOException {
    if ("GET".equals(method) && "/health".equals(path)) {
      return "{\"status\":\"UP\"}";
    }
    if ("POST".equals(method) && "/waitlists".equals(path)) {
      ObjectNode node = mapper.createObjectNode();
      node.put("id", "wl-1");
      node.put("clientInstance", instanceId);
      return mapper.writeValueAsString(node);
    }
    if ("POST".equals(method) && "/waitlists/wl-1/entries".equals(path)) {
      JsonNode request = mapper.readTree(body);
      ObjectNode node = mapper.createObjectNode();
      node.put("entryId", "en-1");
      node.put("status", "WAITING");
      node.put("priority", request.path("priority").asInt(1));
      node.put("position", request.path("priority").asInt(1) == 5 ? 1 : 2);
      node.put("estimatedWaitTimeMinutes", 0);
      node.put("clientInstance", instanceId);
      return mapper.writeValueAsString(node);
    }
    if ("GET".equals(method) && "/waitlists/wl-1/entries/en-1".equals(path)) {
      return "{\"entryId\":\"en-1\",\"status\":\"WAITING\",\"position\":1,"
          + "\"estimatedWaitTimeMinutes\":0,\"clientInstance\":\"" + instanceId + "\"}";
    }
    if ("POST".equals(method) && "/waitlists/wl-1/serve-next".equals(path)) {
      return "{\"entryId\":\"en-1\",\"status\":\"CALLED\",\"clientInstance\":\""
          + instanceId + "\"}";
    }
    if ("PATCH".equals(method) && "/waitlists/wl-1/entries/en-1".equals(path)) {
      return "{\"entryId\":\"en-1\",\"status\":\"SERVED\",\"clientInstance\":\""
          + instanceId + "\"}";
    }
    return null;
  }
}
