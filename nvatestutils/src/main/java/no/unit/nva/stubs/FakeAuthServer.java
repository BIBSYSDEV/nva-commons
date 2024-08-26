package no.unit.nva.stubs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.auth.AuthorizedBackendClient.AUTHORIZATION_HEADER;
import static no.unit.nva.auth.OAuthConstants.HTTPS;
import static no.unit.nva.auth.OAuthConstants.OAUTH_TOKEN;
import static no.unit.nva.auth.OAuthConstants.OAUTH_USER_INFO;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.util.Map;
import no.unit.nva.auth.CognitoUserInfo;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.JacocoGenerated;

public class FakeAuthServer {

    public static final String ACCESS_TOKEN_TEMPLATE = """
        {"access_token": "%s","expires_in": %s}
        """;
    public static final String ACCESS_TOKEN_FORBIDDEN = randomString();
    public static final String FORBIDDEN_BODY = randomString();
    private WireMockServer httpServer;
    private URI serverUri;
    private Map<String, CognitoUserInfo> accessTokenUserMap;

    public FakeAuthServer() {
        initialize();
        stubForbidden();
    }

    public void close() {
        httpServer.stop();
    }

    public URI getServerUri() {
        return serverUri;
    }

    public void setUserBase(Map<String, CognitoUserInfo> accessTokenToUserMap) {
        this.accessTokenUserMap = accessTokenToUserMap;
        accessTokenUserMap.keySet().forEach(this::stubEndpointForUserEntry);
    }

    public String createHttpInteractions(String clientId,
                                         String clientSecret,
                                         String expectedAccessToken,
                                         String exampleResourcePath,
                                         int expectedExpiresIn) {
        createOAuthAccessTokenResponse(clientId, clientSecret, expectedAccessToken, expectedExpiresIn);
        return createResponseForProtectedContent(expectedAccessToken, exampleResourcePath);
    }

    public String createResponseForProtectedContent(String expectedAccessToken, String exampleResourcePath) {
        var protectedContent = randomString();
        stubFor(get(exampleResourcePath)
                    .withHeader(AUTHORIZATION_HEADER, new EqualToPattern("Bearer " + expectedAccessToken))
                    .willReturn(aResponse().withBody(protectedContent).withStatus(HTTP_OK)));
        return protectedContent;
    }

    public void createOAuthAccessTokenResponse(String clientId, String clientSecret, String expectedAccessToken,
                                               int expectedExpiresIn) {
        var body = format(ACCESS_TOKEN_TEMPLATE, expectedAccessToken, expectedExpiresIn);
        stubFor(post(OAUTH_TOKEN)
                    .withBasicAuth(clientId, clientSecret)
                    .withRequestBody(new ContainsPattern("grant_type=client_credentials"))
                    .willReturn(createOauthClientResponse(body, HTTP_OK)));
    }

    @JacocoGenerated
    public void createOAuthAccessTokenResponseForbidden(String clientId, String clientSecret) {
        stubFor(post(OAUTH_TOKEN)
                    .withBasicAuth(clientId, clientSecret)
                    .withRequestBody(new ContainsPattern("grant_type=client_credentials"))
                    .willReturn(createOauthClientResponse("{}", HTTP_FORBIDDEN)));
    }

    @JacocoGenerated
    public void createOAuthAccessTokenResponseMissingToken(String clientId, String clientSecret) {
        stubFor(post(OAUTH_TOKEN)
                    .withBasicAuth(clientId, clientSecret)
                    .withRequestBody(new ContainsPattern("grant_type=client_credentials"))
                    .willReturn(createOauthClientResponse("{}", HTTP_OK)));
    }

    private void initialize() {
        httpServer = new WireMockServer(options().httpDisabled(true).dynamicHttpsPort());
        httpServer.start();
        serverUri = URI.create(httpServer.baseUrl());
        WireMock.configureFor(HTTPS, serverUri.getHost(), httpServer.httpsPort());
    }

    private void stubEndpointForUserEntry(String accessToken) {
        stubFor(get(OAUTH_USER_INFO)
                    .withHeader(HttpHeaders.AUTHORIZATION, equalTo(bearerToken(accessToken)))
                    .willReturn(createUserInfoResponse(accessToken))
        );
    }

    private void stubForbidden() {
        stubFor(get(OAUTH_USER_INFO)
                    .withHeader(HttpHeaders.AUTHORIZATION, equalTo(bearerToken(ACCESS_TOKEN_FORBIDDEN)))
                    .willReturn(createForbiddenResponse())
        );
    }

    private ResponseDefinitionBuilder createForbiddenResponse() {
        return aResponse()
                   .withStatus(HTTP_FORBIDDEN)
                   .withBody(FORBIDDEN_BODY);
    }

    private ResponseDefinitionBuilder createOauthClientResponse(String body, int statusCode) {
        return aResponse()
                   .withStatus(statusCode)
                   .withBody(body);
    }

    private ResponseDefinitionBuilder createUserInfoResponse(String accessToken) {
        return aResponse()
                   .withBody(userInfoString(accessToken))
                   .withStatus(HTTP_OK);
    }

    private String userInfoString(String accessToken) {
        var userInfo = accessTokenUserMap.get(accessToken);
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(userInfo)).orElseThrow();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }
}
