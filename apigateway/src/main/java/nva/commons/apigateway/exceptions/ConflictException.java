package nva.commons.apigateway.exceptions;

import org.apache.http.HttpStatus;

public class ConflictException extends ApiGatewayException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(Exception exception) {
        super(exception);
    }

    public ConflictException(Exception exception, String message) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_CONFLICT;
    }
}
