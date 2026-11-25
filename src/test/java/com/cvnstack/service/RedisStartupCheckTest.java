package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Checks that startup accepts a Redis PONG and rejects any other reply.
 *
 * <p>AI-assisted.
 */
@ExtendWith(MockitoExtension.class)
class RedisStartupCheckTest {

  @Mock
  private StringRedisTemplate redis;

  @Mock
  private RedisConnectionFactory connections;

  @Mock
  private RedisConnection connection;

  @Test
  void pongLetsStartupContinue() {
    when(redis.getConnectionFactory()).thenReturn(connections);
    when(connections.getConnection()).thenReturn(connection);
    when(connection.ping()).thenReturn("PONG");

    assertDoesNotThrow(() -> new RedisStartupCheck(redis).run(null));
  }

  @Test
  void anyOtherReplyStopsStartup() {
    when(redis.getConnectionFactory()).thenReturn(connections);
    when(connections.getConnection()).thenReturn(connection);
    when(connection.ping()).thenReturn("LOADING");

    RedisStartupCheck check = new RedisStartupCheck(redis);
    assertThrows(IllegalStateException.class, () -> check.run(null));
  }
}
