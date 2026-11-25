package com.cvnstack.mockedclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks that one machine reuses a saved instance id.
 *
 * <p>AI-assisted.
 */
class InstanceIdStoreTest {

  @TempDir
  Path directory;

  @Test
  void reusesSavedInstanceId() throws Exception {
    Path file = directory.resolve("instance");
    InstanceIdStore store = new InstanceIdStore();
    String created = store.loadOrCreate(file);
    assertEquals(created, store.loadOrCreate(file));
    assertFalse(created.isBlank());
  }

  @Test
  void replacesBlankFile() throws Exception {
    Path file = directory.resolve("blank");
    Files.writeString(file, "  \n");
    String created = new InstanceIdStore().loadOrCreate(file);
    assertFalse(created.isBlank());
    assertEquals(created, Files.readString(file).trim());
  }
}
