package nva.commons.apigateway.exceptions;

import org.apache.http.HttpStatus;

public class NotFoundException extends ApiGatewayException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(Exception exception) {
        super(exception);
    }

    public NotFoundException(Exception exception, String message) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_NOT_FOUND;
    }
}
