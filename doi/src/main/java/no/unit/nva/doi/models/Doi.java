package no.unit.nva.doi.models;

import static nva.commons.core.StringUtils.isBlank;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;

public class Doi {

    public static final String DEFAULT_HOST = "doi.org";
    public static final String NULL_ARGUMENT_ERROR = "No argument should be blank";
    private static final String DOI_SCHEME = "doi";
    private static final String DOC_SCHEME = "doc";
    private final URI uri;

    protected Doi(URI doiUri) {
        this.uri = doiUri;
    }

    @JsonCreator
    public static Doi fromUriString(String uriString) {
        return Doi.fromUri(URI.create(uriString));
    }

    public static Doi fromUri(URI doiUri) {
        return new Doi(doiUri);
    }

    public static Doi fromDoiIdentifier(String doiHost, String doiIdentifier) {
        return new Doi(UriWrapper.fromHost(doiHost).addChild(doiIdentifier).getUri());
    }

    public static Doi fromPrefixAndSuffix(String doiHost, String prefix, String suffix) {
        if (isBlank(doiHost) || isBlank(prefix) || isBlank(suffix)) {
            throw new IllegalArgumentException(NULL_ARGUMENT_ERROR);
        }
        return new Doi(UriWrapper.fromHost(doiHost).addChild(prefix).addChild(suffix).getUri());
    }

    public static Doi fromDoiIdentifier(String doiIdentifier) {
        return new Doi(UriWrapper.fromHost(DEFAULT_HOST).addChild(doiIdentifier).getUri());
    }

    public URI getUri() {
        return uri;
    }

    public Doi changeHost(String host) {
        var newUri = UriWrapper.fromHost(host).addChild(uri.getPath()).getUri();
        return Doi.fromUri(newUri);
    }

    @Override
    @JsonValue
    public String toString() {
        return uri.toString();
    }

    public String toIdentifier() {
        return UnixPath.fromString(uri.getPath()).removeRoot().toString();
    }

    public URI getStandardizedUri() {
        return isDoiUri() ? convertDoiUriToHttpsUri() : convertHttpBasedUri();
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Doi)) {
            return false;
        }
        Doi doi = (Doi) o;
        return Objects.equals(getUri(), doi.getUri());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getUri());
    }

    private URI convertHttpBasedUri() {
        return new Doi(UriWrapper.fromHost(DEFAULT_HOST).addChild(uri.getPath()).getUri()).getUri();
    }

    private URI convertDoiUriToHttpsUri() {
        return UriWrapper.fromHost(DEFAULT_HOST).addChild(uri.getSchemeSpecificPart()).getUri();
    }

    private boolean isDoiUri() {
        return DOI_SCHEME.equalsIgnoreCase(uri.getScheme()) || DOC_SCHEME.equalsIgnoreCase(uri.getScheme());
    }
}
