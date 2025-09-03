package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class PreconditionFailedException extends ApiGatewayException {

    public static final String PRECONDITION_FAILED = "Precondition Failed";

    public PreconditionFailedException(String message) {
        super(message);
    }

    public PreconditionFailedException() {
        super(PRECONDITION_FAILED);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_PRECON_FAILED;
    }
}
