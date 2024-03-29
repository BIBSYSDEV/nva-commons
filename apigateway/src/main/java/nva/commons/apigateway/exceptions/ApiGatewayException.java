package nva.commons.apigateway.exceptions;

import java.util.Objects;

public abstract class ApiGatewayException extends Exception {

    public static final String MISSING_STATUS_CODE = "Status code cannot be null for exception:";
    private Integer runtimeStatusCode;
    private Object instance;

    public ApiGatewayException(String message) {
        super(message);
    }

    public ApiGatewayException(Exception exception) {
        super(exception);
    }

    public ApiGatewayException(Exception exception, Integer statusCode) {
        super(exception);
        this.runtimeStatusCode = statusCode;
    }

    public ApiGatewayException(Exception exception, String message) {
        super(message, exception);
    }

    public ApiGatewayException(String message, Object instance) {
        super(message);
        this.instance = instance;
    }

    public Object getInstance() {
        return instance;
    }

    protected abstract Integer statusCode();

    /**
     * Get the status code that should be returned to the REST-client.
     *
     * @return the status code.
     */
    public final Integer getStatusCode() {
        if (Objects.nonNull(runtimeStatusCode)) {
            return runtimeStatusCode;
        }
        if (Objects.isNull(statusCode())) {
            throw new IllegalStateException(MISSING_STATUS_CODE + this.getClass().getCanonicalName());
        }
        return statusCode();
    }
}





