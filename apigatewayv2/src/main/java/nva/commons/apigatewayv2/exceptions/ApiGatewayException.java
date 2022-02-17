package nva.commons.apigatewayv2.exceptions;

import java.util.Objects;

public abstract class ApiGatewayException extends Exception {

    public static final String MISSING_STATUS_CODE = "Status code cannot be null for exception:";

    protected ApiGatewayException(String message) {
        super(message);
    }

    protected ApiGatewayException(Exception exception) {
        super(exception);
    }

    protected ApiGatewayException(Exception exception, String message) {
        super(message, exception);
    }

    /**
     * Get the status code that should be returned to the REST-client.
     *
     * @return the status code.
     */
    public final Integer getStatusCode() {
        if (Objects.isNull(statusCode())) {
            throw new IllegalStateException(MISSING_STATUS_CODE + this.getClass().getCanonicalName());
        }
        return statusCode();
    }

    protected abstract Integer statusCode();
}





