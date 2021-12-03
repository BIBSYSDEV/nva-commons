package nva.commons.core.paths;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Try;

/**
 * Class for easily building and manipulating URIs. Tools to easily append paths and not have to deal with checking
 * whether the path delimiter is present or not.
 */
public class UriWrapper {

    public static final String EMPTY_FRAGMENT = null;
    public static final String EMPTY_PATH = null;
    public static final String MISSING_HOST = "Missing host for creating URI";
    private final URI uri;

    public UriWrapper(URI uri) {
        assert Objects.nonNull(uri);
        this.uri = uri;
    }

    public UriWrapper(String uri) {
        this(URI.create(uri));
    }

    public UriWrapper(String scheme, String host) {
        this(createUriWithSchemeAndHost(scheme, host));
    }

    private static URI createUriWithSchemeAndHost(String scheme, String host) {
        return attempt(() -> new URI(scheme, host, EMPTY_PATH, EMPTY_FRAGMENT))
            .orElseThrow(fail -> new IllegalArgumentException(MISSING_HOST, fail.getException()));
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
            () -> new URI(uri.getScheme(), uri.getHost(), totalPath.toString(), uri.getQuery(), EMPTY_FRAGMENT))
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
            .map(attempt(p -> new URI(uri.getScheme(), uri.getHost(), p.toString(), EMPTY_FRAGMENT)))
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
        URI newUri = attempt(() -> new URI(uri.getScheme(), uri.getHost(), uri.getPath(), queryString, EMPTY_FRAGMENT))
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

    public String toString() {
        return this.getUri().toString();
    }
}
