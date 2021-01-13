package nva.commons.apigateway.exceptions;

import java.io.IOException;
import java.net.HttpURLConnection;

public class ApiIoException extends ApiGatewayException {

    public static final int ERROR_CODE = HttpURLConnection.HTTP_INTERNAL_ERROR;

    public ApiIoException(IOException e, String errorMessage) {
        super(e, errorMessage);
    }

    @Override
    public Integer statusCode() {
        return ERROR_CODE;
    }
}
