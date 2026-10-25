package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.request.CreateWaitlistRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/**
 * Checks creation validation, persistence, and duplicate-name propagation.
 *
 * <p>AI-assisted.
 */
class CreateWaitlistServiceTest {

  private final WaitlistStorage storage = mock(WaitlistStorage.class);
  private final CreateWaitlistService service = new CreateWaitlistService(storage);

  @Test
  void createsWithUuidAndReturnsStorageResult() {
    Waitlist stored = new Waitlist("stored-id", "Stored settings", 20, 7);
    when(storage.saveWaitlist(any())).thenReturn(stored);

    Waitlist result = service.createWaitlist(new CreateWaitlistRequest("Support", 10, 5));

    ArgumentCaptor<Waitlist> captor = ArgumentCaptor.forClass(Waitlist.class);
    verify(storage).saveWaitlist(captor.capture());
    Waitlist created = captor.getValue();
    assertEquals(created.id(), UUID.fromString(created.id()).toString());
    assertEquals("Support", created.name());
    assertEquals(10, created.maxLength());
    assertEquals(5, created.processingTimePerEntryMinutes());
    assertSame(stored, result);
    verifyNoMoreInteractions(storage);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n"})
  void rejectsInvalidName(String name) {
    assertBadRequest(new CreateWaitlistRequest(name, 10, 5));
  }

  @ParameterizedTest
  @CsvSource({"0,5", "-1,5", "10,0", "10,-1"})
  void rejectsNonPositiveSettings(int maxLength, int processingTime) {
    assertBadRequest(new CreateWaitlistRequest("Support", maxLength, processingTime));
  }

  @Test
  void rejectsNullRequest() {
    assertBadRequest(null);
  }

  @Test
  void propagatesStorageConflict() {
    ApiException conflict = ApiException.conflict("Waitlist name already exists");
    when(storage.saveWaitlist(any())).thenThrow(conflict);

    ApiException thrown = assertThrows(ApiException.class,
        () -> service.createWaitlist(new CreateWaitlistRequest("Support", 10, 5)));

    assertSame(conflict, thrown);
    assertEquals(HttpStatus.CONFLICT, thrown.status());
    verify(storage).saveWaitlist(any());
    verifyNoMoreInteractions(storage);
  }

  private void assertBadRequest(CreateWaitlistRequest request) {
    ApiException exception = assertThrows(ApiException.class,
        () -> service.createWaitlist(request));
    assertEquals(HttpStatus.BAD_REQUEST, exception.status());
    assertEquals("INVALID_REQUEST", exception.errorCode());
    verifyNoInteractions(storage);
  }
}
