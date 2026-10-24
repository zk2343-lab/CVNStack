package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cvnstack.error.ApiException;
import com.cvnstack.error.ErrorCode;
import com.cvnstack.error.GlobalExceptionHandler;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies Flyway schema and storage semantics against a real PostgreSQL database.
 *
 * <p>AI-assisted.
 */
@JdbcTest(properties = "spring.autoconfigure.exclude=")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresWaitlistStorage.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
class PostgresWaitlistStorageTest {

  private static final Instant INSERTED = Instant.parse("2020-01-01T00:00:00.123456Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired
  private PostgresWaitlistStorage storage;
  @Autowired
  private JdbcTemplate jdbc;
  @Autowired
  private Flyway flyway;

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void cleanTables() {
    jdbc.execute("TRUNCATE entries, waitlists");
  }

  @Test
  void malformedWaitlistIdsAreBadRequestsAcrossStoragePaths() {
    String entryId = UUID.randomUUID().toString();
    Entry entry = entry("abc", EntryStatus.WAITING);
    List<Runnable> operations = List.of(
        () -> storage.findWaitlistById("abc"),
        () -> storage.findEntry("abc", entryId),
        () -> storage.findEntriesByWaitlistId("abc"),
        () -> storage.deleteEntry("abc", entryId),
        () -> storage.callEntryIfWaiting("abc", entryId, INSERTED),
        () -> storage.countWaitingEntries("abc"),
        () -> storage.saveWaitlist(new Waitlist("abc", "Malformed", 2, 5)),
        () -> storage.saveEntry(entry),
        () -> storage.insertEntryIfCapacityAvailable(entry));
    for (Runnable operation : operations) {
      ApiException failure = assertThrows(ApiException.class, operation::run);
      assertEquals(HttpStatus.BAD_REQUEST, failure.status());
      assertEquals(ErrorCode.INVALID_REQUEST, failure.errorCode());
      assertEquals("Invalid waitlist id", failure.getMessage());
      assertEquals(HttpStatus.BAD_REQUEST,
          new GlobalExceptionHandler().handleApiException(failure).getStatusCode());
    }
  }

  @Test
  void malformedEntryIdsAreBadRequestsAcrossStoragePaths() {
    Waitlist waitlist = storage.saveWaitlist(waitlist(2));
    Entry entry = new Entry("abc", waitlist.id(), "Person", 2, EntryStatus.WAITING, INSERTED);
    List<Runnable> operations = List.of(
        () -> storage.findEntry(waitlist.id(), "abc"),
        () -> storage.deleteEntry(waitlist.id(), "abc"),
        () -> storage.callEntryIfWaiting(waitlist.id(), "abc", INSERTED),
        () -> storage.saveEntry(entry),
        () -> storage.insertEntryIfCapacityAvailable(entry));
    for (Runnable operation : operations) {
      ApiException failure = assertThrows(ApiException.class, operation::run);
      assertEquals(HttpStatus.BAD_REQUEST, failure.status());
      assertEquals(ErrorCode.INVALID_REQUEST, failure.errorCode());
      assertEquals("Invalid entry id", failure.getMessage());
    }
    assertTrue(storage.findEntriesByWaitlistId(waitlist.id()).isEmpty());
  }

  @Test
  void validMissingIdsRemainEmptyAndServicesReturnNotFound() {
    String missingId = UUID.randomUUID().toString();
    assertTrue(storage.findWaitlistById(missingId).isEmpty());
    assertTrue(storage.findEntry(missingId, missingId).isEmpty());
    assertTrue(storage.callEntryIfWaiting(missingId, missingId, INSERTED).isEmpty());
    assertFalse(storage.deleteEntry(missingId, missingId));
    EntryQueryService service = new EntryQueryService(storage, new EntryRulesService());
    ApiException missingWaitlist = assertThrows(ApiException.class,
        () -> service.getEntry(missingId, missingId));
    assertEquals(HttpStatus.NOT_FOUND, missingWaitlist.status());
    Waitlist waitlist = storage.saveWaitlist(waitlist(2));
    ApiException missingEntry = assertThrows(ApiException.class,
        () -> service.getEntry(waitlist.id(), missingId));
    assertEquals(HttpStatus.NOT_FOUND, missingEntry.status());
  }

  @Test
  void migrationRunsOnceAndWaitlistUpsertsById() {
    Waitlist waitlist = waitlist(3);
    assertEquals(waitlist, storage.saveWaitlist(waitlist));
    assertEquals(Optional.of(waitlist), storage.findWaitlistById(waitlist.id()));
    assertEquals(Optional.of(waitlist), storage.findWaitlistByName(waitlist.name()));
    Waitlist changed = new Waitlist(waitlist.id(), "Renamed", 4, 7);
    assertEquals(changed, storage.saveWaitlist(changed));
    assertEquals(Optional.of(changed), storage.findWaitlistById(waitlist.id()));
    assertTrue(storage.findWaitlistByName(waitlist.name()).isEmpty());
    assertEquals(0, flyway.migrate().migrationsExecuted);
    assertEquals(1, flyway.info().applied().length);
  }

  @Test
  void duplicateNameIsConflictOnInsertAndUpdate() {
    Waitlist first = storage.saveWaitlist(waitlist(3));
    Waitlist second = storage.saveWaitlist(waitlist(3));
    for (String id : List.of(UUID.randomUUID().toString(), second.id())) {
      ApiException failure = assertThrows(ApiException.class,
          () -> storage.saveWaitlist(new Waitlist(id, first.name(), 3, 5)));
      assertEquals(HttpStatus.CONFLICT, failure.status());
    }
    assertEquals(Optional.of(second), storage.findWaitlistById(second.id()));
  }

  @Test
  void entryRoundTripUsesDatabaseWriteTimeAndInsertIsNotAnUpsert() {
    Waitlist waitlist = storage.saveWaitlist(waitlist(3));
    Entry input = new Entry(UUID.randomUUID().toString(), waitlist.id(), "Person", 2,
        EntryStatus.WAITING, INSERTED, null, INSERTED);
    Entry stored = storage.saveEntry(input);
    assertNotNull(stored.updatedAt());
    assertTrue(stored.updatedAt().isAfter(INSERTED));
    assertEquals(INSERTED, stored.insertedAt());
    assertEquals(Optional.of(stored), storage.findEntry(waitlist.id(), stored.id()));
    assertThrows(DataIntegrityViolationException.class,
        () -> storage.saveEntry(input.withPriority(5)));
    assertEquals(Optional.of(stored), storage.findEntry(waitlist.id(), stored.id()));
    Entry sameName = storage.saveEntry(entry(waitlist.id(), EntryStatus.WAITING));
    assertEquals(2, storage.findEntriesByWaitlistId(waitlist.id()).size());
    assertEquals(stored.name(), sameName.name());
  }

  @Test
  void conditionalCallPreservesInsertionTimeAndRetainsCalledEntry() {
    Waitlist waitlist = storage.saveWaitlist(waitlist(3));
    Entry stored = storage.saveEntry(entry(waitlist.id(), EntryStatus.WAITING));
    Instant calledAt = INSERTED.plusSeconds(60);
    assertTrue(storage.callEntryIfWaiting(UUID.randomUUID().toString(), stored.id(), calledAt)
        .isEmpty());
    Entry called = storage.callEntryIfWaiting(waitlist.id(), stored.id(), calledAt).orElseThrow();
    assertEquals(EntryStatus.CALLED, called.status());
    assertEquals(calledAt, called.calledAt());
    assertEquals(INSERTED, called.insertedAt());
    assertFalse(called.updatedAt().isBefore(stored.updatedAt()));
    assertEquals(Optional.of(called), storage.findEntry(waitlist.id(), stored.id()));
    assertTrue(storage.callEntryIfWaiting(waitlist.id(), stored.id(), calledAt.plusSeconds(1))
        .isEmpty());
    assertEquals(0, storage.countWaitingEntries(waitlist.id()));
  }

  @Test
  void competingPatchesCannotBothOverwriteTheSameObservedRow() throws Exception {
    Waitlist waitlist = storage.saveWaitlist(waitlist(3));
    Entry original = storage.saveEntry(entry(waitlist.id(), EntryStatus.WAITING));
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> {
        assertTrue(start.await(10, TimeUnit.SECONDS));
        return storage.updateEntryIfUnchanged(original.withPriority(4), original);
      });
      var second = executor.submit(() -> {
        assertTrue(start.await(10, TimeUnit.SECONDS));
        return storage.updateEntryIfUnchanged(original.withPriority(5), original);
      });
      start.countDown();
      Optional<Entry> a = first.get(10, TimeUnit.SECONDS);
      Optional<Entry> b = second.get(10, TimeUnit.SECONDS);
      assertTrue(a.isPresent() ^ b.isPresent());
      assertEquals(a.isPresent() ? a : b, storage.findEntry(waitlist.id(), original.id()));
    }
  }

  @Test
  void conditionalPatchUsesDatabaseTimeAndRejectsStaleStatusAndPriority() {
    Waitlist waitlist = storage.saveWaitlist(waitlist(3));
    Entry original = storage.saveEntry(entry(waitlist.id(), EntryStatus.WAITING));
    Entry reordered = storage.updateEntryIfUnchanged(original.withPriority(5), original)
        .orElseThrow();
    assertEquals(5, reordered.priority());
    assertEquals(original.insertedAt(), reordered.insertedAt());
    assertFalse(reordered.updatedAt().isBefore(original.updatedAt()));
    assertEquals(Optional.of(reordered), storage.findEntry(waitlist.id(), original.id()));
    assertTrue(storage.updateEntryIfUnchanged(original.withStatus(EntryStatus.CANCELLED),
        original).isEmpty());
    Entry called = storage.callEntryIfWaiting(waitlist.id(), original.id(), INSERTED)
        .orElseThrow();
    assertTrue(storage.updateEntryIfUnchanged(reordered.withPriority(3), reordered).isEmpty());
    Entry served = storage.updateEntryIfUnchanged(called.withStatus(EntryStatus.SERVED), called)
        .orElseThrow();
    assertEquals(INSERTED, served.calledAt());
    assertEquals(Optional.of(served), storage.findEntry(waitlist.id(), original.id()));
    assertTrue(storage.deleteEntry(waitlist.id(), original.id()));
    assertTrue(storage.updateEntryIfUnchanged(served, served).isEmpty());
  }

  @Test
  void deleteAndFindAreScopedToWaitlist() {
    Waitlist waitlist = storage.saveWaitlist(waitlist(3));
    Entry stored = storage.saveEntry(entry(waitlist.id(), EntryStatus.WAITING));
    String otherId = UUID.randomUUID().toString();
    assertTrue(storage.findEntry(otherId, stored.id()).isEmpty());
    assertFalse(storage.deleteEntry(otherId, stored.id()));
    assertTrue(storage.deleteEntry(waitlist.id(), stored.id()));
    assertFalse(storage.deleteEntry(waitlist.id(), stored.id()));
  }

  @Test
  void databaseRejectsInvalidPriorityAndMissingParent() {
    Waitlist waitlist = storage.saveWaitlist(waitlist(3));
    Entry stored = storage.saveEntry(entry(waitlist.id(), EntryStatus.WAITING));
    for (int priority : List.of(0, 6)) {
      assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
          "UPDATE entries SET priority = ? WHERE id = ?", priority, UUID.fromString(stored.id())));
    }
    assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
        "UPDATE entries SET waitlist_id = ? WHERE id = ?",
        UUID.randomUUID(), UUID.fromString(stored.id())));
    ApiException missing = assertThrows(ApiException.class,
        () -> storage.saveEntry(entry(UUID.randomUUID().toString(), EntryStatus.WAITING)));
    assertEquals(HttpStatus.NOT_FOUND, missing.status());
    assertThrows(DataIntegrityViolationException.class,
        () -> storage.saveWaitlist(new Waitlist(UUID.randomUUID().toString(), "Zero", 2, 0)));
  }

  @Test
  void capacityIncludesCalledButExcludesTerminalEntries() {
    Waitlist waitlist = storage.saveWaitlist(waitlist(2));
    storage.saveEntry(entry(waitlist.id(), EntryStatus.CALLED));
    storage.saveEntry(entry(waitlist.id(), EntryStatus.SERVED));
    storage.saveEntry(entry(waitlist.id(), EntryStatus.CANCELLED));
    Entry inserted = storage.insertEntryIfCapacityAvailable(
        entry(waitlist.id(), EntryStatus.WAITING));
    assertNotNull(inserted.updatedAt());
    ApiException full = assertThrows(ApiException.class, () ->
        storage.insertEntryIfCapacityAvailable(entry(waitlist.id(), EntryStatus.WAITING)));
    assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, full.status());
    assertEquals(4, storage.findEntriesByWaitlistId(waitlist.id()).size());
    ApiException missing = assertThrows(ApiException.class, () ->
        storage.insertEntryIfCapacityAvailable(entry(UUID.randomUUID().toString(),
            EntryStatus.WAITING)));
    assertEquals(HttpStatus.NOT_FOUND, missing.status());
  }

  @Test
  void competingInsertsCannotExceedCapacity() throws Exception {
    Waitlist waitlist = storage.saveWaitlist(waitlist(1));
    CountDownLatch start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var first = workers.submit(() -> tryInsert(waitlist.id(), start));
      var second = workers.submit(() -> tryInsert(waitlist.id(), start));
      start.countDown();
      int accepted = (first.get(15, TimeUnit.SECONDS) ? 1 : 0)
          + (second.get(15, TimeUnit.SECONDS) ? 1 : 0);
      assertEquals(1, accepted);
    }
    assertEquals(1, storage.findEntriesByWaitlistId(waitlist.id()).size());
  }

  private boolean tryInsert(String waitlistId, CountDownLatch start) throws InterruptedException {
    assertTrue(start.await(5, TimeUnit.SECONDS));
    try {
      storage.insertEntryIfCapacityAvailable(entry(waitlistId, EntryStatus.WAITING));
      return true;
    } catch (ApiException failure) {
      assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, failure.status());
      return false;
    }
  }

  private static Waitlist waitlist(int capacity) {
    return new Waitlist(UUID.randomUUID().toString(), "List " + UUID.randomUUID(), capacity, 5);
  }

  private static Entry entry(String waitlistId, EntryStatus status) {
    return new Entry(UUID.randomUUID().toString(), waitlistId, "Person", 2, status, INSERTED);
  }
}
