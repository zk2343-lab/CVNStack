package com.cvnstack.mockedclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Local restaurant server. The browser talks to this process, which calls the waitlist service.
 *
 * <p>AI-assisted.
 */
public final class RestaurantServer {

  public static final int DEFAULT_PORT = 8090;

  private WaitlistClient client;

  private String mode;

  private final int requestedPort;

  private final boolean logCalls;

  private final StandBookStore books;

  private final ObjectMapper mapper = new ObjectMapper();

  private HttpServer server;

  private ExecutorService executor;

  /**
   * Creates a server that proxies to the given waitlist client.
   *
   * @param client upstream client, including the instance id
   * @param mode label shown to the host stand ({@code local} or {@code gcp})
   * @param requestedPort bind port, or 0 for an ephemeral port
   * @param logCalls whether to print each proxied call
   * @param books where rooms and guest details are saved
   */
  public RestaurantServer(WaitlistClient client, String mode, int requestedPort,
      boolean logCalls, StandBookStore books) {
    this.client = client;
    this.mode = mode;
    this.requestedPort = requestedPort;
    this.logCalls = logCalls;
    this.books = books;
  }

  /**
   * Binds the host stand on 127.0.0.1.
   *
   * @throws IOException when the port cannot be bound
   */
  public void start() throws IOException {
    if (server != null) {
      throw new IllegalStateException("restaurant server is already running");
    }
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", requestedPort), 0);
    server.createContext("/", this::handle);
    executor = Executors.newFixedThreadPool(4, runnable -> {
      Thread thread = new Thread(runnable, "mocked-client");
      thread.setDaemon(true);
      return thread;
    });
    server.setExecutor(executor);
    server.start();
  }

  /**
   * Returns the bound port.
   *
   * @return local listen port
   */
  public int port() {
    return server.getAddress().getPort();
  }

  /**
   * Stops the host stand.
   */
  public void close() {
    if (server != null) {
      server.stop(0);
      server = null;
    }
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
  }

  private void handle(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    try {
      if ("GET".equals(exchange.getRequestMethod()) && isPage(path)) {
        sendBytes(exchange, 200, "text/html; charset=utf-8", resource("/static/index.html"));
        return;
      }
      if ("GET".equals(exchange.getRequestMethod()) && "/app.css".equals(path)) {
        sendBytes(exchange, 200, "text/css; charset=utf-8", resource("/static/app.css"));
        return;
      }
      if ("GET".equals(exchange.getRequestMethod()) && "/app.js".equals(path)) {
        sendBytes(
            exchange, 200, "text/javascript; charset=utf-8", resource("/static/app.js"));
        return;
      }
      if ("GET".equals(exchange.getRequestMethod()) && "/api/session".equals(path)) {
        sendBytes(exchange, 200, "application/json; charset=utf-8", sessionJson());
        return;
      }
      if ("POST".equals(exchange.getRequestMethod()) && "/api/upstream".equals(path)) {
        switchUpstream(exchange);
        return;
      }
      if (path.startsWith("/api/book/")) {
        book(exchange, path.substring("/api/book/".length()));
        return;
      }
      if (path.startsWith("/api/proxy/")) {
        proxy(exchange, path.substring("/api/proxy".length()));
        return;
      }
      sendBytes(exchange, 404, "text/plain; charset=utf-8", bytes("not found"));
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      sendBytes(exchange, 502, "application/json; charset=utf-8", upstreamError(ex));
    }
  }

  private boolean isPage(String path) {
    return "/".equals(path) || "/index.html".equals(path);
  }

  private void proxy(HttpExchange exchange, String upstreamPath)
      throws IOException, InterruptedException {
    if (!upstreamPath.startsWith("/") || upstreamPath.contains("..")) {
      sendBytes(exchange, 400, "text/plain; charset=utf-8", bytes("bad path"));
      return;
    }
    byte[] raw = exchange.getRequestBody().readAllBytes();
    String json = raw.length == 0 ? null : new String(raw, StandardCharsets.UTF_8);
    try {
      ApiReply reply = currentClient().exchange(
          exchange.getRequestMethod(), upstreamPath, json);
      if (logCalls) {
        System.out.println(exchange.getRequestMethod() + " " + upstreamPath
            + " -> " + reply.status());
      }
      if (reply.status() == 204) {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return;
      }
      sendBytes(exchange, reply.status(), "application/json; charset=utf-8", bytes(reply.body()));
    } catch (IOException ex) {
      sendBytes(exchange, 502, "application/json; charset=utf-8", upstreamError(ex));
    }
  }

