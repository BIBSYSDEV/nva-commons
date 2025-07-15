package no.unit.nva.auth.uriretriever;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Optional;

public interface RawContentRetriever {

    Optional<String> getRawContent(URI uri, String mediaType);

    Optional<HttpResponse<String>> fetchResponse(URI uri, String mediaType);
}