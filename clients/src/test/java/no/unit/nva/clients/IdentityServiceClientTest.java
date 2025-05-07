package no.unit.nva.clients;

import static no.unit.nva.auth.FetchUserInfo.AUTHORIZATION_HEADER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.clients.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.clients.CustomerDto.RightsRetentionStrategy;
import no.unit.nva.clients.UserDto.Role;
import no.unit.nva.clients.UserDto.ViewingScope;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

class IdentityServiceClientTest {

    public static final String BEARER_TOKEN = "Bearer 123";
    public static final String clientId = randomString();
    public static final String actingUser = randomString();
    public static final URI customer = randomUri();
    public static final URI cristinOrgUri = randomUri();
    public static final String BEARER_BEARER_TOKEN_TEST = "Bearer BEARER_TOKEN_TEST";
    HttpClient httpClient = mock(HttpClient.class);
    CognitoCredentials cognitoCredentials;
    HttpResponse<String> okResponseWithBody = mock(HttpResponse.class);
    HttpResponse<String> notOkResponse = mock(HttpResponse.class);
    HttpResponse<String> notFoundResponse = mock(HttpResponse.class);
    private IdentityServiceClient authorizedIdentityServiceClient;

    @SuppressWarnings("unchecked")
    static HttpResponse<String> mockResponse(String body) {
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        return response;
    }

    @BeforeEach
    void setup() throws IOException, InterruptedException {
        cognitoCredentials = new CognitoCredentials(() -> "id", () -> "secret", URI.create("https://backend-auth/"));

        when(okResponseWithBody.statusCode()).thenReturn(500);
        when(okResponseWithBody.body()).thenReturn("");

        when(notFoundResponse.statusCode()).thenReturn(404);
        when(notFoundResponse.body()).thenReturn("");

        var response = new GetExternalClientResponse(clientId, actingUser, customer, cristinOrgUri);
        when(okResponseWithBody.statusCode()).thenReturn(200);
        when(okResponseWithBody.body()).thenReturn(response.toString());

        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(okResponseWithBody);

        authorizedIdentityServiceClient = new IdentityServiceClient(httpClient, BEARER_TOKEN, cognitoCredentials);
    }

    @Test
    void shouldSendRequestToCorrectUrlWhenGettingExternalClients()
        throws IOException, InterruptedException, NotFoundException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
            .thenAnswer((Answer) invocation -> {
                Object[] args = invocation.getArguments();
                HttpRequest request = (HttpRequest) args[0];
                var path = request.uri().getPath();
                if (path.equals("/users-roles/external-clients/" + clientId)) {
                    return okResponseWithBody;
                }
                return null;
            });

