package no.unit.nva.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.Test;

class IdentityServiceExceptionTest {

  private static final URI REQUEST_URI_WITH_SENSITIVE_PARTS =
      URI.create(
          "https://user:secret@localhost:8443/customer/abc%2F123?email=someone@example.org#x");

  @Test
  void shouldLeaveUserInfoPortQueryAndFragmentOutOfMessage() {
    var exception =
        new IdentityServiceRequestFailedException(
            REQUEST_URI_WITH_SENSITIVE_PARTS, new IOException("Connection reset"));

    assertEquals(
        "Request to identity service failed: https://localhost/customer/abc%2F123",
        exception.getMessage());
  }

  @Test
  void shouldKeepFullRequestUri() {
    var exception = new IdentityServiceNotFoundException(REQUEST_URI_WITH_SENSITIVE_PARTS);

    assertEquals(REQUEST_URI_WITH_SENSITIVE_PARTS, exception.getRequestUri());
  }
}
