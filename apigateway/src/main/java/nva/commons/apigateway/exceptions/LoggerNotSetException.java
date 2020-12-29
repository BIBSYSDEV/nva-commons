package nva.commons.apigateway.exceptions;

import java.net.HttpURLConnection;

public class LoggerNotSetException extends ApiGatewayException {

    public static final String WARNING_MESSAGE = "Logger not set for handler: ";
    public static final String DIRECTIONS = "Set logger in handler constructor: logger = "
        + "LoggerFactory.getLogger(MyHandler.class) ";
    public static final String PATTERN_FOR_STR_WITH_ONE_FORMATTING_PLACEHOLDER = "%s%%s%s";
    public static final String ERROR_MESSAGE_PATTERN = String.format(PATTERN_FOR_STR_WITH_ONE_FORMATTING_PLACEHOLDER,
        WARNING_MESSAGE, DIRECTIONS);

    public LoggerNotSetException(String className) {
        super(String.format(ERROR_MESSAGE_PATTERN, className));
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_INTERNAL_ERROR;
    }
}
