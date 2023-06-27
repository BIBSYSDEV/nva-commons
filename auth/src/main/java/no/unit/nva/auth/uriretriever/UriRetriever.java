package no.unit.nva.auth.uriretriever;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class UriRetriever implements RawContentRetriever {

    public static final String ACCEPT = "Accept";
    private final HttpClient httpClient;

    public UriRetriever() {
        this.httpClient = newHttpClient();
    }

    public UriRetriever(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Optional<String> getRawContent(URI uri, String mediaType) {
        return attempt(() -> httpClient.send(createHttpRequest(uri, mediaType),
            BodyHandlers.ofString(StandardCharsets.UTF_8)))
                   .map(HttpResponse::body)
                   .toOptional();
    }

    private static HttpClient newHttpClient() {
        return HttpClient.newHttpClient();
    }

    private HttpRequest createHttpRequest(URI uri, String mediaType) {
        return HttpRequest.newBuilder()
                   .uri(uri)
                   .headers(ACCEPT, mediaType)
                   .GET()
                   .build();
    }
}