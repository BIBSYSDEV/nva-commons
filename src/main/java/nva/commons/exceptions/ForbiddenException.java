package nva.commons.exceptions;

import org.apache.http.HttpStatus;

public class ForbiddenException extends ApiGatewayException {

    private static final String FORBIDDEN_MESSAGE = "Forbidden";

    public ForbiddenException() {
        super(FORBIDDEN_MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_FORBIDDEN;
    }
}
