package nva.commons.exceptions;

import org.apache.http.HttpStatus;

public class ForbiddenException extends ApiGatewayException {

    public static String DEFAULT_MESSAGE = "Forbidden";

    public ForbiddenException() {
        super(DEFAULT_MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_FORBIDDEN;
    }
}
