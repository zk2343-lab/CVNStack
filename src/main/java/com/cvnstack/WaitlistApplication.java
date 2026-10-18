package com.cvnstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the waitlist service.
 *
 * <p>AI-assisted.
 */
@SpringBootApplication
public class WaitlistApplication {

  /**
   * Boots the Spring application.
   *
   * @param args command-line arguments forwarded to Spring Boot
   */
  public static void main(final String[] args) {
    SpringApplication.run(WaitlistApplication.class, args);
  }
}
