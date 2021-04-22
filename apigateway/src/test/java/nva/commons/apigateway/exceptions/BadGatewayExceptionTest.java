package nva.commons.apigateway.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

class BadGatewayExceptionTest {

    @Test
    public void statusCodeReturnsBadGateway() {
        BadGatewayException exception = new BadGatewayException("some message");
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
    }
}