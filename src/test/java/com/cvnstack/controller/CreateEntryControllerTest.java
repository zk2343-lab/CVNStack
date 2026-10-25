package com.cvnstack.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.service.CacheStorage;
import com.cvnstack.service.WaitlistStorage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Checks entry POST requests through the real controller and service with mocked storage ports.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateEntryControllerTest {

  @LocalServerPort
  private int port;

  @MockitoBean
  private WaitlistStorage storage;

  @MockitoBean
  private CacheStorage cache;

  @AfterEach
  void neverUsesUnconditionalSave() {
    verify(storage, never()).saveEntry(any());
  }

  private void existingWaitlist() {
    when(storage.findWaitlistById("wl-1"))
        .thenReturn(Optional.of(new Waitlist("wl-1", "Support", 10, 5)));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {"{\"name\":\"Alice\",\"priority\":3}|3",
      "{\"name\":\"Alice\",\"priority\":4}|4", "{\"name\":\"Alice\"}|1",
      "{\"name\":\"Alice\",\"priority\":\"4\"}|4"})
  void createsEntryWithGeneratedIdAndWaitingInformation(String body, int priority) {
    existingWaitlist();
    AtomicReference<Entry> saved = new AtomicReference<>();
    when(storage.insertEntryIfCapacityAvailable(any())).thenAnswer(call -> {
      Entry entry = call.getArgument(0);
      saved.set(entry);
      return entry;
    });
    Entry older = new Entry("older", "wl-1", "Bob", 5, EntryStatus.WAITING, Instant.EPOCH);
    when(storage.findEntriesByWaitlistId("wl-1"))
        .thenAnswer(call -> List.of(saved.get(), older));
    String id = given().port(port).contentType("application/json").body(body)
        .when().post("/waitlists/wl-1/entries")
        .then().statusCode(200)
        .body("waitlistId", equalTo("wl-1"))
        .body("status", equalTo("WAITING"))
        .body("priority", equalTo(priority))
        .body("position", equalTo(2))
        .body("estimatedWaitTimeMinutes", equalTo(5))
        .body("size()", equalTo(6))
        .extract().path("entryId");
    assertNotNull(UUID.fromString(id));
    assertEquals(saved.get().id(), id);
    verify(storage).insertEntryIfCapacityAvailable(any());
    verify(cache).putEntry(saved.get());
    verify(cache).replaceWaitingOrder("wl-1", List.of("older", id));
  }

  @ParameterizedTest
  @ValueSource(strings = {"4.5", "5.9", "4.0", "4e0"})
  void floatingPointPriorityReturns400BeforePersistence(String priority) {
    given().port(port).contentType("application/json")
        .body("{\"name\":\"Alice\",\"priority\":" + priority + "}")
        .when().post("/waitlists/wl-1/entries")
        .then().statusCode(400);
    verifyNoInteractions(storage, cache);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\" \"}",
      "{\"name\":\"Alice\",\"priority\":0}", "{\"name\":\"Alice\",\"priority\":6}"})
  void invalidFieldsReturn400WithoutStorageAccess(String body) {
    given().port(port).contentType("application/json").body(body)
        .when().post("/waitlists/wl-1/entries")
        .then().statusCode(400).body("error", equalTo("INVALID_REQUEST"));
    verifyNoInteractions(storage, cache);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{", "null", "", "[]",
      "{\"name\":\"Alice\",\"priority\":\"invalid\"}",
      "{\"name\":{},\"priority\":1}", "{\"name\":\"Alice\",\"priority\":{}}"})
  void unreadableBodyReturns400WithoutStorageAccess(String body) {
    given().port(port).contentType("application/json").body(body)
        .when().post("/waitlists/wl-1/entries")
        .then().statusCode(400);
    verifyNoInteractions(storage, cache);
  }

  @Test
  void unknownWaitlistReturns404WithoutInsert() {
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.empty());
    given().port(port).contentType("application/json").body("{\"name\":\"Alice\"}")
        .when().post("/waitlists/wl-1/entries")
        .then().statusCode(404).body("error", equalTo("NOT_FOUND"));
    verify(storage, never()).insertEntryIfCapacityAvailable(any());
    verifyNoInteractions(cache);
  }

  @Test
  void fullWaitlistReturns422() {
    existingWaitlist();
    when(storage.insertEntryIfCapacityAvailable(any()))
        .thenThrow(ApiException.capacityExceeded("Waitlist is full"));
    given().port(port).contentType("application/json").body("{\"name\":\"Alice\"}")
        .when().post("/waitlists/wl-1/entries")
        .then().statusCode(422).body("error", equalTo("CAPACITY_EXCEEDED"));
    verifyNoInteractions(cache);
  }
}
