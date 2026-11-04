package nva.commons.apigateway.exceptions;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

class PreconditionFailedExceptionTest {

  @Test
  void preconditionFailedExceptionShowsSuppliedMessage() {
    var message = randomString();
    var exception = new PreconditionFailedException(message);
    assertEquals(message, exception.getMessage());
  }

  @Test
  void preconditionFailedExceptionShowsDefaultMessage() {
    var exception = new PreconditionFailedException();
    assertEquals("Precondition Failed", exception.getMessage());
  }

  @Test
  void preconditionFailedExceptionHasStatusCode412() {
    var exception = new PreconditionFailedException();
    assertEquals(HttpURLConnection.HTTP_PRECON_FAILED, exception.getStatusCode());
  }
}
