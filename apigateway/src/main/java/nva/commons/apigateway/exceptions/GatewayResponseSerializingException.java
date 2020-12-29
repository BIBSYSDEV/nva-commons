package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class GatewayResponseSerializingException extends ApiGatewayException {

    public static final String ERROR_MESSAGE = "Failed serializing ResponseBody to JSON string.";
    private static final Integer ERROR_CODE = HttpURLConnection.HTTP_INTERNAL_ERROR;

    public GatewayResponseSerializingException(Exception e) {
        super(e, ERROR_MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return ERROR_CODE;
    }
}
