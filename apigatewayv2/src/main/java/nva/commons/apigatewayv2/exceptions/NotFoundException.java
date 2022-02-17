package nva.commons.apigatewayv2.exceptions;

import java.net.HttpURLConnection;

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
        return HttpURLConnection.HTTP_NOT_FOUND;
    }
}
