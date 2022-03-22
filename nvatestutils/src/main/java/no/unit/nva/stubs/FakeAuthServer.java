package no.unit.nva.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static nva.commons.core.attempt.Try.attempt;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.net.HttpHeaders;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import no.unit.nva.auth.UserInfo;
import no.unit.nva.commons.json.JsonUtils;

public class FakeAuthServer {

    public static final String OAUTH_USER_INFO = "/oauth2/userInfo";
    public static final String HTTPS = "https";
    private WireMockServer httpServer;
    private URI serverUri;
    private Map<String, UserInfo> accessTokenUserMap;

    public FakeAuthServer() {
        initialize();
    }

    public void close() {
        httpServer.stop();
    }

    public URI getServerUri() {
        return serverUri;
    }

    public void setUserBase(Map<String, UserInfo> accessTokenToUserMap) {
        this.accessTokenUserMap = accessTokenToUserMap;
        accessTokenUserMap.keySet().forEach(this::stubEndpointForUserEntry);
    }

    private FakeAuthServer initialize() {
        httpServer = new WireMockServer(options().httpDisabled(true).dynamicHttpsPort());

        httpServer.start();
        serverUri = URI.create(httpServer.baseUrl());
        WireMock.configureFor(HTTPS, serverUri.getHost(), httpServer.httpsPort());
        return this;
    }

    private void stubEndpointForUserEntry(String accessToken) {
        stubFor(get(OAUTH_USER_INFO)
                    .withHeader(HttpHeaders.AUTHORIZATION, equalTo(bearerToken(accessToken)))
                    .willReturn(createUserInfoResponse(accessToken))
        );
    }

    private ResponseDefinitionBuilder createUserInfoResponse(String accessToken) {
        return aResponse()
            .withBody(userInfoString(accessToken))
            .withStatus(HttpURLConnection.HTTP_OK);
    }

    private String userInfoString(String accessToken) {
        var userInfo = accessTokenUserMap.get(accessToken);
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(userInfo)).orElseThrow();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
