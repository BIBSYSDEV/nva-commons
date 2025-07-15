package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class GoneException extends ApiGatewayException {

    public GoneException(String message) {
        super(message);
    }

    public GoneException(Exception exception) {
        super(exception);
    }

    public GoneException(Exception exception, String message) {
        super(exception, message);
    }

    public GoneException(String message, Object tombstone) {
        super(message, tombstone);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_GONE;
    }
}
