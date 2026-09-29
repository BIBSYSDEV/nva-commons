package nva.commons.apigateway.exceptions;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

class UnsupportedMediaTypeExceptionTest {

  @Test
  void shouldReturn415StatusCodeOnUnsupportedMediaTypeException() {
    var exception = new UnsupportedMediaTypeException(randomString());
    assertEquals(HttpURLConnection.HTTP_UNSUPPORTED_TYPE, exception.statusCode());
  }
}
