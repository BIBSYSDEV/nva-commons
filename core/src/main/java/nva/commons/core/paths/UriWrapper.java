package nva.commons.core.paths;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

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
    private static final int VALUE = 1;
    private static final int DEFAULT_PORT = -VALUE;
    private static final String AMPERSAND = "&";
    private static final String ASSIGNMENT = "=";
    private static final int NO_MATCH = -1;
    private URI uri;

    @JacocoGenerated
    @Deprecated(forRemoval = true)
    public UriWrapper(URI uri) {
        if (isNull(uri)) {
            throw new IllegalArgumentException(NULL_INPUT_ERROR);
        }
        this.uri = uri;
    }

    /**
     * Utility for working with URIs in a consistent manner.
     *
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

    public static UriWrapper fromUri(URI uri) {
        return new UriWrapper(uri);
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

    public UriWrapper replacePathElementByIndexFromEnd(int index, String replacement) {
        var newPath = getPath().replacePathElementByIndexFromEnd(index, replacement);
        return attempt(() -> new URI(uri.getScheme(),
                                     uri.getUserInfo(),
                                     uri.getHost(),
                                     uri.getPort(),
                                     newPath.toString(),
                                     uri.getQuery(),
                                     uri.getFragment()))
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

    public String getLastPathElement() {
        return getPath().getLastPathElement();
    }

    /**
     * Returns the last element from the URI path.
     *
     * @return element.
     * @deprecated User getLastPathElement
     */
    @Deprecated(since = "getLastPathElement was introduced")
    @JacocoGenerated
    public String getFilename() {
        return getLastPathElement();
    }

    public UriWrapper addQueryParameter(String param, String value) {
        this.uri = attempt(() -> new URIBuilder(uri)
                                     .removeQuery()
                                     .addParameters(extractUriQueryParameters())
                                     .addParameter(param, value)
                                     .build())
                       .orElseThrow();
        return this;
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

        return
            attempt(() -> createUriFromHostUri(scheme, host, port))
                .or(() -> createUriFromHostDomain(scheme, host, port))
                .orElseThrow(fail -> new IllegalArgumentException(MISSING_HOST, fail.getException()));
    }

    private static URI createUriFromHostUri(String scheme, String host, int port) throws URISyntaxException {
        var hostUri = URI.create(host);
        var hostString = hostUri.getHost();
        return createUriFromHostDomain(scheme, hostString, port);
    }

    private static URI createUriFromHostDomain(String scheme, String host, int port) throws URISyntaxException {
        return new URI(scheme, EMPTY_USER_INFO, host, port, EMPTY_PATH, EMPTY_QUERY, EMPTY_FRAGMENT);
    }

    private static List<NameValuePair> extractKeyValuePairs(String query) {
        return Arrays.stream(splitKeyValuePairs(query))
                   .map(UriWrapper::createNameValuePair)
                   .toList();
    }

    private static NameValuePair createNameValuePair(String pairString) {
        var assignation = pairString.indexOf(ASSIGNMENT);
        if (assignation == NO_MATCH) {
            return new BasicNameValuePair(decode(pairString), null);
        }
        return new BasicNameValuePair(decode(pairString.substring(0, assignation)),
                                      decode(pairString.substring(assignation + 1)));
    }

    private static String decode(String encodedString) {
        return URLDecoder.decode(encodedString, StandardCharsets.UTF_8);
    }

    private static String[] splitKeyValuePairs(String query) {
        return query.split(AMPERSAND);
    }

    private List<NameValuePair> extractUriQueryParameters() {
        var query = uri.getRawQuery();
        return nonNull(query)
                   ? extractKeyValuePairs(query)
                   : Collections.emptyList();
    }
}
