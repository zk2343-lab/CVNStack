package com.cvnstack.service;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Runs add, patch, delete, and serve-next against live PostgreSQL and Redis together.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.autoconfigure.exclude=")
@Testcontainers
class StoreConsistencyTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
          .withExposedPorts(6379);

  @LocalServerPort
  private int port;

  @Autowired
  private WaitlistStorage storage;

  @Autowired
  private CacheStorage cache;

  @Autowired
  private CacheConsistencyService consistency;

  @Autowired
  private JdbcTemplate jdbc;

  @Autowired
  private StringRedisTemplate redis;

  private Waitlist waitlist;

  @DynamicPropertySource
  static void configureStores(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @BeforeEach
  void setUp() {
    jdbc.execute("TRUNCATE entries, waitlists");
    try (RedisConnection connection = redis.getConnectionFactory().getConnection()) {
      connection.serverCommands().flushDb();
    }
    waitlist = storage.saveWaitlist(new Waitlist(
        UUID.randomUUID().toString(), "Support-" + UUID.randomUUID(), 10, 5));
  }

  @Test
  void debugStoresExposesPostgresRowsAndRedisKeys() {
    given().port(port)
        .when().get("/debug/stores")
        .then().statusCode(200)
        .body("waitlists.size()", equalTo(1))
        .body("waitlists[0].id", equalTo(waitlist.id()))
        .body("entries.size()", equalTo(0));
  }

  @Test
  void wipeStoresClearsPostgresRowsAndRedisKeys() {
    given().port(port).contentType("application/json")
        .body("{\"name\":\"Alice\",\"priority\":1}")
        .when().post("/waitlists/{id}/entries", waitlist.id())
        .then().statusCode(200);
    assertTrue(cache.findWaitingOrder(waitlist.id()).isPresent());

    given().port(port)
        .when().post("/debug/wipe-stores")
        .then().statusCode(204);

    given().port(port)
        .when().get("/debug/stores")
        .then().statusCode(200)
        .body("waitlists.size()", equalTo(0))
        .body("entries.size()", equalTo(0))
        .body("redis.size()", equalTo(0));
  }

  @Test
  void addPatchDeleteAndServeNextKeepRedisAlignedWithPostgres() {
    String normalId = given().port(port).contentType("application/json")
        .body("{\"name\":\"Alice\",\"priority\":1}")
        .when().post("/waitlists/{id}/entries", waitlist.id())
        .then().statusCode(200)
        .body("position", equalTo(1))
        .extract().path("entryId");
    String urgentId = given().port(port).contentType("application/json")
        .body("{\"name\":\"Bob\",\"priority\":5}")
        .when().post("/waitlists/{id}/entries", waitlist.id())
        .then().statusCode(200)
        .body("position", equalTo(1))
        .extract().path("entryId");

    assertEquals(Optional.of(List.of(urgentId, normalId)), cache.findWaitingOrder(waitlist.id()));
    assertEquals(EntryStatus.WAITING,
        storage.findEntry(waitlist.id(), urgentId).orElseThrow().status());
    assertTrue(cache.findEntry(waitlist.id(), urgentId).isPresent());

    given().port(port).contentType("application/json").body("{\"priority\":2}")
        .when().patch("/waitlists/{id}/entries/{entryId}", waitlist.id(), urgentId)
        .then().statusCode(200)
        .body("position", equalTo(1));
    assertEquals(Optional.of(List.of(urgentId, normalId)), cache.findWaitingOrder(waitlist.id()));
    assertEquals(2, storage.findEntry(waitlist.id(), urgentId).orElseThrow().priority());
    assertEquals(2, cache.findEntry(waitlist.id(), urgentId).orElseThrow().priority());

    given().port(port)
        .when().delete("/waitlists/{id}/entries/{entryId}", waitlist.id(), normalId)
        .then().statusCode(200);
    assertTrue(storage.findEntry(waitlist.id(), normalId).isEmpty());
    assertTrue(cache.findEntry(waitlist.id(), normalId).isEmpty());
    assertEquals(Optional.of(List.of(urgentId)), cache.findWaitingOrder(waitlist.id()));

    given().port(port)
        .when().post("/waitlists/{id}/serve-next", waitlist.id())
        .then().statusCode(200)
        .body("entryId", equalTo(urgentId))
        .body("status", equalTo("CALLED"));
    assertEquals(EntryStatus.CALLED,
        storage.findEntry(waitlist.id(), urgentId).orElseThrow().status());
    assertEquals(EntryStatus.CALLED,
        cache.findEntry(waitlist.id(), urgentId).orElseThrow().status());
    assertEquals(Optional.of(List.of()), cache.findWaitingOrder(waitlist.id()));
  }

  @Test
  void delayedOlderPatchWriteDoesNotOverwriteNewerCache() {
    String normalId = given().port(port).contentType("application/json")
        .body("{\"name\":\"Alice\",\"priority\":1}")
        .when().post("/waitlists/{id}/entries", waitlist.id())
        .then().statusCode(200)
        .extract().path("entryId");
    String urgentId = given().port(port).contentType("application/json")
        .body("{\"name\":\"Bob\",\"priority\":5}")
        .when().post("/waitlists/{id}/entries", waitlist.id())
        .then().statusCode(200)
        .extract().path("entryId");

    given().port(port).contentType("application/json").body("{\"priority\":4}")
        .when().patch("/waitlists/{id}/entries/{entryId}", waitlist.id(), urgentId)
        .then().statusCode(200)
        .body("priority", equalTo(4))
        .body("position", equalTo(1));
    Entry latest = storage.findEntry(waitlist.id(), urgentId).orElseThrow();
    Entry delayedOlderWrite = new Entry(latest.id(), latest.waitlistId(), latest.name(),
        2, latest.status(), latest.insertedAt(), latest.calledAt(),
        latest.updatedAt().minusMillis(1));

    consistency.replaceCacheAfterCommit(
        waitlist.id(), delayedOlderWrite, List.of(normalId, urgentId));

    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}", waitlist.id(), urgentId)
        .then().statusCode(200)
        .body("priority", equalTo(4))
        .body("position", equalTo(1));
    assertEquals(4, storage.findEntry(waitlist.id(), urgentId).orElseThrow().priority());
    assertEquals(4, cache.findEntry(waitlist.id(), urgentId).orElseThrow().priority());
    assertEquals(Optional.of(List.of(urgentId, normalId)), cache.findWaitingOrder(waitlist.id()));
  }

  @Test
  void missingRankAndEntryKeysAreRebuiltFromPostgresBeforeTheResponse() {
    Entry normal = addEntry("Alice", 1, T0);
    Entry urgent = addEntry("Bob", 5, T0.plusSeconds(1));
    cache.invalidateWaitlist(waitlist.id());
    assertTrue(cache.findWaitingOrder(waitlist.id()).isEmpty());
    assertTrue(cache.findEntry(waitlist.id(), normal.id()).isEmpty());

    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}", waitlist.id(), normal.id())
        .then().statusCode(200)
        .body("position", equalTo(2))
        .body("estimatedWaitTimeMinutes", equalTo(5));

    assertEquals(Optional.of(normal), cache.findEntry(waitlist.id(), normal.id()));
    assertEquals(Optional.of(List.of(urgent.id(), normal.id())),
        cache.findWaitingOrder(waitlist.id()));
  }

  @Test
  void overlappingServeNextClaimsTwoEntriesOrOneEntryAnd204() throws Exception {
    Entry first = addEntry("Alice", 5, T0);
    Entry second = addEntry("Bob", 1, T0.plusSeconds(1));
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> left = pool.submit(() -> serveNextAfter(start));
      Future<Integer> right = pool.submit(() -> serveNextAfter(start));
      start.countDown();
      Set<Integer> codes = new HashSet<>(List.of(left.get(5, TimeUnit.SECONDS),
          right.get(5, TimeUnit.SECONDS)));
      assertTrue(codes.equals(Set.of(200)) || codes.equals(Set.of(200, 204)));
    } finally {
      pool.shutdownNow();
    }

    List<Entry> called = storage.findEntriesByWaitlistId(waitlist.id()).stream()
        .filter(entry -> entry.status() == EntryStatus.CALLED)
        .toList();
    if (called.size() == 2) {
      assertNotEquals(called.get(0).id(), called.get(1).id());
      Set<String> claimed = Set.of(called.get(0).id(), called.get(1).id());
      assertEquals(Set.of(first.id(), second.id()), claimed);
    } else {
      assertEquals(1, called.size());
    }
    assertEquals(2 - called.size(), storage.countWaitingEntries(waitlist.id()));
  }

  private int serveNextAfter(CountDownLatch start) throws InterruptedException {
    assertTrue(start.await(5, TimeUnit.SECONDS));
    return given().port(port)
        .when().post("/waitlists/{id}/serve-next", waitlist.id())
        .then().extract().statusCode();
  }

  private Entry addEntry(String name, int priority, Instant insertedAt) {
    return storage.saveEntry(new Entry(
        UUID.randomUUID().toString(), waitlist.id(), name, priority,
        EntryStatus.WAITING, insertedAt));
  }
}
