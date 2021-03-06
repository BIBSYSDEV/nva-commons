package nva.commons.doi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiConverter {

    public static final String DOI_HOST = "doi.org";
    public static final String HTTPS = "https";
    public static final String HTTP = "http";
    public static final String VALUE_DELIMITER = ",";
    public static final int SINGLE_ELEMENT = 1;
    public static final String EMPTY_FRAGMENT = null;
    public static final String PATH_SEPARATOR = "/";
    public static final String NOT_ONE_URI = "Expected exactly one URI. Found:%d: %s. Input was %s";
    public static final String ERROR_WHEN_CREATING_URI = "Unexpected error while creating URI for doi:";
    private static final String ERROR_WHEN_SETTING_DOI_HOST = "Unexpected error while setting host for DOI URI:";
    private static final Logger logger = LoggerFactory.getLogger(DoiConverter.class);
    private static String DOI_PREFIX = "doi:";

    public DoiConverter() {
    }

    public URI toUri(String doi) {
        if (Objects.nonNull(doi)) {
            Set<URI> result = Stream.of(doi)
                .filter(DoiValidator::validate)
                .map(this::createUri)
                .map(this::schemeToHttps)
                .collect(Collectors.toSet());

            requireSingleElement(result, doi);
            return result.iterator().next();
        }
        return null;
    }

    private void requireSingleElement(Set<URI> uris, String errorMessageDetails) {
        if (uris.size() != SINGLE_ELEMENT) {
            String values = uris.stream().map(URI::toString).collect(Collectors.joining(VALUE_DELIMITER));
            String errorMessage = String.format(NOT_ONE_URI, uris.size(), values, errorMessageDetails);
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    private URI createUri(String doi) {
        if (isUrl(doi)) {
            return URI.create(doi);
        }
        return createUriFromDoiString(doi);
    }

    private URI createUriFromDoiString(String inputDoi) {
        String stripedDoi = stripPrefix(inputDoi);
        String doiAsPath = doiToRelativePath(stripedDoi);
        try {
            return new URI(HTTPS, DOI_HOST, doiAsPath, EMPTY_FRAGMENT);
        } catch (URISyntaxException e) {
            logger.error(ERROR_WHEN_CREATING_URI + stripedDoi, e);
            throw new IllegalStateException(e);
        }
    }

    private String doiToRelativePath(String stripedDoi) {
        if (!stripedDoi.startsWith(PATH_SEPARATOR)) {
            return PATH_SEPARATOR + stripedDoi;
        } else {
            return stripedDoi;
        }
    }

    private String stripPrefix(String doi) {
        return doi.replaceFirst(DOI_PREFIX, "");
    }

    private boolean isUrl(String doi) {
        return doi.startsWith(HTTP);
    }

    private URI schemeToHttps(URI uri) {
        try {
            return new URI(HTTPS, DOI_HOST, uri.getPath(), EMPTY_FRAGMENT);
        } catch (URISyntaxException e) {
            logger.error(ERROR_WHEN_SETTING_DOI_HOST + uri.toString());
            throw new IllegalStateException(e);
        }
    }
}
