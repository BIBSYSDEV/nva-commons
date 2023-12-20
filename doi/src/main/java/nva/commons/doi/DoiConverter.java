package nva.commons.doi;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiConverter {

    public static final String DOI_HOST = "doi.org";
    public static final String HTTPS = "https";
    public static final String HTTP = "http";
    public static final String EMPTY_FRAGMENT = null;
    public static final String PATH_SEPARATOR = "/";
    public static final String ROOT_PATH = PATH_SEPARATOR;
    public static final String EMPTY_STRING = "";
    public static final String NON_ALPHANUMERIC_CHARACTERS_AT_THE_END_OF_STRING = "[^\\w\\d]+$";
    private static final String ERROR_WHEN_SETTING_DOI_HOST = "Unexpected error while setting host for DOI URI:";
    private static final Logger logger = LoggerFactory.getLogger(DoiConverter.class);
    private static final String NOT_HTTP_URI_REGEX = "([^/]+:)";
    public static final Pattern CONTAINS_BASE_PATH_BUT_NOT_PROTOCOL = Pattern.compile(
        "(?<!https?://)(?:dx\\.)?doi\\.org/.*", Pattern.CASE_INSENSITIVE);
    public static final String PROTOCOL = "https://";

    private final Function<URI, Boolean> onlineValidationFunction;

    @JacocoGenerated
    public DoiConverter() {
        this(new DoiValidator());
    }

    public DoiConverter(Function<URI, Boolean> onlineValidationFunction) {
        this.onlineValidationFunction = onlineValidationFunction;
    }

    public DoiConverter(DoiValidator doiValidator) {
        this(doiValidator::validateOnline);
    }

    public URI toUri(String doi) {
        return Optional.ofNullable(doi)
                   .stream()
                   .map(StringUtils::removeWhiteSpaces)
                   .map(this::removeGarbageCharacters)
                   .map(this::addHttpsIfNecessary)
                   .filter(DoiValidator::validateOrThrow)
                   .map(this::createUri)
                   .filter(onlineValidationFunction::apply)
                   .collect(SingletonCollector.collectOrElse(null));
    }

    private String addHttpsIfNecessary(String doi) {
        var matcher = CONTAINS_BASE_PATH_BUT_NOT_PROTOCOL.matcher(doi);
        if (matcher.matches()) {
            return PROTOCOL + doi;
        }
        return doi;
    }

    private String removeGarbageCharacters(String doi) {
        return doi.replaceFirst(NON_ALPHANUMERIC_CHARACTERS_AT_THE_END_OF_STRING, EMPTY_STRING);
    }

    private URI createUri(String doi) {
        URI uri = isUrl(doi) ? URI.create(doi) : createUriFromDoiString(doi);
        return mapToStandardUri(uri);
    }

    private URI createUriFromDoiString(String inputDoi) {
        String stripedDoi = stripPrefix(inputDoi);
        String doiAsPath = doiToRelativePath(stripedDoi);
        return attempt(() -> new URI(HTTPS, DOI_HOST, doiAsPath, EMPTY_FRAGMENT)).orElseThrow();
    }

    private String doiToRelativePath(String stripedDoi) {
        if (!stripedDoi.startsWith(PATH_SEPARATOR)) {
            return PATH_SEPARATOR + stripedDoi;
        } else {
            return stripedDoi;
        }
    }

    private String stripPrefix(String doi) {
        return doi.replaceFirst(NOT_HTTP_URI_REGEX, EMPTY_STRING);
    }

    private boolean isUrl(String doi) {
        return doi.startsWith(HTTP);
    }

    private URI mapToStandardUri(URI uri) {
        try {
            return new URI(HTTPS, DOI_HOST, uri.getPath(), EMPTY_FRAGMENT);
        } catch (URISyntaxException e) {
            logger.error(ERROR_WHEN_SETTING_DOI_HOST + uri);
            throw new IllegalStateException(e);
        }
    }
}
