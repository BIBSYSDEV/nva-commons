package no.unit.nva.auth;

import static nva.commons.core.attempt.Try.attempt;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchUserInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchUserInfo.class);
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_ERROR_MESSAGE = "Could not authorize user";
    private final HttpClient httpClient;
    private final Supplier<URI> cognitoUri;
    private final String authorizationHeader;

    public FetchUserInfo(HttpClient httpClient, Supplier<URI> cognitoUri, String authorizationHeader) {
        this.httpClient = httpClient;
        this.cognitoUri = cognitoUri;
        this.authorizationHeader = authorizationHeader;
    }

    public CognitoUserInfo fetch() {
        var request = HttpRequest.newBuilder(cognitoUri.get())
                          .header(AUTHORIZATION_HEADER, authorizationHeader)
                          .GET()
                          .build();
        return attempt(() -> httpClient.send(request, BodyHandlers.ofString()))
                   .map(this::responseIsSuccessful)
                   .map(HttpResponse::body)
                   .map(CognitoUserInfo::fromString)
                   .orElseThrow(fail -> handleFailure(request.uri(), fail.getException()));
    }

    private HttpResponse<String> responseIsSuccessful(HttpResponse<String> response) {
        if (HttpURLConnection.HTTP_OK != response.statusCode()) {
            LOGGER.error("Failed to look up UserInfo from Cognito. Got status code {} with body '{}'",
                         response.statusCode(), response.body());
            throw new RuntimeException(AUTHORIZATION_ERROR_MESSAGE);
        }
        return response;
    }

    private RuntimeException handleFailure(URI uri, Exception exception) {
        LOGGER.error("Failed to look up UserInfo from Cognito (" + uri + ")", exception);
        return new RuntimeException(AUTHORIZATION_ERROR_MESSAGE, exception);
    }
}
