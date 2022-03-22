package no.unit.nva.auth;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.http.HttpClient;
import java.util.Map;
import no.unit.nva.stubs.FakeAuthServer;
import no.unit.nva.stubs.WiremockHttpClient;
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
    void shouldReturnUserInfoWhenForAccessToken() {
        var accessToken = randomString();
        var expectedUserInfo = UserInfo.builder().withCurrentCustomer(randomUri()).build();
        authServer.setUserBase(Map.of(accessToken, expectedUserInfo));
        var fetchUserInfo = new FetchUserInfo(httpClient, authServer.getServerUri(), bearerToken(accessToken));
        var actualUserInfo = fetchUserInfo.fetch();
        assertThat(actualUserInfo, is(equalTo(expectedUserInfo)));
    }

    @Test
    void shouldThrowExceptionWhenFailingToFetchUserInfo() {
        var accessToken = randomString();
        var expectedUserInfo = UserInfo.builder().withCurrentCustomer(randomUri()).build();
        authServer.setUserBase(Map.of(accessToken, expectedUserInfo));
        String unexpectedToken = randomString();
        var fetchUserInfo = new FetchUserInfo(httpClient, authServer.getServerUri(), bearerToken(unexpectedToken));
        var exception = assertThrows(RuntimeException.class, fetchUserInfo::fetch);
        assertThat(exception.getMessage(), containsString(FetchUserInfo.AUTHORIZATION_ERROR_MESSAGE));
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}