package com.cvnstack.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.service.CacheStorage;
import com.cvnstack.service.WaitlistStorage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
 * Checks GET and serve-next HTTP behavior with real PostgreSQL and Redis storage.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.autoconfigure.exclude=")
@Testcontainers
class ReadPathApiTest {

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
    // Both stores belong only to this test class's temporary containers.
    jdbc.execute("TRUNCATE entries, waitlists");
    try (RedisConnection connection = redis.getConnectionFactory().getConnection()) {
      connection.serverCommands().flushDb();
    }
    waitlist = storage.saveWaitlist(new Waitlist(
        UUID.randomUUID().toString(), "Support", 10, 5));
  }

  // getEntry test
  @Test
  void getWaitingEntries() {
    Entry normal = addEntry("Desktop", Entry.DEFAULT_PRIORITY, T0);
    Entry urgent = addEntry("ROBOT", 5, T0.plusSeconds(1));

    String firstResponse = given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}", waitlist.id(), normal.id())
        .then().statusCode(200)
        .body("waitlistId", equalTo(waitlist.id()))
        .body("entryId", equalTo(normal.id()))
        .body("status", equalTo("WAITING"))
        .body("priority", equalTo(Entry.DEFAULT_PRIORITY))
        .body("position", equalTo(2))
        .body("estimatedWaitTimeMinutes", equalTo(5))
        .extract().asString();

    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}", waitlist.id(), normal.id())
        .then().statusCode(200)
        .body(equalTo(firstResponse));

    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}", waitlist.id(), urgent.id())
        .then().statusCode(200)
        .body("entryId", equalTo(urgent.id()))
        .body("position", equalTo(1))
        .body("estimatedWaitTimeMinutes", equalTo(0));

    assertEquals(Optional.of(normal), storage.findEntry(waitlist.id(), normal.id()));
    assertEquals(Optional.of(urgent), storage.findEntry(waitlist.id(), urgent.id()));
  }

  @ParameterizedTest
  @EnumSource(value = EntryStatus.class, names = {"CALLED", "SERVED", "CANCELLED"})
  void getNonWaitingEntry(EntryStatus status) {
    Entry entry = storage.saveEntry(new Entry(
        UUID.randomUUID().toString(), waitlist.id(), "Desktop", 1, status,
        T0, T0.plusSeconds(60)));

    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}", waitlist.id(), entry.id())
        .then().statusCode(200)
        .body("status", equalTo(status.name()))
        .body("position", nullValue())
        .body("estimatedWaitTimeMinutes", nullValue());

    assertEquals(Optional.of(entry), storage.findEntry(waitlist.id(), entry.id()));
  }

  @Test
  void getMissingWaitlist() {
    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}",
            UUID.randomUUID().toString(), UUID.randomUUID().toString())
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"));
  }

  @Test
  void getMissingEntry() {
    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}",
            waitlist.id(), UUID.randomUUID().toString())
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"));
  }

  @Test
  void getEntryFromAnotherWaitlist() {
    Entry entry = addEntry("Desktop", 1, T0);
    Waitlist other = storage.saveWaitlist(new Waitlist(
        UUID.randomUUID().toString(), "Other", 10, 5));

    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}", other.id(), entry.id())
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"));

    assertEquals(Optional.of(entry), storage.findEntry(waitlist.id(), entry.id()));
  }

  @Test
  void getEntryWithInvalidIds() {
    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}",
            "invalid-id", UUID.randomUUID().toString())
        .then().statusCode(400)
        .body("error", equalTo("INVALID_REQUEST"));

    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}", waitlist.id(), "invalid-id")
        .then().statusCode(400)
        .body("error", equalTo("INVALID_REQUEST"));
  }

  // serveNext test
  @Test
  void serveNextUpdatesDatabaseAndInvalidatesCache() {
    Entry normal = addEntry("Desktop", 1, T0);
    Entry urgent = addEntry("ROBOT", 5, T0.plusSeconds(1));
    cache.putEntry(normal);
    cache.putEntry(urgent);
    cache.replaceWaitingOrder(waitlist.id(), List.of(urgent.id(), normal.id()));
    assertEquals(Optional.of(urgent), cache.findEntry(waitlist.id(), urgent.id()));
    assertEquals(Optional.of(List.of(urgent.id(), normal.id())),
        cache.findWaitingOrder(waitlist.id()));

    given().port(port)
        .when().post("/waitlists/{id}/serve-next", waitlist.id())
        .then().statusCode(200)
        .body("waitlistId", equalTo(waitlist.id()))
        .body("entryId", equalTo(urgent.id()))
        .body("status", equalTo("CALLED"))
        .body("priority", equalTo(5));

    Entry called = storage.findEntry(waitlist.id(), urgent.id()).orElseThrow();
    assertEquals(EntryStatus.CALLED, called.status());
    assertNotNull(called.calledAt());
    assertNotNull(called.updatedAt());
    assertEquals(urgent.insertedAt(), called.insertedAt());
    assertFalse(called.updatedAt().isBefore(urgent.updatedAt()));
    assertTrue(cache.findWaitingOrder(waitlist.id()).isEmpty());
    assertTrue(cache.findEntry(waitlist.id(), urgent.id()).isEmpty());
    assertTrue(cache.findEntry(waitlist.id(), normal.id()).isEmpty());

    given().port(port)
        .when().get("/waitlists/{id}/entries/{entryId}", waitlist.id(), normal.id())
        .then().statusCode(200)
        .body("position", equalTo(1))
        .body("estimatedWaitTimeMinutes", equalTo(0));
  }

  @Test
  void serveNextUsesInsertionOrderForEqualPriority() {
    Entry late = addEntry("Late", 3, T0.plusSeconds(1));
    Entry early = addEntry("Early", 3, T0);

    given().port(port)
        .when().post("/waitlists/{id}/serve-next", waitlist.id())
        .then().statusCode(200)
        .body("entryId", equalTo(early.id()));

    given().port(port)
        .when().post("/waitlists/{id}/serve-next", waitlist.id())
        .then().statusCode(200)
        .body("entryId", equalTo(late.id()));

    given().port(port)
        .when().post("/waitlists/{id}/serve-next", waitlist.id())
        .then().statusCode(204)
        .body(equalTo(""));

    assertEquals(EntryStatus.CALLED,
        storage.findEntry(waitlist.id(), early.id()).orElseThrow().status());
    assertEquals(EntryStatus.CALLED,
        storage.findEntry(waitlist.id(), late.id()).orElseThrow().status());
    assertEquals(0, storage.countWaitingEntries(waitlist.id()));
  }

  @Test
  void serveNextOnEmptyWaitlist() {
    cache.replaceWaitingOrder(waitlist.id(), List.of());

    given().port(port)
        .when().post("/waitlists/{id}/serve-next", waitlist.id())
        .then().statusCode(204)
        .body(equalTo(""));

    assertTrue(storage.findEntriesByWaitlistId(waitlist.id()).isEmpty());
    assertEquals(Optional.of(List.of()), cache.findWaitingOrder(waitlist.id()));
  }

  @Test
  void serveNextOnMissingWaitlist() {
    given().port(port)
        .when().post("/waitlists/{id}/serve-next", UUID.randomUUID().toString())
        .then().statusCode(404)
        .body("error", equalTo("NOT_FOUND"));
  }

  @Test
  void serveNextWithInvalidId() {
    given().port(port)
        .when().post("/waitlists/{id}/serve-next", "invalid-id")
        .then().statusCode(400)
        .body("error", equalTo("INVALID_REQUEST"));
  }

  private Entry addEntry(String name, int priority, Instant insertedAt) {
    return storage.saveEntry(new Entry(
        UUID.randomUUID().toString(), waitlist.id(), name, priority,
        EntryStatus.WAITING, insertedAt));
  }
}
