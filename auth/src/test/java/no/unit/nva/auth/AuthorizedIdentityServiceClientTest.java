package no.unit.nva.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.stubbing.Answer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizedIdentityServiceClientTest {

    public static final String BEARER_TOKEN = "Bearer 123";
    public static final String clientId = randomString();
    public static final URI customer = randomUri();
    HttpClient httpClient = mock(HttpClient.class);
    CognitoCredentials cognitoCredentials;
    HttpResponse<String> okResponseWithBody = mock(HttpResponse.class);
    HttpResponse<String> notOkResponse = mock(HttpResponse.class);
    HttpResponse<String> notFoundResponse = mock(HttpResponse.class);
    private AuthorizedIdentityServiceClient authorizedIdentityServiceClient;

    @BeforeEach
    private void setup() throws IOException, InterruptedException {
        cognitoCredentials = new CognitoCredentials(() -> "id", () -> "secret", URI.create("https://backend-auth/"));

        when(okResponseWithBody.statusCode()).thenReturn(500);
        when(okResponseWithBody.body()).thenReturn("");

        when(notFoundResponse.statusCode()).thenReturn(404);
        when(notFoundResponse.body()).thenReturn("");

        when(okResponseWithBody.statusCode()).thenReturn(200);
        when(okResponseWithBody.body()).thenReturn(new GetExternalClientResponse(clientId, customer).toString());

        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(okResponseWithBody);

        authorizedIdentityServiceClient = new AuthorizedIdentityServiceClient(httpClient, BEARER_TOKEN, cognitoCredentials);
    }

    @Test
    public void shouldSendRequestToCorrectUrlWhenGettingExternalClients() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
            .thenAnswer((Answer) invocation -> {
                Object[] args = invocation.getArguments();
                HttpRequest request = (HttpRequest) args[0];
                var path = request.uri().getPath();
                if (path.equals("/users-roles/external-clients/" + clientId)) return okResponseWithBody;
                return null;
            });

        var externalClient = authorizedIdentityServiceClient.getExternalClient(clientId);
        assertNotNull(externalClient);
    }

    @Test
    public void shouldReturnExternalClientWhenRequested() throws IOException, InterruptedException {

        var externalClient = authorizedIdentityServiceClient.getExternalClient(clientId);

        assertThat(externalClient.getClientId(), is(equalTo(clientId)));
        assertThat(externalClient.getCustomer(), is(equalTo(customer)));
    }

    @Test
    public void shouldThrowWhenHttpClientReturnsError() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(notOkResponse);

        Executable action = () -> authorizedIdentityServiceClient.getExternalClient(clientId);

        assertThrows(IllegalStateException.class, action);
    }

    @Test
    public void shouldThrowWhenHttpClientNotFound() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(notFoundResponse);

        Executable action = () -> authorizedIdentityServiceClient.getExternalClient(clientId);

        assertThrows(IllegalStateException.class, action);
    }

}