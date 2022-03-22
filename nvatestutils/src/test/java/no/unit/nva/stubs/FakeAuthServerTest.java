package no.unit.nva.stubs;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.google.common.net.HttpHeaders;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import no.unit.nva.auth.CognitoUserInfo;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FakeAuthServerTest {

    public static final String OAUTH_USER_INFO_ENDPOINT = "oauth2/userInfo";
    private FakeAuthServer authServer;
    private HttpClient httpClient;

    @BeforeEach
    public void init() {
        this.authServer = new FakeAuthServer();
        this.httpClient = WiremockHttpClient.create();
    }

    @AfterEach
    public void close() {
        this.authServer.close();
    }

    @Test
    void shouldReturnUserInfoWhenReceivingGetUserInfoRequestWithAccessToken() throws IOException, InterruptedException {
        var expectedUserInfo = CognitoUserInfo.builder().withCurrentCustomer(randomUri()).build();
        var userAccessToken = randomString();
        authServer.setUserBase(Map.of(userAccessToken, expectedUserInfo));
        var getUri = UriWrapper.fromUri(authServer.getServerUri()).addChild(OAUTH_USER_INFO_ENDPOINT).getUri();
        var request = HttpRequest.newBuilder(getUri)
            .header(HttpHeaders.AUTHORIZATION, bearerToken(userAccessToken))
            .build();
        var response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        var actualUserInfo = CognitoUserInfo.fromString(response.body());
        assertThat(actualUserInfo, is(equalTo(expectedUserInfo)));
    }

    private String bearerToken(String userAccessToken) {
        return "Bearer " + userAccessToken;
    }
}