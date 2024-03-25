package no.unit.nva.auth;

import static no.unit.nva.auth.OAuthConstants.OAUTH_USER_INFO;
import static no.unit.nva.stubs.FakeAuthServer.ACCESS_TOKEN_FORBIDDEN;
import static no.unit.nva.stubs.FakeAuthServer.FORBIDDEN_BODY;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;
import no.unit.nva.stubs.FakeAuthServer;
import no.unit.nva.stubs.WiremockHttpClient;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FetchUserInfoTest {

    private FakeAuthServer authServer;
    private HttpClient httpClient;

    @BeforeEach
    public void init() {
        this.authServer = new FakeAuthServer();
        this.httpClient = WiremockHttpClient.create();
    }

    @AfterEach
    public void close() throws InterruptedException {
        this.authServer.close();
        Thread.sleep(100);
    }

    @Test
    void shouldReturnUserInfoWhenRequestWithAccessToken() {
        var accessToken = randomString();
        var expectedUserInfo = CognitoUserInfo.builder().withCurrentCustomer(randomUri()).build();
        authServer.setUserBase(Map.of(accessToken, expectedUserInfo));
        var fetchUserInfo =
            new FetchUserInfo(httpClient, () -> createCognitoUri(), bearerToken(accessToken));
        var actualUserInfo = fetchUserInfo.fetch();
        assertThat(actualUserInfo, is(equalTo(expectedUserInfo)));
    }

    @Test
    void shouldLogResponseDetailsWhenHttpError() {
        var fetchUserInfo =
            new FetchUserInfo(httpClient, this::createCognitoUri, bearerToken(ACCESS_TOKEN_FORBIDDEN));

        var testAppender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(RuntimeException.class, fetchUserInfo::fetch);

        assertThat(testAppender.getMessages(), containsString("Got status code 403"));
        assertThat(testAppender.getMessages(), containsString(FORBIDDEN_BODY));
    }

    @Test
    void shouldThrowExceptionWhenFailingToFetchUserInfo() {
        var accessToken = randomString();
        var expectedUserInfo = CognitoUserInfo.builder().withCurrentCustomer(randomUri()).build();
        authServer.setUserBase(Map.of(accessToken, expectedUserInfo));
        String unexpectedToken = randomString();
        var fetchUserInfo =
            new FetchUserInfo(httpClient, () -> createCognitoUri(), bearerToken(unexpectedToken));
        var exception = assertThrows(RuntimeException.class, fetchUserInfo::fetch);
        assertThat(exception.getMessage(), containsString(FetchUserInfo.AUTHORIZATION_ERROR_MESSAGE));
    }

    private URI createCognitoUri() {
        return UriWrapper.fromUri(authServer.getServerUri()).addChild(OAUTH_USER_INFO).getUri();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}