        var externalClient = authorizedIdentityServiceClient.getExternalClient(clientId);
        assertNotNull(externalClient);
    }

    @Test
    void shouldReturnExternalClientWhenRequested() throws NotFoundException {

        var externalClient = authorizedIdentityServiceClient.getExternalClient(clientId);

        assertThat(externalClient.getClientId(), is(equalTo(clientId)));
        assertThat(externalClient.getActingUser(), is(equalTo(actingUser)));
        assertThat(externalClient.getCustomerUri(), is(equalTo(customer)));
        assertThat(externalClient.getCristinUrgUri(), is(equalTo(cristinOrgUri)));
    }

    @Test
    void shouldReturnUserWhenRequested() throws NotFoundException, IOException, InterruptedException {
        var userName = "userName";
        var expectedUser = createUser(userName);
        var mockedResponse = mockResponse(expectedUser.toJsonString());
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(mockedResponse);
        var actual = authorizedIdentityServiceClient.getUser(userName);
        assertEquals(expectedUser, actual);
    }

    @Test
    void shouldThrowNotFoundWhenUserNotFound() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(notFoundResponse);
        assertThrows(NotFoundException.class, () -> authorizedIdentityServiceClient.getUser(randomString()));
    }

    @Test
    void shouldSendRequestToCorrectUrlWhenGettingUser()
        throws IOException, InterruptedException, NotFoundException {
        var userName = "userName";
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
            .thenAnswer((Answer) invocation -> {
                Object[] args = invocation.getArguments();
                HttpRequest request = (HttpRequest) args[0];
                var path = request.uri().getPath();
                if (path.equals("/users-roles/users/" + userName)) {
                    return okResponseWithBody;
                }
                return null;
            });

        var user = authorizedIdentityServiceClient.getUser(userName);
        assertNotNull(user);
    }

    @Test
    void shouldReturnExternalClientWhenRequestedWithBearerToken()
        throws NotFoundException, IOException, InterruptedException {

        var externalClient = authorizedIdentityServiceClient.getExternalClientByToken(BEARER_BEARER_TOKEN_TEST);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any(BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        var actualAuthorizationHeader = request.headers().firstValue(AUTHORIZATION_HEADER).orElse("");

        assertThat(actualAuthorizationHeader, is(equalTo(BEARER_BEARER_TOKEN_TEST)));
        assertThat(externalClient.getClientId(), is(equalTo(clientId)));
        assertThat(externalClient.getActingUser(), is(equalTo(actingUser)));
        assertThat(externalClient.getCustomerUri(), is(equalTo(customer)));
        assertThat(externalClient.getCristinUrgUri(), is(equalTo(cristinOrgUri)));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenHttpClientReturnsUnhandledError() throws IOException,
                                                                                 InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(notOkResponse);

        Executable action = () -> authorizedIdentityServiceClient.getExternalClient(clientId);

        assertThrows(RuntimeException.class, action);
    }

    @Test
    void shouldThrowNotFoundWhenHttpClientNotFound() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(notFoundResponse);

        Executable action = () -> authorizedIdentityServiceClient.getExternalClient(clientId);

        assertThrows(NotFoundException.class, action);
    }

    @Test
    void shouldReturnCustomerByCristinIdWhenRequested() throws NotFoundException, IOException, InterruptedException {
        var customerCristinId = randomUri();
        var expectedCustomer = createCustomer(customerCristinId);
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(createFetchCustomerByCristinIdUri(customerCristinId))
                          .build();

        when(okResponseWithBody.body()).thenReturn(expectedCustomer.toJsonString());
        when(okResponseWithBody.statusCode()).thenReturn(200);
        when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).thenReturn(okResponseWithBody);

        var actual = authorizedIdentityServiceClient.getCustomerByCristinId(customerCristinId);

        assertEquals(expectedCustomer, actual);
    }

    @Test
    void shouldThrowNotFoundWhenFetchingCustomerByCristinIdReturnsNotFound() throws IOException, InterruptedException {
        var topLevelOrgCristinId = randomUri();

        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(createFetchCustomerByCristinIdUri(topLevelOrgCristinId))
                          .build();

        when(okResponseWithBody.body()).thenReturn(null);
        when(okResponseWithBody.statusCode()).thenReturn(404);
        when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).thenReturn(okResponseWithBody);

        assertThrows(NotFoundException.class, () -> authorizedIdentityServiceClient.getCustomerByCristinId(
            topLevelOrgCristinId));
    }

    @Test
    void shouldReturnCustomerByIdWhenRequested() throws NotFoundException, IOException, InterruptedException {
        var customerId = randomCustomerId();
        var expectedCustomer = createCustomerWithCristinId(customerId);
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(customerId)
                          .build();

        when(okResponseWithBody.body()).thenReturn(expectedCustomer.toJsonString());
        when(okResponseWithBody.statusCode()).thenReturn(200);
        when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).thenReturn(okResponseWithBody);

        var actual = authorizedIdentityServiceClient.getCustomerById(customerId);

        assertEquals(expectedCustomer, actual);
    }

    private static URI randomCustomerId() {
        return randomBackendUri("customer");
    }

    private static URI randomBackendUri(String path) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild(path)
                   .addChild(randomString())
                   .getUri();
    }

    @Test
    void shouldThrowNotFoundWhenFetchingCustomerByIdReturnsNotFound() throws IOException, InterruptedException {
        var customerId = randomCustomerId();

        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(customerId)
                          .build();

        when(okResponseWithBody.body()).thenReturn(null);
        when(okResponseWithBody.statusCode()).thenReturn(404);
        when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).thenReturn(okResponseWithBody);

        assertThrows(NotFoundException.class, () -> authorizedIdentityServiceClient.getCustomerById(
            customerId));
    }

    @Test
    void shouldReturnAllCustomers() throws IOException, InterruptedException, ApiGatewayException {
        var customerList = new CustomerList(List.of(createCustomer(randomCustomerId()), createCustomer(randomUri())));
        var uri = randomUri();
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(uri)
                          .build();

        when(okResponseWithBody.body()).thenReturn(JsonUtils.dtoObjectMapper.writeValueAsString(customerList));
        when(okResponseWithBody.statusCode()).thenReturn(200);
        when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).thenReturn(okResponseWithBody);

        var actual = authorizedIdentityServiceClient.getAllCustomers();

        assertEquals(customerList, actual);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenFetchingAllCustomersFails() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(randomUri())
                          .build();

        when(okResponseWithBody.body()).thenReturn(null);
        when(okResponseWithBody.statusCode()).thenReturn(502);
        when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).thenReturn(okResponseWithBody);

        assertThrows(RuntimeException.class, () -> authorizedIdentityServiceClient.getAllCustomers());
    }

    @Test
    void shouldReturnChannelClaimByIdWhenRequested() throws NotFoundException, IOException, InterruptedException {
        var channelClaim = randomBackendUri("customer/channel-claim");
        var expectedChannelClaim = channelClaimWithId(channelClaim);
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(channelClaim)
                          .build();

        when(okResponseWithBody.body()).thenReturn(expectedChannelClaim.toJsonString());
        when(okResponseWithBody.statusCode()).thenReturn(200);
        when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).thenReturn(okResponseWithBody);

        var actual = authorizedIdentityServiceClient.getChannelClaim(channelClaim);

        assertEquals(expectedChannelClaim, actual);
    }

    @Test
    void shouldThrowNotFoundWhenIdentityServiceRespondsWithNoFoundWhenFetchingChannelClaim() throws IOException,
                                                                                                     InterruptedException {
        var channelClaim = randomBackendUri("customer/channel-claim");
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(channelClaim)
                          .build();

        when(okResponseWithBody.statusCode()).thenReturn(404);
        when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).thenReturn(okResponseWithBody);

        assertThrows(NotFoundException.class, () -> authorizedIdentityServiceClient.getChannelClaim(channelClaim));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenUnhandledExceptionWhenFetchingChannelClaim() throws IOException, InterruptedException {
        var channelClaim = randomUri();
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(channelClaim)
                          .build();

        when(okResponseWithBody.statusCode()).thenReturn(500);
        when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).thenReturn(okResponseWithBody);

        assertThrows(RuntimeException.class, () -> authorizedIdentityServiceClient.getChannelClaim(channelClaim));
    }

    private ChannelClaimDto channelClaimWithId(URI channelClaim) {
        return new ChannelClaimDto(channelClaim, new CustomerSummaryDto(
            randomUri(), randomUri()), new ChannelClaim(channelClaim,
                                                        new ChannelConstraint(randomString(), randomString(),
                                                                                                       List.of(randomString(), randomString()))));
    }

    private static URI createFetchCustomerByCristinIdUri(URI customerCristinId) {
        var fetchCustomerUri = UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                                   .addChild("customer")
                                   .addChild("cristinId")
                                   .getUri();
        return URI.create(fetchCustomerUri + "/" + URLEncoder.encode(customerCristinId.toString(),
                                                                     StandardCharsets.UTF_8));
    }

    private CustomerDto createCustomerWithCristinId(URI customerCristinId) {
        return new CustomerDto(randomUri(), UUID.randomUUID(), randomString(), randomString(), randomString(),
                               customerCristinId, null, false, false, false, null,
                               new RightsRetentionStrategy(randomString(), randomUri()));
    }

    private CustomerDto createCustomer(URI customerId) {
        return new CustomerDto(customerId, UUID.randomUUID(), randomString(), randomString(), randomString(),
                               randomUri(), null, false, false, false, null, null);
    }

    private UserDto createUser(String userName) {
        return UserDto.builder()
                   .withUsername(userName)
                   .withInstitution(randomUri())
                   .withGivenName("Test")
                   .withFamilyName("Testing")
                   .withViewingScope(ViewingScope.builder()
                                         .withType("ViewingScope")
                                         .withIncludedUnits(List.of(randomUri()))
                                         .withExcludedUnits(List.of(randomUri()))
                                         .build())
                   .withRoles(List.of(Role.builder()
                                          .withRolename("Publishing-Curator")
                                          .withAccessRights(List.of("MANAGE_PUBLISHING_REQUESTS"))
                                          .withType("Role")
                                          .build()))
                   .withCristinId(randomUri())
                   .withFeideIdentifier("feideIdentifier")
                   .withInstitutionCristinId(randomUri())
                   .withAffiliation(randomUri())
                   .withType("User")
                   .withAccessRights(List.of("MANAGE_PUBLISHING_REQUESTS"))
                   .build();
    }
}