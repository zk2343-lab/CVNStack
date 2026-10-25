package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.error.ErrorCode;
import com.cvnstack.model.Entry;
import com.cvnstack.model.EntryStatus;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.response.EntryResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Checks entry lookups and read-only behavior using a mock repository.
 *
 * <p>AI-assisted.
 */
@ExtendWith(MockitoExtension.class)
class EntryQueryServiceTest {

  private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
  private static final Waitlist WAITLIST = new Waitlist("wl-1", "Support", 10, 5);

  @Mock
  private WaitlistStorage storage;

  @Mock
  private CacheStorage cache;

  private EntryQueryService service;

  @BeforeEach
  void setUp() {
    service = new EntryQueryService(
        storage, new CacheConsistencyService(storage, cache, new EntryRulesService()));
  }

  @AfterEach
  void doesNotChangeStoredData() {
    verify(storage, never()).saveEntry(any(Entry.class));
    verify(storage, never()).saveWaitlist(any(Waitlist.class));
    verify(storage, never()).deleteEntry(anyString(), anyString());
  }

  @Test
  void waitingEntryReturnsPositionAndEstimate() {
    Entry target = entry("e1", "wl-1", 1, EntryStatus.WAITING);
    Entry urgent = entry("e2", "wl-1", 5, EntryStatus.WAITING);
    Entry called = entry("e3", "wl-1", 5, EntryStatus.CALLED);
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntry("wl-1", "e1")).thenReturn(Optional.of(target));
    when(storage.findEntriesByWaitlistId("wl-1")).thenReturn(List.of(target, called, urgent));

    EntryResponse response = service.getEntry("wl-1", "e1");

    assertEquals("wl-1", response.waitlistId());
    assertEquals("e1", response.entryId());
    assertEquals(EntryStatus.WAITING, response.status());
    assertEquals(1, response.priority());
    assertEquals(2, response.position());
    assertEquals(5, response.estimatedWaitTimeMinutes());
    verify(cache).putEntry(target);
    verify(cache).replaceWaitingOrder("wl-1", List.of("e2", "e1"));
  }

  @ParameterizedTest
  @EnumSource(value = EntryStatus.class, names = {"CALLED", "SERVED", "CANCELLED"})
  void nonWaitingEntryReturnsNullPositionAndEstimate(EntryStatus status) {
    Entry target = entry("e1", "wl-1", 1, status);
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntry("wl-1", "e1")).thenReturn(Optional.of(target));

    EntryResponse response = service.getEntry("wl-1", "e1");

    assertEquals(status, response.status());
    assertNull(response.position());
    assertNull(response.estimatedWaitTimeMinutes());
    verify(storage, never()).findEntriesByWaitlistId(anyString());
  }

  @Test
  void missingWaitlistReturnsNotFound() {
    when(storage.findWaitlistById("missing")).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class,
        () -> service.getEntry("missing", "e1"));

    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals(ErrorCode.NOT_FOUND, exception.errorCode());
    verify(storage, never()).findEntry(anyString(), anyString());
  }

  @Test
  void missingEntryReturnsNotFound() {
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntry("wl-1", "missing")).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class,
        () -> service.getEntry("wl-1", "missing"));

    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals(ErrorCode.NOT_FOUND, exception.errorCode());
  }

  @Test
  void entryFromAnotherWaitlistReturnsNotFound() {
    Entry target = entry("e1", "wl-2", 1, EntryStatus.WAITING);
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.of(WAITLIST));
    when(storage.findEntry("wl-1", "e1")).thenReturn(Optional.of(target));

    ApiException exception = assertThrows(ApiException.class,
        () -> service.getEntry("wl-1", "e1"));

    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals(ErrorCode.NOT_FOUND, exception.errorCode());
    verify(storage, never()).findEntriesByWaitlistId(anyString());
  }

  private static Entry entry(String id, String waitlistId, int priority, EntryStatus status) {
    return new Entry(id, waitlistId, id, priority, status, T0);
  }
}
