package nva.commons.doi;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.concurrent.CompletableFuture;
import nva.commons.core.JacocoGenerated;

public class UnitHttpClient {

    public static final String USER_AGENT = "User-Agent";

    public static final String NVA_USER_AGENT =
        "nva/1.0 (https://github.com/BIBSYSDEV/nva-commons; mailto:support@unit.no)";

    private final HttpClient httpClient;

    @JacocoGenerated
    public UnitHttpClient() {
        httpClient = HttpClient.newHttpClient();
    }

    @JacocoGenerated
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(Builder request, BodyHandler<T> body) {
        HttpRequest finalRequest = request.header(USER_AGENT, NVA_USER_AGENT)
            .build();
        return httpClient.sendAsync(finalRequest, body);
    }
}
