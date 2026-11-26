package com.cvnstack.mockedclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Keeps each service's rooms and guest details on disk, since the waitlist API does not store them.
 *
 * <p>AI-assisted.
 */
public final class StandBookStore {

  private static final byte[] EMPTY = "{}".getBytes(StandardCharsets.UTF_8);

  private final Path directory;

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Creates a store that writes one file per service mode.
   *
   * @param directory folder for the book files
   */
  public StandBookStore(Path directory) {
    this.directory = directory;
  }

  /**
   * Returns the per-user folder shared with the instance id file.
   *
   * @return {@code ~/.cvnstack}
   */
  public static Path defaultDirectory() {
    return Path.of(System.getProperty("user.home"), ".cvnstack");
  }

  /**
   * Reads the saved book for a mode.
   *
   * @param mode {@code local} or {@code gcp}
   * @return saved JSON object, or {@code {}} when nothing is saved yet
   * @throws IOException when the file cannot be read
   */
  public synchronized byte[] read(String mode) throws IOException {
    Path file = file(mode);
    return Files.isRegularFile(file) ? Files.readAllBytes(file) : EMPTY.clone();
  }

  /**
   * Replaces the saved book for a mode.
   *
   * @param mode {@code local} or {@code gcp}
   * @param json a JSON object
   * @throws IllegalArgumentException when the body is not a JSON object
   * @throws IOException when the file cannot be written
   */
  public synchronized void write(String mode, byte[] json) throws IOException {
    JsonNode node;
    try {
      node = mapper.readTree(json);
    } catch (IOException ex) {
      throw new IllegalArgumentException("book must be JSON", ex);
    }
    if (node == null || !node.isObject()) {
      throw new IllegalArgumentException("book must be a JSON object");
    }
    Files.createDirectories(directory);
    Path target = file(mode);
    Path temp = Files.createTempFile(directory, "mocked-client-book", ".tmp");
    Files.write(temp, mapper.writeValueAsBytes(node));
    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);
  }

  private Path file(String mode) {
    if (!UpstreamSelector.LOCAL_MODE.equals(mode) && !UpstreamSelector.CLOUD_MODE.equals(mode)) {
      throw new IllegalArgumentException("mode must be local or gcp");
    }
    return directory.resolve("mocked-client-book-" + mode + ".json");
  }
}
