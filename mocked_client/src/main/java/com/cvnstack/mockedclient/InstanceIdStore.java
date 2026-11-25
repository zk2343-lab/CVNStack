package com.cvnstack.mockedclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Saves one client instance id per machine so later runs reuse it.
 *
 * <p>AI-assisted.
 */
public final class InstanceIdStore {

  /**
   * Returns the per-user file that holds this machine's instance id.
   *
   * @return {@code ~/.cvnstack/mocked-client-instance}
   */
  public Path defaultFile() {
    return Path.of(System.getProperty("user.home"), ".cvnstack", "mocked-client-instance");
  }

  /**
   * Reads a saved id, or writes a new UUID when the file is missing or blank.
   *
   * @param file destination, or null to use {@link #defaultFile()}
   * @return stable instance id for this file
   * @throws IOException when the file cannot be read or written
   */
  public String loadOrCreate(Path file) throws IOException {
    Path target = file == null ? defaultFile() : file;
    if (Files.isRegularFile(target)) {
      String existing = Files.readString(target).trim();
      if (!existing.isEmpty()) {
        return existing;
      }
    }
    String created = UUID.randomUUID().toString();
    Path parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(target, created + System.lineSeparator());
    return created;
  }
}
