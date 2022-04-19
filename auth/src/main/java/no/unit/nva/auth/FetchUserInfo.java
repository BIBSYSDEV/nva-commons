package no.unit.nva.auth;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.function.Supplier;
import nva.commons.core.paths.UriWrapper;

public class FetchUserInfo {

    public static final String OAUTH_USER_INFO_ENDPOINT = "oauth2/userInfo";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_ERROR_MESSAGE = "Could not authorizer user";
    private final HttpClient httpClient;
    private final Supplier<URI> cognitoUri;
    private final String authorizationHeader;

    public FetchUserInfo(HttpClient httpClient, Supplier<URI> cognitoUri, String authorizationHeader) {
        this.httpClient = httpClient;
        this.cognitoUri = cognitoUri;
        this.authorizationHeader = authorizationHeader;
    }

    public CognitoUserInfo fetch() {
        var queryUri = UriWrapper.fromUri(cognitoUri.get()).addChild(OAUTH_USER_INFO_ENDPOINT).getUri();
        var request = HttpRequest.newBuilder(queryUri)
            .header(AUTHORIZATION_HEADER, authorizationHeader)
            .GET()
            .build();
        return attempt(() -> httpClient.send(request, BodyHandlers.ofString()))
            .map(HttpResponse::body)
            .map(CognitoUserInfo::fromString)
            .orElseThrow(fail -> handleFailure(fail.getException()));
    }

    private RuntimeException handleFailure(Exception exception) {
        return new RuntimeException(AUTHORIZATION_ERROR_MESSAGE, exception);
    }
}
