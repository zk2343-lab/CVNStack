package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.lettuce.core.ClientOptions;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks that a missing Memorystore CA fails before Lettuce starts.
 *
 * <p>AI-assisted.
 */
class RedisCertificateConfigTest {

  @TempDir
  private Path tempDir;

  @Test
  void missingCertificateStopsStartup() {
    RedisCertificateConfig config = new RedisCertificateConfig();
    assertThrows(IllegalStateException.class,
        () -> config.redisServerCertificate(tempDir.resolve("missing.pem").toString()));
  }

  @Test
  void existingCertificateIsInstalled() throws Exception {
    java.nio.file.Path pem = Files.createFile(tempDir.resolve("server-ca.pem"));
    RedisCertificateConfig config = new RedisCertificateConfig();

    config.redisServerCertificate(pem.toString())
        .customize(ClientOptions.builder());
  }
}
