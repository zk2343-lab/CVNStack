package com.cvnstack.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.service.CacheStorage;
import com.cvnstack.service.WaitlistStorage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Checks PATCH HTTP behavior through the real controller and service with mocked storage.
 *
 * <p>AI-assisted.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PatchEntryControllerTest {

  @LocalServerPort
  private int port;
  @MockitoBean
  private WaitlistStorage storage;
  @MockitoBean
  private CacheStorage cache;

  private final Instant timestamp = Instant.parse("2020-01-01T00:00:00Z");
  private final Entry current = new Entry("e", "wl", "Alice", 1, EntryStatus.WAITING,
      timestamp, null, timestamp);

  @BeforeEach
  void setup() {
    when(storage.findWaitlistById("wl"))
        .thenReturn(Optional.of(new Waitlist("wl", "Support", 10, 5)));
    when(storage.findEntry("wl", "e")).thenReturn(Optional.of(current));
    when(storage.updateEntryIfUnchanged(any(), any())).thenAnswer(call -> {
      Entry desired = call.getArgument(0);
      return Optional.of(new Entry(desired.id(), desired.waitlistId(), desired.name(),
          desired.priority(), desired.status(), desired.insertedAt(), desired.calledAt(),
          timestamp.plusSeconds(20)));
    });
    when(storage.findEntriesByWaitlistId("wl")).thenReturn(List.of(current.withPriority(5)));
  }

  @AfterEach
  void noInsertOrCacheCoordination() {
    verify(storage, never()).saveEntry(any());
    verifyNoInteractions(cache);
  }

  @Test
  void priorityOnlyReturnsDerivedRankingAndStoredTimestamp() {
    given().port(port).contentType("application/json").body("{\"priority\":5}")
        .patch("/waitlists/wl/entries/e").then().statusCode(200)
        .body("priority", equalTo(5)).body("status", equalTo("WAITING"))
        .body("updatedAt", equalTo(timestamp.plusSeconds(20).toString()))
        .body("position", equalTo(1)).body("estimatedWaitTimeMinutes", equalTo(0))
        .body("size()", equalTo(7));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"status\":\"CALLED\"}",
      "{\"status\":\"CALLED\",\"priority\":5}"})
  void statusAndBothReturnNullWaitingInformation(String body) {
    given().port(port).contentType("application/json").body(body)
        .patch("/waitlists/wl/entries/e").then().statusCode(200)
        .body("status", equalTo("CALLED"))
        .body("position", nullValue()).body("estimatedWaitTimeMinutes", nullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "{}", "null", "{\"status\":null,\"priority\":null}",
      "{\"priority\":0}", "{\"priority\":6}", "{\"status\":\"UNKNOWN\"}"})
  void invalidRequestsReturn400WithoutMutation(String body) {
    given().port(port).contentType("application/json").body(body)
        .patch("/waitlists/wl/entries/e").then().statusCode(400);
    verify(storage, never()).updateEntryIfUnchanged(any(), any());
  }

  @Test
  void missingWaitlistReturns404() {
    when(storage.findWaitlistById("wl")).thenReturn(Optional.empty());
    patchPriority(404);
  }

  @Test
  void missingEntryReturns404() {
    when(storage.findEntry("wl", "e")).thenReturn(Optional.empty());
    patchPriority(404);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"status\":\"WAITING\"}", "{\"priority\":1}"})
  void repeatedValueReturns200WithoutWrite(String body) {
    given().port(port).contentType("application/json").body(body)
        .patch("/waitlists/wl/entries/e").then().statusCode(200)
        .body("updatedAt", equalTo(timestamp.toString()));
    verify(storage, never()).updateEntryIfUnchanged(any(), any());
  }

  @Test
  void terminalStateCannotBeLeftBySecondPatch() {
    when(storage.findEntry("wl", "e")).thenReturn(Optional.of(current),
        Optional.of(current.withStatus(EntryStatus.CANCELLED)));
    given().port(port).contentType("application/json").body("{\"status\":\"CANCELLED\"}")
        .patch("/waitlists/wl/entries/e").then().statusCode(200);
    given().port(port).contentType("application/json").body("{\"status\":\"WAITING\"}")
        .patch("/waitlists/wl/entries/e").then().statusCode(400)
        .body("error", equalTo("INVALID_STATE_TRANSITION"));
    verify(storage, times(1)).updateEntryIfUnchanged(any(), any());
  }

  private void patchPriority(int code) {
    given().port(port).contentType("application/json").body("{\"priority\":5}")
        .patch("/waitlists/wl/entries/e").then().statusCode(code);
    verify(storage, never()).updateEntryIfUnchanged(any(), any());
  }
}

