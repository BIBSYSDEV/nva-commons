package no.unit.nva.auth;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.jr.ob.JSON;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class AuthorizedBackendClient {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String JWT_TOKEN_FIELD = "access_token";

    public static final Map<String, String> GRANT_TYPE_CLIENT_CREDENTIALS = Map.of("grant_type", "client_credentials");
    private final HttpClient httpClient;
    private final CognitoCredentials cognitoCredentials;
    private final boolean bearerTokenIsNotInjectedDirectly;
    private String bearerToken;

    protected AuthorizedBackendClient(HttpClient httpClient,
                                      String bearerToken,
                                      CognitoCredentials cognitoCredentials) {
        this.httpClient = httpClient;
        this.bearerToken = bearerToken;
        this.cognitoCredentials = cognitoCredentials;
        this.bearerTokenIsNotInjectedDirectly = isNull(bearerToken);
    }

    @JacocoGenerated
    public static AuthorizedBackendClient prepareWithCognitoCredentials(CognitoCredentials cognitoCredentials) {
        return prepareWithCognitoCredentials(HttpClient.newHttpClient(), cognitoCredentials);
    }

    public static AuthorizedBackendClient prepareWithCognitoCredentials(HttpClient httpClient,
                                                                        CognitoCredentials cognitoApiClientCredentials) {
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
    protected String getBearerToken() {
        return bearerToken;
    }

    public <T> HttpResponse<T> send(HttpRequest.Builder request, BodyHandler<T> responseBodyHandler)
        throws IOException, InterruptedException {
        if (bearerTokenIsNotInjectedDirectly) {
            refreshToken();
        }
        var authorizedRequest = request.setHeader(AUTHORIZATION_HEADER, bearerToken).build();
        return httpClient.send(authorizedRequest, responseBodyHandler);
    }

    public <T> CompletableFuture<HttpResponse<T>> sendAsync(Builder request,
                                                            BodyHandler<T> responseBodyHandler) {
        if (bearerTokenIsNotInjectedDirectly) {
            refreshToken();
        }
        var authorizedRequest = request.setHeader(AUTHORIZATION_HEADER, bearerToken).build();
        return httpClient.sendAsync(authorizedRequest, responseBodyHandler);
    }

    private static URI standardOauth2TokenEndpoint(URI cognitoHost) {
        return UriWrapper.fromUri(cognitoHost).addChild("oauth2").addChild("token").getUri();
    }

    private static HttpRequest.BodyPublisher clientCredentialsAuthType() {
        var queryParameters = UriWrapper.fromHost("notimportant")
            .addQueryParameters(GRANT_TYPE_CLIENT_CREDENTIALS).getUri().getRawQuery();
        return HttpRequest.BodyPublishers.ofString(queryParameters);
    }

    private String formatBasicAuthenticationHeader() {
        return attempt(this::formatAuthenticationHeaderValue)
            .map(str -> Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8)))
            .map(credentials -> "Basic " + credentials)
            .orElseThrow();
    }

    private String formatAuthenticationHeaderValue() {
        return String.format("%s:%s",
                             cognitoCredentials.getCognitoAppClientId(),
                             cognitoCredentials.getCognitoAppClientSecret());
    }

    private void refreshToken() {
        var tokenUri = standardOauth2TokenEndpoint(cognitoCredentials.getCognitoOAuthServerUri());
        var request = formatRequestForJwtToken(tokenUri);
        this.bearerToken = sendRequestAndExtractToken(request);
    }

    private String createBearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String sendRequestAndExtractToken(HttpRequest request) {
        return attempt(() -> this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
            .map(HttpResponse::body)
            .map(JSON.std::mapFrom)
            .map(json -> json.get(JWT_TOKEN_FIELD))
            .map(Objects::toString)
            .map(this::createBearerToken)
            .orElseThrow();
    }

    private HttpRequest formatRequestForJwtToken(URI tokenUri) {
        return HttpRequest.newBuilder(tokenUri)
            .setHeader(AUTHORIZATION_HEADER, formatBasicAuthenticationHeader())
            .setHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
            .POST(clientCredentialsAuthType())
            .build();
    }
}
