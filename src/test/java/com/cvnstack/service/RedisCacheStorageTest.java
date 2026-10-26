package com.cvnstack.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.WaitingOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Checks cache data and lock ownership against an isolated Redis container.
 *
 * <p>AI-assisted.
 */
@Testcontainers
class RedisCacheStorageTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00.123456789Z");

  @Container
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
          .withExposedPorts(6379);

  private static LettuceConnectionFactory connectionFactory;
  private StringRedisTemplate redis;
  private RedisCacheStorage cache;

  @BeforeAll
  static void openConnection() {
    connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();
    connectionFactory.start();
  }

  @AfterAll
  static void closeConnection() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @BeforeEach
  void setUp() {
    redis = new StringRedisTemplate(connectionFactory);
    // This database belongs only to this test class's temporary container.
    try (RedisConnection connection = connectionFactory.getConnection()) {
      connection.serverCommands().flushDb();
    }
    cache = new RedisCacheStorage(redis);
  }

  // Waiting order tests
  @Test
  void missingWaitingOrderReturnsEmpty() {
    assertTrue(cache.findWaitingOrder("wl-1").isEmpty());
  }

  @Test
  void preservesPriorityAndInsertionOrder() {
    final Entry early = entry("z-early", "wl-1", 1, T0);
    final Entry late = entry("a-late", "wl-1", 1, T0.plusSeconds(1));
    final Entry urgent = entry("urgent", "wl-1", 5, T0.plusSeconds(2));
    final List<Entry> ranked = WaitingOrder.rankedWaiting(List.of(late, early, urgent));
    final List<String> ids = ranked.stream().map(Entry::id).toList();

    cache.replaceWaitingOrder("wl-1", ids);

    assertEquals(Optional.of(List.of("urgent", "z-early", "a-late")),
        cache.findWaitingOrder("wl-1"));
    assertEquals(0.0, redis.opsForZSet().score(prefix("wl-1") + "waiting", "urgent"));
    assertEquals(1.0, redis.opsForZSet().score(prefix("wl-1") + "waiting", "z-early"));
  }

  @Test
  void replacesOldWaitingOrder() {
    cache.replaceWaitingOrder("wl-1", List.of("a", "b"));

    cache.replaceWaitingOrder("wl-1", List.of("b"));

    assertEquals(Optional.of(List.of("b")), cache.findWaitingOrder("wl-1"));
  }

  @Test
  void removeWaitingMemberLeavesOtherRanks() {
    cache.replaceWaitingOrder("wl-1", List.of("urgent", "normal"));

    cache.removeWaitingMember("wl-1", "urgent");

    assertEquals(Optional.of(List.of("normal")), cache.findWaitingOrder("wl-1"));
    cache.removeWaitingMember("wl-1", "normal");
    assertEquals(Optional.of(List.of()), cache.findWaitingOrder("wl-1"));
  }

  @Test
  void distinguishesCachedEmptyOrderFromMissingOrder() {
    cache.replaceWaitingOrder("wl-1", List.of());

    assertEquals(Optional.of(List.of()), cache.findWaitingOrder("wl-1"));
    assertTrue(cache.findWaitingOrder("wl-2").isEmpty());
  }

  @Test
  void missingRankKeyIsNotReportedAsAnEmptyQueue() {
    cache.replaceWaitingOrder("wl-1", List.of("a"));
    redis.delete(prefix("wl-1") + "waiting");

    assertTrue(cache.findWaitingOrder("wl-1").isEmpty());
  }

  @Test
  void keepsWaitlistOrdersSeparate() {
    cache.replaceWaitingOrder("wl-1", List.of("a"));
    cache.replaceWaitingOrder("wl-2", List.of("b"));

    assertEquals(Optional.of(List.of("a")), cache.findWaitingOrder("wl-1"));
    assertEquals(Optional.of(List.of("b")), cache.findWaitingOrder("wl-2"));
  }

  @Test
  void rejectsDuplicateIdsWithoutChangingOrder() {
    cache.replaceWaitingOrder("wl-1", List.of("original"));

    assertThrows(IllegalArgumentException.class,
        () -> cache.replaceWaitingOrder("wl-1", List.of("a", "a")));
    assertEquals(Optional.of(List.of("original")), cache.findWaitingOrder("wl-1"));
  }

  // Entry cache tests
  @Test
  void storesAndReadsEntryFields() {
    final Entry entry = new Entry("e1", "wl-1", "Desktop: 데스크탑", 5, EntryStatus.WAITING, T0);

    cache.putEntry(entry);

    assertEquals(Optional.of(entry), cache.findEntry("wl-1", "e1"));
  }

  @Test
  void storesAndReadsCallTime() {
    Entry called = new Entry("e1", "wl-1", "Desktop", 5, EntryStatus.CALLED,
        T0, T0.plusSeconds(60));

    cache.putEntry(called);

    assertEquals(Optional.of(called), cache.findEntry("wl-1", "e1"));
  }

  @Test
  void preservesAllThreeTimestampsAndReplacesWriteTime() {
    Entry stored = new Entry("e1", "wl-1", "Desktop", 5, EntryStatus.CALLED,
        T0, T0.plusSeconds(60), T0.plusSeconds(61));
    cache.putEntry(stored);
    assertEquals(Optional.of(stored), cache.findEntry("wl-1", "e1"));

    Entry updated = new Entry("e1", "wl-1", "Desktop", 5, EntryStatus.SERVED,
        T0, T0.plusSeconds(60), T0.plusSeconds(120));
    cache.putEntry(updated);
    assertEquals(Optional.of(updated), cache.findEntry("wl-1", "e1"));
  }

  @Test
  void replacingEntryWithoutWriteTimeClearsOldValue() {
    Entry stored = new Entry("e1", "wl-1", "Desktop", 5, EntryStatus.WAITING,
        T0, null, T0.plusSeconds(1));
    cache.putEntry(stored);
    Entry withoutWriteTime = new Entry("e1", "wl-1", "Desktop", 5, EntryStatus.WAITING, T0);

    cache.putEntry(withoutWriteTime);

    assertEquals(Optional.of(withoutWriteTime), cache.findEntry("wl-1", "e1"));
  }

  @Test
  void readsLegacyPayloadWithoutWriteTime() {
    Entry original = entry("e1", "wl-1", 1, T0);
    cache.putEntry(original);
    String key = prefix("wl-1") + "entry:"
        + Base64.getUrlEncoder().withoutPadding()
            .encodeToString("e1".getBytes(StandardCharsets.UTF_8));
    redis.opsForHash().delete(key, "updatedAt");

    assertEquals(Optional.of(original), cache.findEntry("wl-1", "e1"));
  }

  @Test
  void replacingEntryWithoutCallTimeClearsOldValue() {
    Entry called = new Entry("e1", "wl-1", "Desktop", 5, EntryStatus.CALLED,
        T0, T0.plusSeconds(60));
    cache.putEntry(called);
    assertEquals(Optional.of(called), cache.findEntry("wl-1", "e1"));
    Entry withoutCallTime = new Entry("e1", "wl-1", "Desktop", 5, EntryStatus.CALLED, T0);

    cache.putEntry(withoutCallTime);

    assertEquals(Optional.of(withoutCallTime), cache.findEntry("wl-1", "e1"));
  }

  @Test
  void readsCachedEntryWithoutTheNewCallTimeField() {
    Entry original = entry("e1", "wl-1", 1, T0);
    cache.putEntry(original);
    String key = prefix("wl-1") + "entry:"
        + Base64.getUrlEncoder().withoutPadding()
            .encodeToString("e1".getBytes(StandardCharsets.UTF_8));
    redis.opsForHash().delete(key, "calledAt");

    assertEquals(Optional.of(original), cache.findEntry("wl-1", "e1"));
  }

  @Test
  void updatesCachedEntry() {
    final Entry entry = entry("e1", "wl-1", 1, T0);
    cache.putEntry(entry);
    final Entry updated = entry.withPriority(5).withStatus(EntryStatus.CALLED);

    cache.putEntry(updated);

    assertEquals(Optional.of(updated), cache.findEntry("wl-1", "e1"));
  }

  @Test
  void olderEntryWriteDoesNotReplaceNewerPayload() {
    Entry newer = new Entry("e1", "wl-1", "Desktop", 4, EntryStatus.WAITING,
        T0, null, T0.plusSeconds(2));
    Entry older = new Entry("e1", "wl-1", "Desktop", 2, EntryStatus.WAITING,
        T0, null, T0.plusSeconds(1));

    cache.putEntry(newer);
    cache.putEntry(older);

    assertEquals(Optional.of(newer), cache.findEntry("wl-1", "e1"));
  }

  @Test
  void olderWaitingOrderRevisionDoesNotReplaceNewerRank() {
    assertTrue(cache.replaceWaitingOrderIfNewer(
        "wl-1", List.of("urgent", "normal"), T0.plusSeconds(2)));

    assertFalse(cache.replaceWaitingOrderIfNewer(
        "wl-1", List.of("normal", "urgent"), T0.plusSeconds(1)));
    assertEquals(Optional.of(List.of("urgent", "normal")), cache.findWaitingOrder("wl-1"));
  }

  @Test
  void newerWaitingOrderRevisionReplacesRank() {
    cache.replaceWaitingOrderIfNewer("wl-1", List.of("normal"), T0.plusSeconds(1));

    assertTrue(cache.replaceWaitingOrderIfNewer(
        "wl-1", List.of("urgent", "normal"), T0.plusSeconds(2)));
    assertEquals(Optional.of(List.of("urgent", "normal")), cache.findWaitingOrder("wl-1"));
  }

  @Test
  void invalidateClearsRevisionSoALaterRebuildCanWrite() {
    cache.replaceWaitingOrderIfNewer("wl-1", List.of("urgent"), T0.plusSeconds(2));

    cache.invalidateWaitlist("wl-1");

    assertTrue(cache.replaceWaitingOrderIfNewer("wl-1", List.of("normal"), T0.plusSeconds(1)));
    assertEquals(Optional.of(List.of("normal")), cache.findWaitingOrder("wl-1"));
  }

  @Test
  void missingEntryOrAnotherWaitlistReturnsEmpty() {
    cache.putEntry(entry("e1", "wl-1", 1, T0));

    assertTrue(cache.findEntry("wl-1", "missing").isEmpty());
    assertTrue(cache.findEntry("wl-2", "e1").isEmpty());
  }

  @Test
  void deletesOnlyTheRequestedEntryPayload() {
    final Entry retained = entry("e2", "wl-1", 1, T0);
    cache.putEntry(entry("e1", "wl-1", 1, T0));
    cache.putEntry(retained);
    cache.replaceWaitingOrder("wl-1", List.of("e1", "e2"));

    cache.deleteEntry("wl-1", "e1");

    assertTrue(cache.findEntry("wl-1", "e1").isEmpty());
    assertEquals(Optional.of(retained), cache.findEntry("wl-1", "e2"));
    assertEquals(Optional.of(List.of("e1", "e2")), cache.findWaitingOrder("wl-1"));
  }

  @Test
  void idsContainingSeparatorsDoNotCollide() {
    final Entry first = entry("y", "x:entry", 1, T0);
    final Entry second = entry("entry:y", "x", 5, T0);
    cache.putEntry(first);
    cache.putEntry(second);

    cache.invalidateWaitlist("x:entry");

    assertTrue(cache.findEntry("x:entry", "y").isEmpty());
    assertEquals(Optional.of(second), cache.findEntry("x", "entry:y"));
  }

  @Test
  void invalidatesOnlyTheWaitlistCacheAndKeepsItsLock() {
    cache.putEntry(entry("waiting", "wl-1", 1, T0));
    cache.putEntry(entry("called", "wl-1", 1, T0).withStatus(EntryStatus.CALLED));
    final Entry other = entry("e2", "wl-2", 1, T0);
    cache.putEntry(other);
    cache.replaceWaitingOrder("wl-1", List.of("waiting"));
    cache.replaceWaitingOrder("wl-2", List.of("e2"));
    assertTrue(cache.tryAcquireServeLock("wl-1"));

    cache.invalidateWaitlist("wl-1");

    assertTrue(cache.findWaitingOrder("wl-1").isEmpty());
    assertTrue(cache.findEntry("wl-1", "waiting").isEmpty());
    assertTrue(cache.findEntry("wl-1", "called").isEmpty());
    assertEquals(Optional.of(other), cache.findEntry("wl-2", "e2"));
    assertEquals(Optional.of(List.of("e2")), cache.findWaitingOrder("wl-2"));
    assertFalse(new RedisCacheStorage(redis).tryAcquireServeLock("wl-1"));
    cache.releaseServeLock("wl-1");
  }

  // Serve lock tests
  @Test
  void onlyOneCallerAcquiresTheLock() {
    final RedisCacheStorage other = new RedisCacheStorage(redis);

    assertTrue(cache.tryAcquireServeLock("wl-1"));
    assertFalse(cache.tryAcquireServeLock("wl-1"));
    assertFalse(other.tryAcquireServeLock("wl-1"));
    cache.releaseServeLock("wl-1");
    assertTrue(other.tryAcquireServeLock("wl-1"));
    other.releaseServeLock("wl-1");
  }

  @Test
  void nonOwnerCannotReleaseTheLock() {
    final RedisCacheStorage other = new RedisCacheStorage(redis);
    assertTrue(cache.tryAcquireServeLock("wl-1"));

    other.releaseServeLock("wl-1");

    assertFalse(other.tryAcquireServeLock("wl-1"));
    cache.releaseServeLock("wl-1");
  }

  @Test
  void anotherThreadCannotReleaseTheSameInstancesLock() throws Exception {
    assertTrue(cache.tryAcquireServeLock("wl-1"));
    try (var executor = Executors.newSingleThreadExecutor()) {
      final var result = executor.submit(() -> {
        cache.releaseServeLock("wl-1");
        return cache.tryAcquireServeLock("wl-1");
      });
      assertFalse(result.get(5, TimeUnit.SECONDS));
    }
    cache.releaseServeLock("wl-1");
  }

  @Test
  void expiredOwnerCannotReleaseANewerLock() {
    final RedisCacheStorage other = new RedisCacheStorage(redis);
    assertTrue(cache.tryAcquireServeLock("wl-1"));
    redis.expire(prefix("wl-1") + "serve-lock", Duration.ZERO);
    assertTrue(other.tryAcquireServeLock("wl-1"));

    cache.releaseServeLock("wl-1");

    assertFalse(cache.tryAcquireServeLock("wl-1"));
    other.releaseServeLock("wl-1");
  }

  @Test
  void lockExpiresWhenItsOwnerDoesNotReleaseIt() {
    final RedisCacheStorage firstOwner = new RedisCacheStorage(redis);
    assertTrue(firstOwner.tryAcquireServeLock("wl-1"));
    String key = prefix("wl-1") + "serve-lock";
    long remainingMillis = redis.getExpire(key, TimeUnit.MILLISECONDS);
    assertTrue(remainingMillis > 0 && remainingMillis <= 30_000);

    // Shorten only this test lock so the test does not wait thirty seconds.
    redis.expire(key, Duration.ofMillis(100));

    await().atMost(Duration.ofSeconds(5))
        .until(() -> !Boolean.TRUE.equals(redis.hasKey(prefix("wl-1") + "serve-lock")));

    assertTrue(cache.tryAcquireServeLock("wl-1"));
    firstOwner.releaseServeLock("wl-1");
    assertFalse(new RedisCacheStorage(redis).tryAcquireServeLock("wl-1"));
    cache.releaseServeLock("wl-1");
  }

  @Test
  void differentWaitlistsHaveIndependentLocks() {
    assertTrue(cache.tryAcquireServeLock("wl-1"));
    assertTrue(cache.tryAcquireServeLock("wl-2"));

    cache.releaseServeLock("wl-1");

    assertFalse(new RedisCacheStorage(redis).tryAcquireServeLock("wl-2"));
    cache.releaseServeLock("wl-2");
  }

  private static Entry entry(
      final String id, final String waitlistId, final int priority, final Instant insertedAt) {
    return new Entry(id, waitlistId, id, priority, EntryStatus.WAITING, insertedAt);
  }

  private static String prefix(final String waitlistId) {
    return "cvnstack:waitlist:" + Base64.getUrlEncoder().withoutPadding()
        .encodeToString(waitlistId.getBytes(StandardCharsets.UTF_8)) + ":";
  }
}
