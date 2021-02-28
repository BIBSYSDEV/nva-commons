package nva.commons.apigateway.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class GatewayResponseSerializingExceptionTest {

    private static final String MESSAGE = "Some message";

    @DisplayName("Exception contains cause")
    @Test
    public void exceptionContainsCause() {

        Exception cause = new Exception(MESSAGE);
        GatewayResponseSerializingException exception = new GatewayResponseSerializingException(cause);
        assertThat(exception.getMessage(), containsString(GatewayResponseSerializingException.ERROR_MESSAGE));
    }

    @DisplayName("Exception returns Internal Server Error")
    @Test
    public void exceptionReturnsInternalServerError() {
        Exception cause = new Exception(MESSAGE);
        GatewayResponseSerializingException exception = new GatewayResponseSerializingException(cause);
        assertThat(exception.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_INTERNAL_ERROR)));
    }
}