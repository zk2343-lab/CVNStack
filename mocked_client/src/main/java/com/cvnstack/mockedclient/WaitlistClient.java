package com.cvnstack.mockedclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HTTP client for the waitlist service. It does not use server Java types.
 *
 * <p>AI-assisted.
 */
public final class WaitlistClient {

  public static final String INSTANCE_HEADER = "X-Client-Instance";

  private final URI baseUri;

  private final String instanceId;

  private final HttpClient httpClient;

  private final ObjectMapper mapper;

  /**
   * Creates a client for one upstream origin and one instance id.
   *
   * @param baseUri waitlist origin without a trailing slash
   * @param instanceId value sent as {@code X-Client-Instance}
   */
  public WaitlistClient(URI baseUri, String instanceId) {
    this(
        baseUri,
        instanceId,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
        new ObjectMapper());
  }

  /**
   * Creates a client with a caller-supplied HTTP stack.
   *
   * @param baseUri waitlist origin without a trailing slash
   * @param instanceId value sent as {@code X-Client-Instance}
   * @param httpClient JDK HTTP client
   * @param mapper JSON writer for request bodies
   */
  public WaitlistClient(
      URI baseUri, String instanceId, HttpClient httpClient, ObjectMapper mapper) {
    this.baseUri = baseUri;
    this.instanceId = instanceId;
    this.httpClient = httpClient;
    this.mapper = mapper;
  }

  /**
   * Returns the upstream origin.
   *
   * @return base URI
   */
  public URI baseUri() {
    return baseUri;
  }

  /**
   * Returns the instance id sent on each call.
   *
   * @return instance id
   */
  public String instanceId() {
    return instanceId;
  }

  /**
   * Calls {@code GET /health}.
   *
   * @return upstream status and body
   * @throws IOException when the call fails
   * @throws InterruptedException when the call is interrupted
   */
  public ApiReply health() throws IOException, InterruptedException {
    return exchange("GET", "/health", null);
  }

  /**
   * Creates a dining-room waitlist.
   *
   * @param name unique waitlist name
   * @param maxLength maximum waiting parties
   * @param minutes processing minutes used for the estimate
   * @return upstream status and body
   * @throws IOException when the call fails
   * @throws InterruptedException when the call is interrupted
   */
  public ApiReply createWaitlist(String name, int maxLength, int minutes)
      throws IOException, InterruptedException {
    ObjectNode body = mapper.createObjectNode();
    body.put("name", name);
    body.put("maxLength", maxLength);
    body.put("processingTimePerEntryMinutes", minutes);
    return exchange("POST", "/waitlists", mapper.writeValueAsString(body));
  }

  /**
   * Checks a party in. Priority 5 is the rush party and is seated first.
   *
   * @param waitlistId waitlist id
   * @param name party name
   * @param priority urgency from 1 to 5, or null for the service default
   * @return upstream status and body
   * @throws IOException when the call fails
   * @throws InterruptedException when the call is interrupted
   */
  public ApiReply checkIn(String waitlistId, String name, Integer priority)
      throws IOException, InterruptedException {
    ObjectNode body = mapper.createObjectNode();
    body.put("name", name);
    if (priority != null) {
      body.put("priority", priority);
    }
    String path = "/waitlists/" + encode(waitlistId) + "/entries";
    return exchange("POST", path, mapper.writeValueAsString(body));
  }

  /**
   * Reads one party, including position and estimate while it is waiting.
   *
   * @param waitlistId waitlist id
   * @param entryId entry id
   * @return upstream status and body
   * @throws IOException when the call fails
   * @throws InterruptedException when the call is interrupted
   */
  public ApiReply readEntry(String waitlistId, String entryId)
      throws IOException, InterruptedException {
    String path = "/waitlists/" + encode(waitlistId) + "/entries/" + encode(entryId);
    return exchange("GET", path, null);
  }

  /**
   * Reads the waitlist and every party in waiting order.
   *
   * @param waitlistId waitlist id
   * @return upstream status and body
   * @throws IOException when the call fails
   * @throws InterruptedException when the call is interrupted
   */
  public ApiReply readWaitlist(String waitlistId) throws IOException, InterruptedException {
    return exchange("GET", "/waitlists/" + encode(waitlistId), null);
  }

  /**
   * Calls the next waiting party.
   *
   * @param waitlistId waitlist id
   * @return upstream status and body; 204 when nobody is waiting
   * @throws IOException when the call fails
   * @throws InterruptedException when the call is interrupted
   */
  public ApiReply serveNext(String waitlistId) throws IOException, InterruptedException {
    return exchange("POST", "/waitlists/" + encode(waitlistId) + "/serve-next", null);
  }

  /**
   * Moves a party to {@code SERVED} or {@code CANCELLED}.
   *
   * @param waitlistId waitlist id
   * @param entryId entry id
   * @param status {@code SERVED} or {@code CANCELLED}
   * @return upstream status and body
   * @throws IOException when the call fails
   * @throws InterruptedException when the call is interrupted
   */
  public ApiReply updateStatus(String waitlistId, String entryId, String status)
      throws IOException, InterruptedException {
    ObjectNode body = mapper.createObjectNode();
    body.put("status", status);
    String path = "/waitlists/" + encode(waitlistId) + "/entries/" + encode(entryId);
    return exchange("PATCH", path, mapper.writeValueAsString(body));
  }

  /**
   * Sends one request and always includes {@code X-Client-Instance}.
   *
   * @param method HTTP method
   * @param path absolute upstream path
   * @param json request body, or null when there is none
   * @return upstream status and body
   * @throws IOException when the call fails
   * @throws InterruptedException when the call is interrupted
   */
  public ApiReply exchange(String method, String path, String json)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(path))
        .timeout(Duration.ofSeconds(20))
        .header("Accept", "application/json")
        .header(INSTANCE_HEADER, instanceId);
    if (json == null) {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      builder.header("Content-Type", "application/json");
      builder.method(method, HttpRequest.BodyPublishers.ofString(json));
    }
    HttpResponse<String> response = httpClient.send(
        builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    String responseBody = response.body() == null ? "" : response.body();
    return new ApiReply(response.statusCode(), responseBody);
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
