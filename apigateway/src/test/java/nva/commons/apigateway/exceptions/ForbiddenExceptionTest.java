package nva.commons.apigateway.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

public class ForbiddenExceptionTest {

    @Test
    public void forbiddenExceptionReturnsForbiddenStatusCode() {
        ForbiddenException exception = new ForbiddenException();
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_FORBIDDEN)));
    }
}