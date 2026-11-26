package com.cvnstack.mockedclient;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Confirms the client build does not put waitlist server types on the classpath.
 *
 * <p>AI-assisted.
 */
class ClasspathBoundaryTest {

  @Test
  void serverControllerIsAbsent() {
    assertThrows(ClassNotFoundException.class,
        () -> Class.forName("com.cvnstack.controller.HealthController"));
  }
}
