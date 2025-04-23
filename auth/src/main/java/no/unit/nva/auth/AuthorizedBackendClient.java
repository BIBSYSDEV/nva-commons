package no.unit.nva.auth;

import static java.util.Objects.isNull;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class AuthorizedBackendClient {

    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_HOST = "API_HOST";

    private final HttpClient httpClient;
    private final CachedJwtProvider cachedJwtProvider;
    private final boolean bearerTokenIsNotInjectedDirectly;
    private String bearerToken;

    protected AuthorizedBackendClient(HttpClient httpClient,
                                      String bearerToken,
                                      CognitoCredentials cognitoCredentials) {
        this.httpClient = httpClient;
        this.bearerToken = bearerToken;
        this.cachedJwtProvider = new CachedJwtProvider(new CognitoAuthenticator(httpClient, cognitoCredentials),
                                                       Clock.systemDefaultZone());
        this.bearerTokenIsNotInjectedDirectly = isNull(bearerToken);
    }

    @JacocoGenerated
    public static AuthorizedBackendClient prepareWithCognitoCredentials(CognitoCredentials cognitoCredentials) {
        return prepareWithCognitoCredentials(HttpClient.newHttpClient(), cognitoCredentials);
    }

    public static AuthorizedBackendClient prepareWithCognitoCredentials(
        HttpClient httpClient, CognitoCredentials cognitoApiClientCredentials) {
        return new AuthorizedBackendClient(httpClient, null, cognitoApiClientCredentials);
    }

    @JacocoGenerated
    public static AuthorizedBackendClient prepareWithBearerToken(String bearerToken) {
        return prepareWithBearerToken(HttpClient.newHttpClient(), bearerToken);
    }

    public static AuthorizedBackendClient prepareWithBearerToken(HttpClient httpClient, String bearerToken) {
        return new AuthorizedBackendClient(httpClient, bearerToken, null);
    }

    @JacocoGenerated
    public static AuthorizedBackendClient prepareWithBearerTokenAndCredentials(HttpClient httpClient,
                                                                               String bearerToken,
                                                                               CognitoCredentials cognitoCredentials) {
        return new AuthorizedBackendClient(httpClient, bearerToken, cognitoCredentials);
    }

    public <T> HttpResponse<T> send(HttpRequest.Builder request, BodyHandler<T> responseBodyHandler)
        throws IOException, InterruptedException {
        refreshTokenIfExpired();
        var authorizedRequest = request.setHeader(AUTHORIZATION_HEADER, bearerToken).build();

        if (!hasValidBackendHost(authorizedRequest)) {
            throw new IllegalArgumentException(
                "Request host does not match the backend hostname or API_HOST is not set");
        }

        return httpClient.send(authorizedRequest, responseBodyHandler);
    }

    private boolean hasValidBackendHost(HttpRequest request) {
        return new Environment().readEnvOpt(API_HOST)
                   .map(hostName -> request.uri().getHost().equals(hostName))
                   .orElse(false);
    }

    public <T> CompletableFuture<HttpResponse<T>> sendAsync(Builder request,
                                                            BodyHandler<T> responseBodyHandler) {
        refreshTokenIfExpired();
        var authorizedRequest = request.setHeader(AUTHORIZATION_HEADER, bearerToken).build();
        return httpClient.sendAsync(authorizedRequest, responseBodyHandler);
    }

    @JacocoGenerated
    protected String getBearerToken() {
        return bearerToken;
    }

    private void refreshTokenIfExpired() {
        if (bearerTokenIsNotInjectedDirectly) {
            this.bearerToken = createBearerToken(cachedJwtProvider.getValue().getToken());
        }
    }

    private String createBearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
