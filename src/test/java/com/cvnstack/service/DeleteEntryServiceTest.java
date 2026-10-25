package com.cvnstack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.cvnstack.error.ApiException;
import com.cvnstack.error.ErrorCode;
import com.cvnstack.model.Waitlist;
import com.cvnstack.model.response.DeleteEntryResponse;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Checks entry removal and repeated deletes using a mocked storage boundary.
 *
 * <p>AI-assisted.
 */
@ExtendWith(MockitoExtension.class)
class DeleteEntryServiceTest {

  @Mock
  private WaitlistStorage storage;

  private DeleteEntryService service;
  private InOrder calls;

  @BeforeEach
  void setUp() {
    service = new DeleteEntryService(storage);
    calls = inOrder(storage);
  }

  @AfterEach
  void usesOnlyWaitlistLookupAndDirectDelete() {
    verifyNoMoreInteractions(storage);
  }

  @Test
  void successfulDeleteReturnsConfirmation() {
    existingWaitlist();
    when(storage.deleteEntry("wl-1", "e-1")).thenReturn(true);

    assertEquals(new DeleteEntryResponse("Entry successfully removed", "e-1", "wl-1"),
        service.deleteEntry("wl-1", "e-1"));

    verifyDeleteCalls();
  }

  @Test
  void missingWaitlistReturns404WithoutDelete() {
    when(storage.findWaitlistById("wl-1")).thenReturn(Optional.empty());

    assertNotFound("Waitlist does not exist");

    calls.verify(storage).findWaitlistById("wl-1");
  }

  @Test
  void missingEntryReturns404() {
    existingWaitlist();
    when(storage.deleteEntry("wl-1", "e-1")).thenReturn(false);

    assertNotFound("Entry does not exist");

    verifyDeleteCalls();
  }

  @Test
  void secondDeleteReturns404() {
    existingWaitlist();
    when(storage.deleteEntry("wl-1", "e-1")).thenReturn(true, false);

    assertEquals("e-1", service.deleteEntry("wl-1", "e-1").entryId());
    assertNotFound("Entry does not exist");

    verifyDeleteCalls();
    verifyDeleteCalls();
  }

  @Test
  void storageFailurePropagates() {
    existingWaitlist();
    RuntimeException failure = new RuntimeException("Database unavailable");
    when(storage.deleteEntry("wl-1", "e-1")).thenThrow(failure);

    assertSame(failure, assertThrows(RuntimeException.class,
        () -> service.deleteEntry("wl-1", "e-1")));

    verifyDeleteCalls();
  }

  private void existingWaitlist() {
    when(storage.findWaitlistById("wl-1"))
        .thenReturn(Optional.of(new Waitlist("wl-1", "Support", 10, 5)));
  }

  private void assertNotFound(String message) {
    ApiException exception = assertThrows(ApiException.class,
        () -> service.deleteEntry("wl-1", "e-1"));
    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals(ErrorCode.NOT_FOUND, exception.errorCode());
    assertEquals(message, exception.getMessage());
  }

  private void verifyDeleteCalls() {
    calls.verify(storage).findWaitlistById("wl-1");
    calls.verify(storage).deleteEntry("wl-1", "e-1");
  }
}
