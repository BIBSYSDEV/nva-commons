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

    public static Doi fromDoiString(String doiHost, String doiPath) {
        return new Doi(UriWrapper.fromHost(doiHost).addChild(doiPath).getUri());
    }

    public URI getUri() {
        return uri;
    }

    @Override
    @JsonValue
    public String toString() {
        return uri.toString();
    }

    public String getDoiString() {
        return UnixPath.fromString(uri.getPath()).removeRoot().toString();
    }

    public URI getStandardizedUri() {
        return UriWrapper.fromHost(DEFAULT_HOST).addChild(uri.getPath()).getUri();
    }
}
