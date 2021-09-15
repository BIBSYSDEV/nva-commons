package nva.commons.core.paths;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.attempt.Try;

/**
 * Class for easily building and manipulating URIs. Tools to easily append paths and not have to deal with checking
 * whether the path delimiter is present or not.
 */
public class UriWrapper {

    public static final String EMPTY_FRAGMENT = null;
    public static final String EMPTY_PATH = null;
    private final URI uri;

    public UriWrapper(URI uri) {
        assert Objects.nonNull(uri);
        this.uri = uri;
    }

    public UriWrapper(String uri) {
        this(URI.create(uri));
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

        return attempt(() -> new URI(uri.getScheme(), uri.getHost(), totalPath.toString(), EMPTY_FRAGMENT))
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
}
