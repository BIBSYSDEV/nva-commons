package nva.commons.apigatewayv2.exceptions;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

class BadRequestExceptionTest {

    @Test
    public void statusCodeReturnsBadRequest() {
        BadRequestException exception = new BadRequestException("some message");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    public void shouldContainCause() {
        RuntimeException cause = new RuntimeException(randomString());
        BadRequestException exception = new BadRequestException("some message", cause);
        assertThat(exception.getCause(), is(equalTo(cause)));
    }
}