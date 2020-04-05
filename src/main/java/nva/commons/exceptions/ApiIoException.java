package nva.commons.exceptions;

import java.io.IOException;
import org.apache.http.HttpStatus;

public class ApiIoException extends ApiGatewayException {

    public ApiIoException(IOException e, String errorMessage) {
        super(e, errorMessage);
    }

    @Override
    public Integer statusCode() {
        return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
}
