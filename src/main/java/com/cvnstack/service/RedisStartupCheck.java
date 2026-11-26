package com.cvnstack.service;

import java.util.Objects;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Pings Redis before serving when the image asks for a store check.
 *
 * <p>AI-assisted.
 */
@Component
@ConditionalOnProperty(name = "cvnstack.startup.verify-stores", havingValue = "true")
public class RedisStartupCheck implements ApplicationRunner {

  private final StringRedisTemplate redis;

  /**
   * Creates the startup check.
   *
   * @param redis Redis client for the configured host
   */
  public RedisStartupCheck(final StringRedisTemplate redis) {
    this.redis = redis;
  }

  /**
   * Fails startup unless Redis answers PING.
   *
   * @param args unused command-line arguments
   */
  @Override
  public void run(final ApplicationArguments args) {
    try (RedisConnection connection =
        Objects.requireNonNull(redis.getConnectionFactory()).getConnection()) {
      String reply = connection.ping();
      if (!"PONG".equalsIgnoreCase(reply)) {
        throw new IllegalStateException("Redis PING returned " + reply);
      }
    }
  }
}
