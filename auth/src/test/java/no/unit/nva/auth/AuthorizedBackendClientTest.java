package no.unit.nva.auth;

import static no.unit.nva.auth.AuthorizedBackendClient.CLIENT_ID;
import static no.unit.nva.auth.AuthorizedBackendClient.CLIENT_SECRET;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
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

    @BeforeEach
    public void init() {
        FakeAuthServer authServer = new FakeAuthServer();
        var expectedAccessToken = randomString();
        protectedContent = authServer.addBackendClient(CLIENT_ID,
                                                       CLIENT_SECRET,
                                                       expectedAccessToken,
                                                       EXAMPLE_RESOURCE_PATH);
        serverUri = authServer.getServerUri();
        this.httpClient = WiremockHttpClient.create();
    }

    @Test
    void shouldSendRequestsContainingTheAccessToken() throws IOException, InterruptedException {
        var client = AuthorizedBackendClient.create(serverUri, httpClient);
        var resourceUri = UriWrapper.fromUri(serverUri).addChild(EXAMPLE_RESOURCE_PATH).getUri();
        var request = HttpRequest.newBuilder(resourceUri).GET();
        var response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.body(), containsString(protectedContent));
    }

    @Test
    void shouldSendAsyncRequestsContainingTheAccessToken() {
        var client = AuthorizedBackendClient.create(serverUri, httpClient);
        var resourceUri = UriWrapper.fromUri(serverUri).addChild(EXAMPLE_RESOURCE_PATH).getUri();
        var request = HttpRequest.newBuilder(resourceUri).GET();
        var response =
            client.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8)).join();
        assertThat(response.body(), containsString(protectedContent));
    }
}