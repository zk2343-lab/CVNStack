package com.cvnstack.mockedclient;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;

/**
 * Runs one restaurant-client command against the waitlist service.
 *
 * <p>AI-assisted.
 */
public final class CommandRunner {

  /**
   * Runs the parsed command.
   *
   * @param options parsed arguments
   * @param out destination for status codes and bodies
   * @return process status, 0 when the upstream call succeeded
   * @throws IOException when the instance file or the upstream call fails
   * @throws InterruptedException when a call is interrupted
   */
  public int run(ClientOptions options, PrintStream out)
      throws IOException, InterruptedException {
    if ("help".equals(options.command())) {
      MockedClientMain.printUsage(out);
      return 0;
    }
    validate(options);
    String instanceId = new InstanceIdStore().loadOrCreate(options.instanceFile());
    URI baseUri = new UpstreamSelector().select(options.mode(), options.baseUrl());
    WaitlistClient client = new WaitlistClient(baseUri, instanceId);
    return switch (options.command()) {
      case "serve" -> serve(options, client, out);
      case "health" -> print(client.health(), out);
      case "create-waitlist" -> print(createWaitlist(options, client), out);
      case "check-in" -> print(checkIn(options, client), out);
      case "position" -> print(readPosition(options, client), out);
      case "serve-next" -> print(serveNext(options, client), out);
      case "served" -> print(update(options, client, "SERVED"), out);
      case "cancelled" -> print(update(options, client, "CANCELLED"), out);
      default -> throw new IllegalArgumentException("unknown command " + options.command());
    };
  }

  private void validate(ClientOptions options) {
    switch (options.command()) {
      case "serve", "health" -> {
        // no extra flags
      }
      case "create-waitlist" -> {
        required(options.name(), "name");
        requiredInt(options.maxLength(), "max-length");
        requiredInt(options.minutes(), "minutes");
      }
      case "check-in" -> {
        required(options.waitlistId(), "waitlist");
        required(options.name(), "name");
      }
      case "position", "served", "cancelled" -> {
        required(options.waitlistId(), "waitlist");
        required(options.entryId(), "entry");
      }
      case "serve-next" -> required(options.waitlistId(), "waitlist");
      default -> throw new IllegalArgumentException("unknown command " + options.command());
    }
  }

  private int serve(ClientOptions options, WaitlistClient client, PrintStream out)
      throws IOException {
    RestaurantServer server = new RestaurantServer(client, options.mode(),
        options.listenPort(), true, new StandBookStore(StandBookStore.defaultDirectory()));
    server.start();
    out.println("Restaurant client http://127.0.0.1:" + server.port());
    out.println("Upstream " + client.baseUri());
    out.println("Instance " + client.instanceId());
    try {
      Thread.currentThread().join();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      server.close();
    }
    return 0;
  }

  private ApiReply createWaitlist(ClientOptions options, WaitlistClient client)
      throws IOException, InterruptedException {
    return client.createWaitlist(
        required(options.name(), "name"),
        requiredInt(options.maxLength(), "max-length"),
        requiredInt(options.minutes(), "minutes"));
  }

  private ApiReply checkIn(ClientOptions options, WaitlistClient client)
      throws IOException, InterruptedException {
    return client.checkIn(
        required(options.waitlistId(), "waitlist"),
        required(options.name(), "name"),
        options.priority());
  }

  private ApiReply serveNext(ClientOptions options, WaitlistClient client)
      throws IOException, InterruptedException {
    return client.serveNext(required(options.waitlistId(), "waitlist"));
  }

  private ApiReply readPosition(ClientOptions options, WaitlistClient client)
      throws IOException, InterruptedException {
    return client.readEntry(
        required(options.waitlistId(), "waitlist"),
        required(options.entryId(), "entry"));
  }

  private ApiReply update(ClientOptions options, WaitlistClient client, String status)
      throws IOException, InterruptedException {
    return client.updateStatus(
        required(options.waitlistId(), "waitlist"),
        required(options.entryId(), "entry"),
        status);
  }

  private int print(ApiReply reply, PrintStream out) {
    out.println(reply.status());
    if (!reply.body().isBlank()) {
      out.println(reply.body());
    }
    return reply.status() >= 400 ? 1 : 0;
  }

  private String required(String value, String flag) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("missing --" + flag);
    }
    return value;
  }

  private int requiredInt(Integer value, String flag) {
    if (value == null) {
      throw new IllegalArgumentException("missing --" + flag);
    }
    return value;
  }
}
