package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class UnsupportedAcceptHeaderException extends ApiGatewayException {

    public static final String UNSUPPORTED_ACCEPT_HEADER_VALUE = "Unsupported Accept header value.";

    public UnsupportedAcceptHeaderException() {
        super(UNSUPPORTED_ACCEPT_HEADER_VALUE);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
    }
}
