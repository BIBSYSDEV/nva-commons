package no.unit.nva.auth;

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
    private String bearerToken;

    protected AuthorizedBackendClient(HttpClient httpClient,
                                      String bearerToken,
                                      CognitoCredentials cognitoCredentials) {
        this.httpClient = httpClient;
        this.bearerToken = bearerToken;
        this.cognitoCredentials = cognitoCredentials;
    }

    @JacocoGenerated
    public static AuthorizedBackendClient prepareWithBackendCredentials(CognitoCredentials cognitoCredentials) {
        return prepareWithBackendCredentials(HttpClient.newHttpClient(), cognitoCredentials);
    }

    public static AuthorizedBackendClient prepareWithBackendCredentials(HttpClient httpClient,
                                                                        CognitoCredentials cognitoApiClientCredentials) {
        var client = new AuthorizedBackendClient(httpClient, null, cognitoApiClientCredentials);
        client.refreshToken();
        return client;
    }

    @JacocoGenerated
    public static AuthorizedBackendClient prepareWithUserCredentials(String bearerToken) {
        return prepareWithUserCredentials(HttpClient.newHttpClient(), bearerToken);
    }

    public static AuthorizedBackendClient prepareWithUserCredentials(HttpClient httpClient, String bearerToken) {
        return new AuthorizedBackendClient(httpClient, bearerToken, null);
    }

    public <T> HttpResponse<T> send(HttpRequest.Builder request, BodyHandler<T> responseBodyHandler)
        throws IOException, InterruptedException {
        var authorizedRequest = request.setHeader(AUTHORIZATION_HEADER, bearerToken).build();
        return httpClient.send(authorizedRequest, responseBodyHandler);
    }

    public <T> CompletableFuture<HttpResponse<T>> sendAsync(Builder request,
                                                            BodyHandler<T> responseBodyHandler) {
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
        return attempt(() -> formatAuthenticationHeaderValue())
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
