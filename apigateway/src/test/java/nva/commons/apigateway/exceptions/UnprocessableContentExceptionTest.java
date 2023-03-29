package nva.commons.apigateway.exceptions;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

class UnprocessableContentExceptionTest {

    public static final int HTTP_UNPROCESSABLE_CONTENT = 422;

    @Test
    void statusCodeReturnsUnprocessableContent() {
        UnprocessableContentException exception = new UnprocessableContentException("some message");
        assertThat(exception.getStatusCode(), is(equalTo(HTTP_UNPROCESSABLE_CONTENT)));
    }
}