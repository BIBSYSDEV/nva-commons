package nva.commons.exceptions;

import java.io.IOException;
import org.apache.http.HttpStatus;

public class ApiIoException extends ApiGatewayException {

    public static final int ERROR_CODE = HttpStatus.SC_INTERNAL_SERVER_ERROR;

    public ApiIoException(IOException e, String errorMessage) {
        super(e, errorMessage);
    }

    @Override
    public Integer statusCode() {
        return ERROR_CODE;
    }
}
