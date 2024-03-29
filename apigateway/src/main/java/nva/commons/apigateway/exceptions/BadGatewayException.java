package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class BadGatewayException extends ApiGatewayException {

    public BadGatewayException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_GATEWAY;
    }
}
