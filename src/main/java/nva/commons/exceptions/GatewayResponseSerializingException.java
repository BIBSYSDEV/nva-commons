package nva.commons.exceptions;

import org.apache.http.HttpStatus;

public class GatewayResponseSerializingException extends ApiGatewayException {

    public static final String ERROR_MESSAGE = "Failed serializing ResponseBody to JSON string.";
    private static final Integer ERROR_CODE = HttpStatus.SC_INTERNAL_SERVER_ERROR;

    public GatewayResponseSerializingException(Exception e) {
        super(e, ERROR_MESSAGE);
    }

    @Override
    protected Integer statusCode() {
        return ERROR_CODE;
    }
}
