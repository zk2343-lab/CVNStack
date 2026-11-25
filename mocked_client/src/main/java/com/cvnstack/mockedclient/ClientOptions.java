package com.cvnstack.mockedclient;

import java.nio.file.Path;

/**
 * Parsed command line for the restaurant client.
 *
 * <p>AI-assisted.
 *
 * @param command subcommand name
 * @param mode {@code local} or {@code gcp} when baseUrl is omitted
 * @param baseUrl explicit upstream origin, or null
 * @param instanceFile saved instance id file, or null for the machine default
 * @param listenPort port for the local restaurant server
 * @param name waitlist or party name
 * @param maxLength waitlist capacity
 * @param minutes processing minutes per party
 * @param waitlistId waitlist id
 * @param entryId entry id
 * @param priority party priority from 1 to 5, or null
 */
public record ClientOptions(
    String command,
    String mode,
    String baseUrl,
    Path instanceFile,
    int listenPort,
    String name,
    Integer maxLength,
    Integer minutes,
    String waitlistId,
    String entryId,
    Integer priority) {

  /**
   * Parses CLI arguments. Upstream flags may appear before or after the command.
   *
   * @param args raw arguments
   * @return parsed options
   */
  public static ClientOptions parse(String[] args) {
    if (args == null || args.length == 0) {
      throw new IllegalArgumentException("missing command");
    }
    String command = null;
    String mode = UpstreamSelector.LOCAL_MODE;
    String baseUrl = null;
    Path instanceFile = null;
    int listenPort = RestaurantServer.DEFAULT_PORT;
    String name = null;
    Integer maxLength = null;
    Integer minutes = null;
    String waitlistId = null;
    String entryId = null;
    Integer priority = null;
    for (int index = 0; index < args.length; index++) {
      String token = args[index];
      if (token.startsWith("--")) {
        String value = requiredValue(args, index, token);
        index++;
        switch (token) {
          case "--mode" -> mode = value;
          case "--base-url" -> baseUrl = value;
          case "--instance-file" -> instanceFile = Path.of(value);
          case "--port" -> listenPort = parseInt(value, "port");
          case "--name" -> name = value;
          case "--max-length" -> maxLength = parseInt(value, "max-length");
          case "--minutes" -> minutes = parseInt(value, "minutes");
          case "--waitlist" -> waitlistId = value;
          case "--entry" -> entryId = value;
          case "--priority" -> priority = parseInt(value, "priority");
          default -> throw new IllegalArgumentException("unknown option " + token);
        }
      } else if (command == null) {
        command = token;
      } else {
        throw new IllegalArgumentException("unexpected argument " + token);
      }
    }
    if (command == null || command.isBlank()) {
      throw new IllegalArgumentException("missing command");
    }
    return new ClientOptions(
        command,
        mode,
        baseUrl,
        instanceFile,
        listenPort,
        name,
        maxLength,
        minutes,
        waitlistId,
        entryId,
        priority);
  }

  private static String requiredValue(String[] args, int index, String token) {
    if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
      throw new IllegalArgumentException("missing value for " + token);
    }
    return args[index + 1];
  }

  private static int parseInt(String value, String flag) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(flag + " must be an integer");
    }
  }
}
