package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class UnsupportedOperationException extends ApiGatewayException {

    public UnsupportedOperationException(String message) {
        super(message);
    }

    public UnsupportedOperationException(Exception exception) {
        super(exception);
    }

    public UnsupportedOperationException(Exception exception, String message) {
        super(exception, message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_METHOD;
    }

}
