package nva.commons.apigateway.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import java.util.List;
import org.junit.jupiter.api.Test;

class BadRequestExceptionTest {

    @Test
    public void statusCodeReturnsBadRequest() {
        BadRequestException exception = new BadRequestException("some message");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
    }

    @Test
    void errorsAreEmptyByDefault() {
        BadRequestException exception = new BadRequestException("some message");
        assertThat(exception.getErrors(), is(empty()));
    }

    @Test
    void errorsAreEmptyWhenConstructedWithCause() {
        BadRequestException exception = new BadRequestException("some message", new RuntimeException("cause"));
        assertThat(exception.getErrors(), is(empty()));
    }

    @Test
    void errorsAreReturnedWhenSupplied() {
        ValidationError validationError = new ValidationError("must not be blank", "#/title");
        BadRequestException exception = new BadRequestException("some message", List.of(validationError));
        assertThat(exception.getErrors(), contains(validationError));
    }
}
