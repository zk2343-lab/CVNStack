package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Checks input guards and Redis failures with a mock connection.
 *
 * <p>AI-assisted.
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheStorageUnitTest {

  @Mock
  private StringRedisTemplate redis;

  @Mock
  private ValueOperations<String, String> values;

  private RedisCacheStorage cache;

  @BeforeEach
  void setUp() {
    cache = new RedisCacheStorage(redis);
  }

  @Test
  void rejectsDuplicateIdsBeforeWritingToRedis() {
    assertThrows(IllegalArgumentException.class,
        () -> cache.replaceWaitingOrder("wl-1", List.of("a", "a")));
    assertThrows(IllegalArgumentException.class,
        () -> cache.replaceWaitingOrderIfNewer("wl-1", List.of("a", "a"), Instant.EPOCH));
    verifyNoInteractions(redis);
  }

  @Test
  void releasingAnUnownedLockDoesNotContactRedis() {
    cache.releaseServeLock("wl-1");

    verifyNoInteractions(redis);
  }

  @Test
  void connectionFailureIsNotReportedAsLockContention() {
    when(redis.opsForValue()).thenReturn(values);
    when(values.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(30))))
        .thenThrow(new RedisConnectionFailureException("Redis unavailable"));

    assertThrows(RedisConnectionFailureException.class,
        () -> cache.tryAcquireServeLock("wl-1"));
  }
}
