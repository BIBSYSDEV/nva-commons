package nva.commons.apigateway.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

class UnauthorizedExceptionTest {

    @Test
    void statusCodeReturnsBadRequest() {
        UnauthorizedException exception = new UnauthorizedException();
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_UNAUTHORIZED)));
    }
}