  private void book(HttpExchange exchange, String bookMode) throws IOException {
    if (!UpstreamSelector.LOCAL_MODE.equals(bookMode)
        && !UpstreamSelector.CLOUD_MODE.equals(bookMode)) {
      sendBytes(exchange, 404, "text/plain; charset=utf-8", bytes("not found"));
      return;
    }
    String method = exchange.getRequestMethod();
    if ("GET".equals(method)) {
      sendBytes(exchange, 200, "application/json; charset=utf-8", books.read(bookMode));
      return;
    }
    if (!"PUT".equals(method)) {
      sendBytes(exchange, 405, "text/plain; charset=utf-8", bytes("method not allowed"));
      return;
    }
    try {
      books.write(bookMode, exchange.getRequestBody().readAllBytes());
    } catch (IllegalArgumentException ex) {
      sendBytes(exchange, 400, "application/json; charset=utf-8",
          bytes("{\"error\":\"INVALID_REQUEST\",\"message\":\"book must be a JSON object\"}"));
      return;
    }
    exchange.sendResponseHeaders(204, -1);
    exchange.close();
  }

  private void switchUpstream(HttpExchange exchange) throws IOException {
    String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    String requested;
    try {
      requested = mapper.readTree(raw).path("mode").asText("");
    } catch (IOException ex) {
      sendBytes(exchange, 400, "application/json; charset=utf-8",
          bytes("{\"error\":\"INVALID_REQUEST\"}"));
      return;
    }
    if (!UpstreamSelector.LOCAL_MODE.equals(requested)
        && !UpstreamSelector.CLOUD_MODE.equals(requested)) {
      sendBytes(exchange, 400, "application/json; charset=utf-8",
          bytes("{\"error\":\"INVALID_REQUEST\",\"message\":\"mode must be local or gcp\"}"));
      return;
    }
    useMode(requested);
    if (logCalls) {
      System.out.println("upstream " + requested + " " + currentClient().baseUri());
    }
    sendBytes(exchange, 200, "application/json; charset=utf-8", sessionJson());
  }

  private synchronized void useMode(String selected) {
    URI base = new UpstreamSelector().select(selected, null);
    mode = selected;
    client = new WaitlistClient(base, client.instanceId());
  }

  private synchronized WaitlistClient currentClient() {
    return client;
  }

  private synchronized byte[] sessionJson() throws IOException {
    ObjectNode node = mapper.createObjectNode();
    node.put("mode", mode);
    node.put("baseUrl", client.baseUri().toString());
    node.put("instanceId", client.instanceId());
    node.put("listenPort", port());
    return mapper.writeValueAsBytes(node);
  }

  private byte[] resource(String name) throws IOException {
    try (InputStream input = RestaurantServer.class.getResourceAsStream(name)) {
      if (input == null) {
        throw new IOException("missing resource " + name);
      }
      return input.readAllBytes();
    }
  }

  private byte[] upstreamError(Exception ex) {
    ObjectNode node = mapper.createObjectNode();
    node.put("error", "UPSTREAM_UNAVAILABLE");
    node.put("message", ex.getMessage() == null ? "upstream call failed" : ex.getMessage());
    try {
      return mapper.writeValueAsBytes(node);
    } catch (IOException writeFailure) {
      return bytes("{\"error\":\"UPSTREAM_UNAVAILABLE\"}");
    }
  }

  private byte[] bytes(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  private void sendBytes(HttpExchange exchange, int status, String type, byte[] body)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", type);
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    byte[] payload = body == null ? new byte[0] : body;
    exchange.sendResponseHeaders(status, payload.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(payload);
    }
  }
}
