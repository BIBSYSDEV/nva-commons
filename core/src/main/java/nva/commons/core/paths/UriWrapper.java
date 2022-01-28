package nva.commons.core.paths;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;

/**
 * Class for easily building and manipulating URIs. Tools to easily append paths and not have to deal with checking
 * whether the path delimiter is present or not.
 */
public class UriWrapper {

    public static final String EMPTY_FRAGMENT = null;
    public static final String EMPTY_PATH = null;
    public static final String EMPTY_USER_INFO = null;
    public static final String EMPTY_QUERY = null;
    public static final String MISSING_HOST = "Missing host for creating URI";
    public static final String HTTPS = "https";
    public static final String NULL_INPUT_ERROR = "Input URI cannot be null.";
    private static final int DEFAULT_PORT = -1;
    private final URI uri;

    public UriWrapper(URI uri) {
        if (isNull(uri)) {
            throw new IllegalArgumentException(NULL_INPUT_ERROR);
        }
        this.uri = uri;
    }

    /**
     * @param uri the URI string
     * @deprecated Use the static call {@link UriWrapper#fromUri(String)} instead.
     */
    @JacocoGenerated
    @Deprecated(forRemoval = true)
    public UriWrapper(String uri) {
        this(URI.create(uri));
    }

    public UriWrapper(String scheme, String host) {
        this(createUriWithSchemeAndHost(scheme, host, DEFAULT_PORT));
    }

    public static UriWrapper fromUri(String uri) {
        return new UriWrapper(URI.create(uri));
    }

    public static UriWrapper fromHost(String host) {
        return new UriWrapper(createUriWithSchemeAndHost(UriWrapper.HTTPS, host, DEFAULT_PORT));
    }

    public static UriWrapper fromHost(String host, int port) {
        return new UriWrapper(createUriWithSchemeAndHost(UriWrapper.HTTPS, host, port));
    }

    public URI getUri() {
        return uri;
    }

    public UriWrapper getHost() {
        return attempt(() -> new URI(uri.getScheme(), uri.getHost(), EMPTY_PATH, EMPTY_FRAGMENT))
            .map(UriWrapper::new)
            .orElseThrow();
    }

    public UriWrapper addChild(String... path) {
        return addChild(UnixPath.of(path));
    }

    /**
     * Appends a path to the URI.
     *
     * @param childPath the path to be appended.
     * @return a UriWrapper containing the whole path.
     */
    public UriWrapper addChild(UnixPath childPath) {
        UnixPath thisPath = getPath();
        UnixPath totalPath = thisPath.addChild(childPath).addRoot();

        return attempt(
            () -> new URI(uri.getScheme(),
                          uri.getUserInfo(),
                          uri.getHost(),
                          uri.getPort(),
                          totalPath.toString(),
                          uri.getQuery(),
                          EMPTY_FRAGMENT))
            .map(UriWrapper::new)
            .orElseThrow();
    }

    public UnixPath toS3bucketPath() {
        return getPath().removeRoot();
    }

    public UnixPath getPath() {
        String pathString = uri.getPath();
        return UnixPath.of(pathString);
    }

    public Optional<UriWrapper> getParent() {
        return Optional.of(uri)
            .map(URI::getPath)
            .map(UnixPath::of)
            .flatMap(UnixPath::getParent)
            .map(attempt(p -> new URI(uri.getScheme(),
                                      uri.getUserInfo(),
                                      uri.getHost(),
                                      uri.getPort(),
                                      p.toString(),
                                      EMPTY_QUERY,
                                      EMPTY_FRAGMENT)))
            .map(Try::orElseThrow)
            .map(UriWrapper::new);
    }

    public String getFilename() {
        return getPath().getFilename();
    }

    public UriWrapper addQueryParameter(String param, String value) {
        var queryString = StringUtils.isBlank(uri.getQuery())
                              ? param + "=" + value
                              : uri.getQuery() + "&" + param + "=" + value;
        URI newUri = attempt(() -> new URI(uri.getScheme(),
                                           uri.getUserInfo(),
                                           uri.getHost(),
                                           uri.getPort(),
                                           uri.getPath(),
                                           queryString,
                                           EMPTY_FRAGMENT))
            .orElseThrow();
        return new UriWrapper(newUri);
    }

    public UriWrapper addQueryParameters(Map<String, String> parameters) {
        UriWrapper uriWrapper = new UriWrapper(getUri());
        for (Map.Entry<String, String> e : parameters.entrySet()) {
            uriWrapper = uriWrapper.addQueryParameter(e.getKey(), e.getValue());
        }
        return uriWrapper;
    }

    @Override
    public String toString() {
        return this.getUri().toString();
    }

    private static URI createUriWithSchemeAndHost(String scheme, String host, int port) {
        return attempt(() -> new URI(scheme, EMPTY_USER_INFO, host, port, EMPTY_PATH, EMPTY_QUERY, EMPTY_FRAGMENT))
            .orElseThrow(fail -> new IllegalArgumentException(MISSING_HOST, fail.getException()));
    }
}
