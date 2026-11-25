package com.cvnstack.mockedclient;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Entry point for the restaurant host stand and its command-line actions.
 *
 * <p>AI-assisted.
 */
public final class MockedClientMain {

  private MockedClientMain() {}

  /**
   * Starts a command or the local restaurant server.
   *
   * @param args command line
   */
  public static void main(String[] args) {
    try {
      ClientOptions options = ClientOptions.parse(args);
      int code = new CommandRunner().run(options, System.out);
      if (!"serve".equals(options.command())) {
        System.exit(code);
      }
    } catch (IllegalArgumentException ex) {
      System.err.println(ex.getMessage());
      printUsage(System.err);
      System.exit(2);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      System.err.println(ex.getMessage());
      System.exit(1);
    } catch (IOException ex) {
      System.err.println(ex.getMessage());
      System.exit(1);
    }
  }

  /**
   * Prints the command summary.
   *
   * @param out destination
   */
  public static void printUsage(PrintStream out) {
    out.println("Restaurant host stand for the waitlist service.");
    out.println("serve [--mode local|gcp] [--base-url URL] [--port 8090]");
    out.println("health | create-waitlist | check-in | position | serve-next");
    out.println("served | cancelled");
    out.println("create-waitlist --name NAME --max-length N --minutes N");
    out.println("check-in --waitlist ID --name PARTY [--priority 5]");
    out.println("position --waitlist ID --entry ID");
    out.println("serve-next --waitlist ID");
    out.println("served|cancelled --waitlist ID --entry ID");
    out.println("local uses http://localhost:8080. gcp uses the hosted service.");
    out.println("Instance id file: ~/.cvnstack/mocked-client-instance");
  }
}
