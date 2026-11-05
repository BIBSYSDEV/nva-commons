package nva.commons.apigateway.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.net.HttpURLConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiIoExceptionTest {

  @Test
  @DisplayName("statusCode returns Internal server error")
  void statusCodeReturnsInternalServerError() {
    assertThat(
        new ApiIoException(null, null).statusCode(),
        is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
  }
}
