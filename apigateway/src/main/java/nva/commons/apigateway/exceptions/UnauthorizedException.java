package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class UnauthorizedException extends ApiGatewayException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException() {
        this("Unauthorized");
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_UNAUTHORIZED;
    }
}
