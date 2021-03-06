package nva.commons.apigateway.exceptions;

public class ApiGatewayUncheckedException extends RuntimeException {

    private final ApiGatewayException exception;

    public ApiGatewayUncheckedException(ApiGatewayException exception) {
        super(exception);
        this.exception = exception;
    }

    public Integer getStatusCode() {
        return exception.getStatusCode();
    }
}
