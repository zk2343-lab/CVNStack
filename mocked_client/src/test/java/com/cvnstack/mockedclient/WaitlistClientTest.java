package com.cvnstack.mockedclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks that the client sends {@code X-Client-Instance} and the waitlist paths.
 *
 * <p>AI-assisted.
 */
class WaitlistClientTest {

  private FakeUpstream upstream;

  @BeforeEach
  void startUpstream() throws Exception {
    upstream = FakeUpstream.start();
  }

  @AfterEach
  void stopUpstream() {
    upstream.close();
  }

  @Test
  void twoInstancesStayDistinct() throws Exception {
    WaitlistClient first = client("machine-a");
    WaitlistClient second = client("machine-b");
    ApiReply created = first.createWaitlist("Dining room", 10, 5);
    second.checkIn("wl-1", "Window", 1);
    assertEquals(200, created.status());
    assertTrue(created.body().contains("machine-a"));
    assertEquals(java.util.List.of("machine-a", "machine-b"), upstream.instanceIds());
  }

  @Test
  void rushCheckInSendsPriorityFive() throws Exception {
    ApiReply reply = client("machine-a").checkIn("wl-1", "Rush", 5);
    assertEquals(200, reply.status());
    assertTrue(reply.body().contains("\"priority\":5"));
    assertTrue(reply.body().contains("\"position\":1"));
    assertTrue(upstream.bodies().get(0).contains("\"priority\":5"));
  }

  @Test
  void readsPositionAndSeatsNext() throws Exception {
    WaitlistClient client = client("machine-a");
    ApiReply position = client.readEntry("wl-1", "en-1");
    ApiReply called = client.serveNext("wl-1");
    ApiReply served = client.updateStatus("wl-1", "en-1", "SERVED");
    ApiReply empty = client.serveNext("empty");
    assertTrue(position.body().contains("\"estimatedWaitTimeMinutes\":0"));
    assertTrue(called.body().contains("CALLED"));
    assertTrue(served.body().contains("SERVED"));
    assertEquals(204, empty.status());
  }

  private WaitlistClient client(String instanceId) {
    return new WaitlistClient(java.net.URI.create(upstream.baseUrl()), instanceId);
  }
}
