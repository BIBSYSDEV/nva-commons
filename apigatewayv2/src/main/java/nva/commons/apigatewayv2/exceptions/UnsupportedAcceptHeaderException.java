package nva.commons.apigatewayv2.exceptions;

import com.google.common.net.MediaType;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.stream.Collectors;

public class UnsupportedAcceptHeaderException extends ApiGatewayException {

    public static final String UNSUPPORTED_ACCEPT_HEADER_VALUE = "%s contains no supported Accept header values. "
                                                                 + "Supported values are: %s";
    public static final String JOINING_DELIMITER = ", ";

    public UnsupportedAcceptHeaderException(List<MediaType> acceptMediaTypes, List<MediaType> supportedMediaTypes) {
        super(createMessage(acceptMediaTypes, supportedMediaTypes));
    }

    public static String createMessage(List<MediaType> acceptMediaTypes, List<MediaType> supportedMediaTypes) {
        String acceptMediaTypesCsv = acceptMediaTypes.stream()
            .map(MediaType::toString)
            .collect(Collectors.joining(", "));
        String supportedMediaTypesCsv = supportedMediaTypes.stream()
            .map(MediaType::toString)
            .collect(Collectors.joining(JOINING_DELIMITER));
        return String.format(UNSUPPORTED_ACCEPT_HEADER_VALUE, acceptMediaTypesCsv, supportedMediaTypesCsv);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
    }
}
