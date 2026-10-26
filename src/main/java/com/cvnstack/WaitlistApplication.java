package com.cvnstack;

import com.fasterxml.jackson.databind.DeserializationFeature;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

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
   * Lets the visualizer on port 8765 call the API on port 8080.
   *
   * @return CORS filter for the local visualizer origin
   */
  @Bean
  public CorsFilter visualizerCorsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://127.0.0.1:8765", "http://localhost:8765"));
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE"));
    config.setAllowedHeaders(List.of("*"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
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
