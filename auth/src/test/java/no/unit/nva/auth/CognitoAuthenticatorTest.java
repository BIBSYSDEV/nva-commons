package no.unit.nva.auth;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.auth.AuthorizedBackendClient.APPLICATION_X_WWW_FORM_URLENCODED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import no.unit.nva.auth.utils.HttpRequestMetadataMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CognitoAuthenticatorTest {

    public static String TEST_TOKEN =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJPbmxpbmUgSldUIEJ1aWxkZXIiLCJpYXQiOjE2Njg1MTE4NTcsImV4cCI6MTcw"
        + "MDA0Nzg1NywiYXVkIjoid3d3LmV4YW1wbGUuY29tIiwic3ViIjoianJvY2tldEBleGFtcGxlLmNvbSIsIkdpdmVuTmFtZSI6IkpvaG5ueSI"
        + "sIlN1cm5hbWUiOiJSb2NrZXQiLCJFbWFpbCI6Impyb2NrZXRAZXhhbXBsZS5jb20iLCJSb2xlIjoiTWFuYWdlciIsInNjb3BlIjoiZXhhbX"
        + "BsZS1zY29wZSJ9.ne8Jb4f2xao1zSJFZxIBRrh4WFNjkaBRV3-Ybp6fHZU";
    public static String TEST_SCOPE = "example-scope";
    final HttpClient httpClient = mock(HttpClient.class);
    HttpResponse<String> okResponse = mock(HttpResponse.class);
    HttpResponse<String> invalidResponse = mock(HttpResponse.class);
    HttpResponse<String> errorResponse = mock(HttpResponse.class);
    private CognitoCredentials credentials;
    private CognitoAuthenticator cognitoAuthenticator;

    @BeforeEach
    public void setup() {
        var authServer = "http://localhost";
        var clientId = randomString();
        var clientSecret = randomString();
        credentials = new CognitoCredentials(() -> clientId, () -> clientSecret, URI.create(authServer));
        cognitoAuthenticator = new CognitoAuthenticator(httpClient, credentials);

        when(okResponse.statusCode()).thenReturn(HTTP_OK);
        when(okResponse.body()).thenReturn("{\"access_token\": \"" + TEST_TOKEN + "\"}");

        when(invalidResponse.statusCode()).thenReturn(HTTP_OK);
        when(invalidResponse.body()).thenReturn("{}");

        when(errorResponse.statusCode()).thenReturn(HTTP_FORBIDDEN);
        when(errorResponse.body()).thenReturn("{}");
    }

    @Test
    void shouldReturnJwtTokenFromHttpRequestToCognito() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(), any())).thenReturn(okResponse);

        var jwt = cognitoAuthenticator.fetchBearerToken();
        assertThat(jwt.getToken(), is(TEST_TOKEN));
    }

    @Test
    void shouldReturnDecodedJwtWithClaims() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(), any())).thenReturn(okResponse);

        var jwt = cognitoAuthenticator.fetchBearerToken();
        assertThat(jwt.getClaim("scope").asString(), is(TEST_SCOPE));
    }

    @Test
    void shouldReturnDecodedJwtWhenSendingBasicAuthentication() throws IOException, InterruptedException {
        var uri = URI.create(credentials.getCognitoOAuthServerUri().toString() + "/oauth2/token");
        var usernamePassword = credentials.getCognitoAppClientId() + ":" + credentials.getCognitoAppClientSecret();
        var encodedAuth = Base64.getEncoder().encodeToString(usernamePassword.getBytes(StandardCharsets.UTF_8));

        var expectedRequest = HttpRequest.newBuilder()
                                  .uri(uri)
                                  .setHeader("Authorization", "Basic " + encodedAuth)
                                  .setHeader("Content-Type", APPLICATION_X_WWW_FORM_URLENCODED)
                                  .POST(BodyPublishers.noBody())
                                  .build();

        when(httpClient.<String>send(argThat(new HttpRequestMetadataMatcher(expectedRequest)), any()))
            .thenReturn(okResponse);

        var jwt = cognitoAuthenticator.fetchBearerToken();
        assertNotNull(jwt);
    }

    @Test
    void shouldThrowWhenResponseIsNotStructuredLikeAToken() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(), any())).thenReturn(invalidResponse);
        assertThrows(IllegalStateException.class, () -> cognitoAuthenticator.fetchBearerToken());
    }

    @Test
    void shouldThrowWhenResponseIsNot200Ok() throws IOException, InterruptedException {
        when(httpClient.<String>send(any(), any())).thenReturn(errorResponse);
        var exception = assertThrows(RuntimeException.class, () -> cognitoAuthenticator.fetchBearerToken());
        assertTrue(exception.getMessage().contains("Got unexpected http response"));
    }
}