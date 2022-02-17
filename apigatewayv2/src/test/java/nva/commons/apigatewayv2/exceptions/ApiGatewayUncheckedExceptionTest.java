package nva.commons.apigatewayv2.exceptions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ApiGatewayUncheckedExceptionTest {

    @DisplayName("ApiGatewayUncheckedException return Internal Server Error")
    @Test
    public void apiGatewayUncheckedExceptionReturnsInternalServerError() {
        ApiGatewayException cause = new TestException("some message");
        ApiGatewayUncheckedException exception = new ApiGatewayUncheckedException(cause);
        assertThat(exception.getStatusCode(), is(equalTo(cause.getStatusCode())));
    }
}