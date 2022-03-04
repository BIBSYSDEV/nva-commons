package nva.commons.apigatewayv2;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static nva.commons.core.attempt.Try.attempt;
import com.google.common.net.MediaType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import nva.commons.apigatewayv2.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.attempt.Try;

public final class MediaTypes {

    public static final MediaType APPLICATION_JSON_LD = MediaType.create("application", "ld+json")
        .withCharset(StandardCharsets.UTF_8);
    public static final MediaType APPLICATION_PROBLEM_JSON = MediaType.create("application", "problem+json")
        .withCharset(StandardCharsets.UTF_8);

    public static final List<MediaType> DEFAULT_SUPPORTED_MEDIA_TYPES = List.of(JSON_UTF_8);
    public static final int DEFAULT_MEDIA_TYPE = 0;
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private MediaTypes() {
    }

    public static MediaType parse(List<String> acceptedMediaTypes, List<MediaType> supportedMediaTypes) {
        var defaultMediaType = backendDefaultMediaType(supportedMediaTypes);
        var requestedMediaTypes = acceptedMediaTypes.stream()
            .map(mediaTypeString -> parseHeader(mediaTypeString, defaultMediaType))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
        var result = findFirstMatchingHeader(supportedMediaTypes, requestedMediaTypes);
        return result.orElseThrow(() -> new UnsupportedAcceptHeaderException(requestedMediaTypes, supportedMediaTypes));
    }

    private static MediaType backendDefaultMediaType(List<MediaType> supportedMediaTypes) {
        return supportedMediaTypes.get(DEFAULT_MEDIA_TYPE);
    }

    private static Optional<MediaType> findFirstMatchingHeader(List<MediaType> supportedMediaTypes,
                                                               List<MediaType> requestedMediaTypes) {
        return requestedMediaTypes.stream()
            .filter(mediaType -> isSupported(mediaType, supportedMediaTypes))
            .filter(MediaTypes::hasSupportedCharSet)
            .findFirst()
            .map(MediaTypes::removeQualityFactorAndSetCharsetIfMissing);
    }

    private static MediaType removeQualityFactorAndSetCharsetIfMissing(MediaType mediaType) {
        return MediaType.create(mediaType.type(), mediaType.subtype())
            .withCharset(mediaType.charset().or(DEFAULT_CHARSET));
    }

    private static boolean hasSupportedCharSet(MediaType mediaType) {
        return mediaType.charset().or(StandardCharsets.UTF_8).equals(StandardCharsets.UTF_8);
    }

    private static boolean isSupported(MediaType mediaType, List<MediaType> supportedMediaTypes) {
        return supportedMediaTypes
            .stream()
            .map(MediaType::withoutParameters)
            .anyMatch(supported -> supported.is(mediaType.withoutParameters()));
    }

    private static Optional<MediaType> parseHeader(String mediaTypeString, MediaType mostPreferredMediaType) {
        return Optional.of(mediaTypeString)
            .map(String::trim)
            .map(attempt(MediaType::parse))
            .flatMap(Try::toOptional)
            .map(mediaType -> mapAnyContentTypeToDefault(mediaType, mostPreferredMediaType));
    }

    private static MediaType mapAnyContentTypeToDefault(MediaType header, MediaType mostPreferredSupportedMediaType) {
        return MediaType.ANY_TYPE.is(header.withoutParameters())
                   ? mostPreferredSupportedMediaType
                   : header;
    }
}
