package com.cvnstack;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Starts the waitlist service.
 *
 * <p>AI-assisted.
 */
@SpringBootApplication
public class WaitlistApplication {

  /**
   * Rejects floating-point JSON numbers for integer targets instead of truncating them.
   *
   * @return customization shared by Spring Boot's JSON mappers
   */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer integerInputCustomizer() {
    return builder -> builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }

  /**
   * Boots the Spring application.
   *
   * @param args command-line arguments forwarded to Spring Boot
   */
  public static void main(final String[] args) {
    SpringApplication.run(WaitlistApplication.class, args);
  }
}
