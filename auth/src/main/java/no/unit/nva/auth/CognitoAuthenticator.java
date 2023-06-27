package no.unit.nva.auth;

import static com.amazonaws.auth.internal.SignerConstants.AUTHORIZATION;
import static no.unit.nva.auth.AuthorizedBackendClient.APPLICATION_X_WWW_FORM_URLENCODED;
import static nva.commons.core.attempt.Try.attempt;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.jr.ob.JSON;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import nva.commons.core.paths.UriWrapper;

public class CognitoAuthenticator {

    public static final String OAUTH2_PATH_SEGMENT = "oauth2";
    public static final String TOKEN_PATH_SEGMENT = "token";
    public static final String BASIC_AUTH_CREDENTIALS_TEMPLATE = "%s:%s";
    public static final String BASIC_AUTH_HEADER_TEMPLATE = "%s %s";
    public static final String AUTHORIZATION_ERROR_MESSAGE = "Could not authorizer client";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "grant_type=client_credentials";
    public static final String JWT_TOKEN_FIELD = "access_token";
    private final CognitoCredentials credentials;
    private final HttpClient httpClient;

    public CognitoAuthenticator(HttpClient httpClient, CognitoCredentials credentials) {
        this.httpClient = httpClient;
        this.credentials = credentials;
    }

    public DecodedJWT fetchBearerToken() {
        var tokenResponse = fetchTokenResponse();
        return attempt(() -> tokenResponse)
                   .map(HttpResponse::body)
                   .map(JSON.std::mapFrom)
                   .map(json -> json.get(JWT_TOKEN_FIELD))
                   .toOptional()
                   .map(Objects::toString)
                   .map(JWT::decode)
                   .orElseThrow();
    }

    private static URI standardOauth2TokenEndpoint(URI cognitoHost) {
        return UriWrapper.fromUri(cognitoHost).addChild(OAUTH2_PATH_SEGMENT).addChild(TOKEN_PATH_SEGMENT).getUri();
    }

    private static HttpRequest.BodyPublisher clientCredentialsAuthType() {
        return HttpRequest.BodyPublishers.ofString(GRANT_TYPE_CLIENT_CREDENTIALS);
    }

    private String formatAuthenticationHeaderValue() {
        return String.format(BASIC_AUTH_CREDENTIALS_TEMPLATE,
                             credentials.getCognitoAppClientId(),
                             credentials.getCognitoAppClientSecret());
    }

    private String formatBasicAuthenticationHeader() {
        return attempt(this::formatAuthenticationHeaderValue)
                   .map(str -> Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8)))
                   .map(credentials -> String.format(BASIC_AUTH_HEADER_TEMPLATE, "Basic", credentials))
                   .orElseThrow();
    }

    private HttpRequest createTokenRequest() {
        var tokenUri = standardOauth2TokenEndpoint(credentials.getCognitoOAuthServerUri());
        return formatRequestForJwtToken(tokenUri);
    }

    private HttpResponse<String> fetchTokenResponse() {
        return attempt(
            () -> this.httpClient.send(createTokenRequest(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        )
                   .map(this::responseIsSuccessful)
                   .orElseThrow();
    }

    private HttpResponse<String> responseIsSuccessful(HttpResponse<String> response) {
        if (HttpURLConnection.HTTP_OK != response.statusCode()) {
            throw new RuntimeException(AUTHORIZATION_ERROR_MESSAGE);
        }
        return response;
    }

    private HttpRequest formatRequestForJwtToken(URI tokenUri) {
        return HttpRequest.newBuilder(tokenUri)
                   .setHeader(AUTHORIZATION, formatBasicAuthenticationHeader())
                   .setHeader(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
                   .POST(clientCredentialsAuthType())
                   .build();
    }
}