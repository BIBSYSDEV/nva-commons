package no.unit.nva.auth;

import static no.unit.nva.auth.AuthorizedBackendClient.prepareWithBearerToken;
import static no.unit.nva.auth.AuthorizedBackendClient.prepareWithCognitoCredentials;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import no.unit.nva.stubs.FakeAuthServer;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthorizedBackendClientTest {

    public static final String EXAMPLE_RESOURCE_PATH = "/example";
    private URI serverUri;
    private HttpClient httpClient;
    private String protectedContent;
    private String expectedAccessToken;
    private CognitoCredentials cognitoCredentials;
    private FakeAuthServer authServer;

    @BeforeEach
    public void init() {
        authServer = new FakeAuthServer();
        expectedAccessToken = createTestJwt(Instant.now().plus(Duration.ofMinutes(5)));
        var clientId = randomString();
        var clientSecret = randomString();
        cognitoCredentials = new CognitoCredentials(() -> clientId, () -> clientSecret, authServer.getServerUri());
        protectedContent = authServer.createHttpInteractions(cognitoCredentials.getCognitoAppClientId(),
                                                             cognitoCredentials.getCognitoAppClientSecret(),
                                                             expectedAccessToken,
                                                             EXAMPLE_RESOURCE_PATH);
        serverUri = authServer.getServerUri();
        this.httpClient = WiremockHttpClient.create();
    }

    @Test
    void shouldSendRequestsContainingTheBackendAccessTokenWhenUserAccessTokenIsNotSubmitted()
        throws IOException, InterruptedException {
        var client = prepareWithCognitoCredentials(httpClient, cognitoCredentials);
        var request = buildRequest();
        var response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.body(), containsString(protectedContent));
    }

    @Test
    void shouldSendAsyncRequestsContainingTheAccessTokenWhenUserAccessTokenIsNotSubmitted() {
        var client = prepareWithCognitoCredentials(httpClient, cognitoCredentials);
        var request = buildRequest();
        var response =
            client.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8)).join();
        assertThat(response.body(), containsString(protectedContent));
    }

    @Test
    void shouldThrowUnexpectedHttpResponseExceptionIfAuthServerReturnsForbidden() {
        authServer.createOAuthAccessTokenResponseForbidden(
            cognitoCredentials.getCognitoAppClientId(),
            cognitoCredentials.getCognitoAppClientSecret()
        );

        var client = prepareWithCognitoCredentials(httpClient, cognitoCredentials);
        var request = buildRequest();
        var exception = assertThrows(UnexpectedHttpResponseException.class,
                                     () -> client.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8))
                                               .join());
        assertThat(exception.getMessage(), containsString("403"));
    }

    @Test
    void shouldThrowIllegalStateExceptionIfAuthServerReturns200okWithoutToken() {
        authServer.createOAuthAccessTokenResponseMissingToken(
            cognitoCredentials.getCognitoAppClientId(),
            cognitoCredentials.getCognitoAppClientSecret()
        );

        var client = prepareWithCognitoCredentials(httpClient, cognitoCredentials);
        var request = buildRequest();
        assertThrows(IllegalStateException.class,
                     () -> client.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8)).join());
    }

    @Test
    void shouldSendRequestsContainingTheUserAccessTokenWhenUserAccessTokenIsSubmitted()
        throws IOException, InterruptedException {
        String bearerToken = "Bearer " + expectedAccessToken;
        var client = AuthorizedBackendClient.prepareWithBearerToken(httpClient, bearerToken);
        var request = buildRequest();

        var response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.body(), containsString(protectedContent));

        var asyncResponse =
            client.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8)).join();
        assertThat(asyncResponse.body(), containsString(protectedContent));
    }

    @Test
    void shouldRefreshTheBackendAccessTokenWhenTokenIsExpiredAndTheAccessTokenHasNotBeenManuallyInjected()
        throws IOException, InterruptedException {
        var client = new AuthorizedBackendClient(httpClient, null, cognitoCredentials);
        var request = buildRequest();
        var firstToken = createTestJwt(Instant.now().minus(Duration.ofMinutes(1)));
        authClientReturnsAnotherAccessTokenOnTheNextCall(firstToken);

        client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));

        var actualFirstToken = client.getBearerToken();
        var secondToken = createTestJwt(Instant.now().plus(Duration.ofMinutes(1)));
        authClientReturnsAnotherAccessTokenOnTheNextCall(secondToken);

        client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));

        var actualSecondToken = client.getBearerToken();
        assertThat(actualSecondToken, is(not(equalTo(actualFirstToken))));
    }

    @Test
    void shouldUseCachedBackendAccessTokenWhenCachedTokenIsNotExpiredAndTokenHasNotBeenManuallyInjected()
        throws IOException, InterruptedException {
        var client = prepareWithCognitoCredentials(httpClient, cognitoCredentials);
        var request = buildRequest();

        client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        var firstToken = client.getBearerToken();

        client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        var secondToken = client.getBearerToken();
        assertEquals(firstToken, secondToken);
    }

    @Test
    void shouldNotRefreshTheAccessTokenWhenTheAccessTokenHasBeenManuallyInjected()
        throws IOException, InterruptedException {
        String bearerToken = "Bearer " + expectedAccessToken;
        var client = prepareWithBearerToken(httpClient, bearerToken);
        var request = buildRequest();

        client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        var firstToken = client.getBearerToken();

        client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        var secondToken = client.getBearerToken();

        assertThat(secondToken, is(equalTo(firstToken)));
    }

    private static String createTestJwt(Instant expiresAt) {
        return JWT.create()
                   .withExpiresAt(Date.from(expiresAt))
                   .sign(Algorithm.none());
    }

    private void authClientReturnsAnotherAccessTokenOnTheNextCall(String accessToken) {
        protectedContent = authServer.createHttpInteractions(cognitoCredentials.getCognitoAppClientId(),
                                                             cognitoCredentials.getCognitoAppClientSecret(),
                                                             accessToken,
                                                             EXAMPLE_RESOURCE_PATH);
    }

    private HttpRequest.Builder buildRequest() {
        var resourceUri = UriWrapper.fromUri(serverUri).addChild(EXAMPLE_RESOURCE_PATH).getUri();
        return HttpRequest.newBuilder(resourceUri).GET();
    }
}