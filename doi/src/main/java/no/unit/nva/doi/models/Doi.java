package no.unit.nva.doi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;

public class Doi {

    public static final String DEFAULT_HOST = "doi.org";
    private final URI uri;

    public Doi(URI doiUri) {
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
        return UriWrapper.fromHost(DEFAULT_HOST).addChild(uri.getPath()).getUri();
    }
}
