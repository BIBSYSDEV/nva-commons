package nva.commons.apigateway.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

class UnprocessableContentExceptionTest {

  private static final int HTTP_UNPROCESSABLE_CONTENT = 422;

  @Test
  void statusCodeReturnsUnprocessableContent() {
    UnprocessableContentException exception = new UnprocessableContentException("some message");
    assertThat(exception.getStatusCode(), is(equalTo(HTTP_UNPROCESSABLE_CONTENT)));
  }
}
