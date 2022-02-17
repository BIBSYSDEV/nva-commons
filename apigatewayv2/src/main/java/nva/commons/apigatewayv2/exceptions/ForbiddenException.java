package nva.commons.apigatewayv2.exceptions;

import java.net.HttpURLConnection;

public class ForbiddenException extends ApiGatewayException {

    public static final String DEFAULT_MESSAGE = "Forbidden";

    public ForbiddenException() {
        super(DEFAULT_MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_FORBIDDEN;
    }
}
