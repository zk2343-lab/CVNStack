package com.cvnstack.mockedclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks the command line against a local stand-in service.
 *
 * <p>AI-assisted.
 */
class CommandRunnerTest {

  @TempDir
  Path directory;

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
  void createWaitlistPrintsTheUpstreamBody() throws Exception {
    ClientOptions options = ClientOptions.parse(new String[] {
        "create-waitlist",
        "--base-url", upstream.baseUrl(),
        "--instance-file", directory.resolve("instance").toString(),
        "--name", "Dining room",
        "--max-length", "10",
        "--minutes", "5"
    });
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
    int code = new CommandRunner().run(options, out);
    String text = buffer.toString(StandardCharsets.UTF_8);
    assertEquals(0, code);
    assertTrue(text.startsWith("200"));
    assertTrue(text.contains("wl-1"));
    assertEquals(1, upstream.instanceIds().size());
  }

  @Test
  void missingNameIsAUsageError() {
    ClientOptions options = ClientOptions.parse(new String[] {
        "create-waitlist",
        "--base-url", upstream.baseUrl(),
        "--max-length", "10",
        "--minutes", "5"
    });
    PrintStream out = new PrintStream(OutputStream.nullOutputStream());
    assertThrows(IllegalArgumentException.class, () -> new CommandRunner().run(options, out));
  }
}
