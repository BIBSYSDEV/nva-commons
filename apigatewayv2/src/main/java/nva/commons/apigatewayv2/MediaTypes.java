package nva.commons.apigatewayv2;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import nva.commons.apigatewayv2.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.attempt.Try;

public final class MediaTypes {

    public static final MediaType APPLICATION_JSON_LD = MediaType.create("application", "ld+json");
    public static final MediaType APPLICATION_PROBLEM_JSON = MediaType.create("application", "problem+json");
    public static final String ACCEPT_HEADER_VALUES_DELIMITER = ",";
    public static final List<MediaType> DEFAULT_SUPPORTED_MEDIA_TYPES = List.of(JSON_UTF_8.withoutParameters());
    public static final int MOST_PREFERRED_DEFAULT_MEDIA_TYPE = 0;

    private MediaTypes() {
    }

    public static MediaType parse(List<String> acceptedMediaTypes, List<MediaType> supportedMediaTypes)
        throws UnsupportedAcceptHeaderException {
        var mostPreferredMediaType = mostPreferredMediaType(supportedMediaTypes);
        var requestedMediaTypes = acceptedMediaTypes.stream()
            .map(mediaTypeString -> parseHeader(mediaTypeString, mostPreferredMediaType))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
        var result = findFirstMatchingHeader(supportedMediaTypes, requestedMediaTypes);
        return result.orElseThrow(() -> new UnsupportedAcceptHeaderException(requestedMediaTypes, supportedMediaTypes));
    }

    private static MediaType mostPreferredMediaType(List<MediaType> supportedMediaTypes) {
        return supportedMediaTypes.get(MOST_PREFERRED_DEFAULT_MEDIA_TYPE);
    }

    private static Optional<MediaType> findFirstMatchingHeader(List<MediaType> supportedMediaTypes,
                                                               List<MediaType> requestedMediaTypes) {
        return requestedMediaTypes.stream()
            .filter(mediaType -> isSupported(mediaType, supportedMediaTypes))
            .findFirst();
    }

    private static boolean isSupported(MediaType mediaType, List<MediaType> supportedMediaTypes) {
        return supportedMediaTypes
            .stream()
            .map(MediaType::withoutParameters)
            .anyMatch(supported -> supported.is(mediaType.withoutParameters()));
    }

    private static Optional<MediaType> parseHeader(String mediaTypeString, MediaType mostPreferedMediaType) {
        var x = Optional.of(mediaTypeString)
            .map(String::trim)
            .map(attempt(MediaType::parse))
            .flatMap(Try::toOptional)
            .map(mediaType -> mapAnyContentTypeToDefault(mediaType, mostPreferedMediaType));
        return x;
    }

    private static MediaType mapAnyContentTypeToDefault(MediaType header, MediaType mostPreferredSupportedMediaType) {
        return MediaType.ANY_TYPE.is(header)
                   ? mostPreferredSupportedMediaType
                   : header;
    }
}