package nva.commons.exceptions;

import org.apache.http.HttpStatus;

public class LoggerNotSetException extends ApiGatewayException {

    public static final String WARNING_MESSAGE = "Logger not set for handler:";
    public static final String DIRECTIONS = "Set logger in handler constructor: logger = "
        + "LoggerFactory.getLogger(MyHandler.class) ";
    public static final String META_FORMATTING = "%s%%s%s";
    public static final String ERROR_MESSAGE_PATTERN = String.format(META_FORMATTING, WARNING_MESSAGE, DIRECTIONS);

    public LoggerNotSetException(String className) {
        super(String.format(ERROR_MESSAGE_PATTERN, className));
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }
}
