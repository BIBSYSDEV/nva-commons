package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class UnauthorizedException extends ApiGatewayException {

    public UnauthorizedException() {
        super("Unauthorized");
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_UNAUTHORIZED;
    }
}
