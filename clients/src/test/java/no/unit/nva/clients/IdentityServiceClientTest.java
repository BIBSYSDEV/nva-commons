package no.unit.nva.clients;

import static no.unit.nva.auth.FetchUserInfo.AUTHORIZATION_HEADER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.clients.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.clients.CustomerDto.RightsRetentionStrategy;
import no.unit.nva.clients.UserDto.Role;
import no.unit.nva.clients.UserDto.ViewingScope;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

class IdentityServiceClientTest {

  public static final String BEARER_TOKEN = "Bearer 123";
  public static final String clientId = randomString();
  public static final String actingUser = randomString();
  public static final URI customer = randomUri();
  public static final URI cristinOrgUri = randomUri();
  public static final String BEARER_BEARER_TOKEN_TEST = "Bearer BEARER_TOKEN_TEST";
  public static final String OPERATION_REQUIRES_AN_AUTHORIZED_CLIENT_MESSAGE =
      "This operation requires an authorized client";
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
    cognitoCredentials =
        new CognitoCredentials(() -> "id", () -> "secret", URI.create("https://backend-auth/"));

    when(okResponseWithBody.statusCode()).thenReturn(500);
    when(okResponseWithBody.body()).thenReturn("");

    when(notFoundResponse.statusCode()).thenReturn(404);
    when(notFoundResponse.body()).thenReturn("");

    var response = new GetExternalClientResponse(clientId, actingUser, customer, cristinOrgUri);
    when(okResponseWithBody.statusCode()).thenReturn(200);
    when(okResponseWithBody.body()).thenReturn(response.toString());

    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenReturn(okResponseWithBody);

    authorizedIdentityServiceClient =
        new IdentityServiceClient(httpClient, BEARER_TOKEN, cognitoCredentials);
  }

  @Test
  void shouldSendRequestToCorrectUrlWhenGettingExternalClients()
      throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenAnswer(
            (Answer)
                invocation -> {
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
  void shouldReturnExternalClientWhenRequested() {

    var externalClient = authorizedIdentityServiceClient.getExternalClient(clientId);

    assertThat(externalClient.getClientId(), is(equalTo(clientId)));
    assertThat(externalClient.getActingUser(), is(equalTo(actingUser)));
    assertThat(externalClient.getCustomerUri(), is(equalTo(customer)));
    assertThat(externalClient.getCristinUrgUri(), is(equalTo(cristinOrgUri)));
  }

  @Test
  void shouldReturnUserWhenRequested() throws IOException, InterruptedException {
    var userName = "userName";
    var expectedUser = createUser(userName);
    var mockedResponse = mockResponse(expectedUser.toJsonString());
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenReturn(mockedResponse);
    var actual = authorizedIdentityServiceClient.getUser(userName);
    assertEquals(expectedUser, actual);
  }

  @Test
  void shouldThrowNotFoundWhenUserNotFound() throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenReturn(notFoundResponse);
    assertThrows(
        IdentityServiceNotFoundException.class,
        () -> authorizedIdentityServiceClient.getUser(randomString()));
  }

  @Test
  void shouldSendRequestToCorrectUrlWhenGettingUser() throws IOException, InterruptedException {
    var userName = "userName";
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenAnswer(
            (Answer)
                invocation -> {
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
      throws IOException, InterruptedException {

    var externalClient =
        authorizedIdentityServiceClient.getExternalClientByToken(BEARER_BEARER_TOKEN_TEST);

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
  void shouldThrowIdentityServiceRequestFailedWhenHttpClientReturnsUnhandledError()
      throws IOException, InterruptedException {
    when(notOkResponse.statusCode()).thenReturn(500);
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(notOkResponse);

    Executable action = () -> authorizedIdentityServiceClient.getExternalClient(clientId);

    var exception = assertThrows(IdentityServiceRequestFailedException.class, action);
    assertInstanceOf(IllegalStateException.class, exception.getCause());
    assertTrue(exception.getMessage().contains("/users-roles/external-clients/" + clientId));
  }

  @Test
  void shouldThrowIdentityServiceRequestFailedWithCauseWhenHttpClientThrowsIOException()
      throws IOException, InterruptedException {
    var networkError = new IOException("Connection reset");
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenThrow(networkError);

    var exception =
        assertThrows(
            IdentityServiceRequestFailedException.class,
            () -> authorizedIdentityServiceClient.getUser(randomString()));

    assertEquals(networkError, exception.getCause());
  }

  @Test
  void shouldThrowIdentityServiceRequestFailedWhenResponseBodyCannotBeParsed()
      throws IOException, InterruptedException {
    var customerId = randomCustomerId();
    var unparsableResponse = mockResponse("not json");
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenReturn(unparsableResponse);

    var exception =
        assertThrows(
            IdentityServiceRequestFailedException.class,
            () -> authorizedIdentityServiceClient.getCustomerById(customerId));

    assertInstanceOf(JsonProcessingException.class, exception.getCause());
    assertEquals(customerId, exception.getRequestUri());
    assertTrue(exception.getMessage().contains(customerId.toString()));
  }

  @Test
  void shouldThrowNotFoundWhenHttpClientNotFound() throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenReturn(notFoundResponse);

    Executable action = () -> authorizedIdentityServiceClient.getExternalClient(clientId);

    var exception = assertThrows(IdentityServiceNotFoundException.class, action);
    assertNull(exception.getCause());
    assertTrue(exception.getMessage().contains("/users-roles/external-clients/" + clientId));
  }

  @Test
  void shouldFindCustomerById() throws IOException, InterruptedException {
    var customer = createCustomer(randomCustomerId());
    stubResponseBody(customer.toJsonString());

    var actual = authorizedIdentityServiceClient.findCustomerById(customer.id());

    assertEquals(Optional.of(customer), actual);
  }

  @Test
  void shouldFindCustomerByCristinId() throws IOException, InterruptedException {
    var customer = createCustomer(randomCustomerId());
    stubResponseBody(customer.toJsonString());

    var actual = authorizedIdentityServiceClient.findCustomerByCristinId(customer.cristinId());

    assertEquals(Optional.of(customer), actual);
  }

  @Test
  void shouldFindUser() throws IOException, InterruptedException {
    var user = createUser(randomString());
    stubResponseBody(user.toJsonString());

    var actual = authorizedIdentityServiceClient.findUser(user.username());

    assertEquals(Optional.of(user), actual);
  }

  @Test
  void shouldFindChannelClaim() throws IOException, InterruptedException {
    var channelClaim = channelClaimWithId(randomBackendUri("customer/channel-claim"));
    stubResponseBody(channelClaim.toJsonString());

    var actual = authorizedIdentityServiceClient.findChannelClaim(channelClaim.id());

    assertEquals(Optional.of(channelClaim), actual);
  }

  @Test
  void shouldFindExternalClient() {
    var actual = authorizedIdentityServiceClient.findExternalClient(clientId);

    assertEquals(clientId, actual.orElseThrow().getClientId());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("finders")
  void shouldReturnEmptyFromFinderWhenIdentityServiceRespondsNotFound(
      String finderName, Function<IdentityServiceClient, Optional<?>> finder)
      throws IOException, InterruptedException {
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenReturn(notFoundResponse);

    assertEquals(Optional.empty(), finder.apply(authorizedIdentityServiceClient));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("finders")
  void shouldThrowIdentityServiceRequestFailedFromFinderWhenRequestFails(
      String finderName, Function<IdentityServiceClient, Optional<?>> finder)
      throws IOException, InterruptedException {
    when(notOkResponse.statusCode()).thenReturn(500);
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(notOkResponse);

    assertThrows(
        IdentityServiceRequestFailedException.class,
        () -> finder.apply(authorizedIdentityServiceClient));
  }

  private static Stream<Arguments> finders() {
    return Stream.of(
        finder("findCustomerById", client -> client.findCustomerById(randomCustomerId())),
        finder("findCustomerByCristinId", client -> client.findCustomerByCristinId(randomUri())),
        finder("findUser", client -> client.findUser(randomString())),
        finder("findChannelClaim", client -> client.findChannelClaim(randomUri())),
        finder("findExternalClient", client -> client.findExternalClient(randomString())));
  }

  private static Arguments finder(
      String finderName, Function<IdentityServiceClient, Optional<?>> finder) {
    return Arguments.of(finderName, finder);
  }

  private void stubResponseBody(String body) throws IOException, InterruptedException {
    var response = mockResponse(body);
    when(httpClient.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
  }

  @Test
  void shouldReturnCustomerByCristinIdWhenRequested() throws IOException, InterruptedException {
    var customerCristinId = randomUri();
    var expectedCustomer = createCustomer(customerCristinId);
    var request =
        HttpRequest.newBuilder()
            .GET()
            .uri(createFetchCustomerByCristinIdUri(customerCristinId))
            .build();

    when(okResponseWithBody.body()).thenReturn(expectedCustomer.toJsonString());
    when(okResponseWithBody.statusCode()).thenReturn(200);
    when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
        .thenReturn(okResponseWithBody);

    var actual = authorizedIdentityServiceClient.getCustomerByCristinId(customerCristinId);

    assertEquals(expectedCustomer, actual);
  }

  @Test
  void shouldThrowNotFoundWhenFetchingCustomerByCristinIdReturnsNotFound()
      throws IOException, InterruptedException {
    var topLevelOrgCristinId = randomUri();

    var request =
        HttpRequest.newBuilder()
            .GET()
            .uri(createFetchCustomerByCristinIdUri(topLevelOrgCristinId))
            .build();

    when(okResponseWithBody.body()).thenReturn(null);
    when(okResponseWithBody.statusCode()).thenReturn(404);
    when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
        .thenReturn(okResponseWithBody);

    assertThrows(
        IdentityServiceNotFoundException.class,
        () -> authorizedIdentityServiceClient.getCustomerByCristinId(topLevelOrgCristinId));
  }

  @Test
  void shouldReturnCustomerByIdWhenRequested() throws IOException, InterruptedException {
    var customerId = randomCustomerId();
    var expectedCustomer = createCustomerWithCristinId(customerId);
    var request = HttpRequest.newBuilder().GET().uri(customerId).build();

    when(okResponseWithBody.body()).thenReturn(expectedCustomer.toJsonString());
    when(okResponseWithBody.statusCode()).thenReturn(200);
    when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
        .thenReturn(okResponseWithBody);

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
  void shouldThrowNotFoundWhenFetchingCustomerByIdReturnsNotFound()
      throws IOException, InterruptedException {
    var customerId = randomCustomerId();

    var request = HttpRequest.newBuilder().GET().uri(customerId).build();

    when(okResponseWithBody.body()).thenReturn(null);
    when(okResponseWithBody.statusCode()).thenReturn(404);
    when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
        .thenReturn(okResponseWithBody);

    var exception =
        assertThrows(
            IdentityServiceNotFoundException.class,
            () -> authorizedIdentityServiceClient.getCustomerById(customerId));

    assertEquals(customerId, exception.getRequestUri());
  }

  @Test
  void shouldReturnAllCustomers() throws IOException, InterruptedException {
    var customerList =
        new CustomerList(List.of(createCustomer(randomCustomerId()), createCustomer(randomUri())));
    var uri = randomUri();
    var request = HttpRequest.newBuilder().GET().uri(uri).build();

    when(okResponseWithBody.body())
        .thenReturn(JsonUtils.dtoObjectMapper.writeValueAsString(customerList));
    when(okResponseWithBody.statusCode()).thenReturn(200);
    when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
        .thenReturn(okResponseWithBody);

    var actual = authorizedIdentityServiceClient.getAllCustomers();

    assertEquals(customerList, actual);
  }

  @Test
  void shouldThrowIdentityServiceRequestFailedWhenFetchingAllCustomersFails()
      throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder().GET().uri(randomUri()).build();

    when(okResponseWithBody.body()).thenReturn(null);
    when(okResponseWithBody.statusCode()).thenReturn(502);
    when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
        .thenReturn(okResponseWithBody);

    assertThrows(
        IdentityServiceRequestFailedException.class,
        () -> authorizedIdentityServiceClient.getAllCustomers());
  }

  @Test
  void shouldReturnChannelClaimByIdWhenRequested() throws IOException, InterruptedException {
    var channelClaim = randomBackendUri("customer/channel-claim");
    var expectedChannelClaim = channelClaimWithId(channelClaim);
    var request = HttpRequest.newBuilder().GET().uri(channelClaim).build();

    when(okResponseWithBody.body()).thenReturn(expectedChannelClaim.toJsonString());
    when(okResponseWithBody.statusCode()).thenReturn(200);
    when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
        .thenReturn(okResponseWithBody);

    var actual = authorizedIdentityServiceClient.getChannelClaim(channelClaim);

    assertEquals(expectedChannelClaim, actual);
  }

  @Test
  void shouldThrowNotFoundWhenIdentityServiceRespondsWithNoFoundWhenFetchingChannelClaim()
      throws IOException, InterruptedException {
    var channelClaim = randomBackendUri("customer/channel-claim");
    var request = HttpRequest.newBuilder().GET().uri(channelClaim).build();

    when(okResponseWithBody.statusCode()).thenReturn(404);
    when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
        .thenReturn(okResponseWithBody);

    assertThrows(
        IdentityServiceNotFoundException.class,
        () -> authorizedIdentityServiceClient.getChannelClaim(channelClaim));
  }

  @Test
  void shouldThrowIdentityServiceRequestFailedWhenUnhandledExceptionWhenFetchingChannelClaim()
      throws IOException, InterruptedException {
    var channelClaim = randomUri();
    var request = HttpRequest.newBuilder().GET().uri(channelClaim).build();

    when(okResponseWithBody.statusCode()).thenReturn(500);
    when(httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
        .thenReturn(okResponseWithBody);

    assertThrows(
        IdentityServiceRequestFailedException.class,
        () -> authorizedIdentityServiceClient.getChannelClaim(channelClaim));
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenFetchingCustomerWithUnauthorizedHttpClient() {
    var service = new IdentityServiceClient(mock(HttpClient.class));
    var throwable =
        assertThrows(IllegalStateException.class, () -> service.getCustomerById(randomUri()));

    assertEquals(OPERATION_REQUIRES_AN_AUTHORIZED_CLIENT_MESSAGE, throwable.getMessage());
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenFetchingUserWithUnauthorizedHttpClient() {
    var service = new IdentityServiceClient(mock(HttpClient.class));
    var throwable =
        assertThrows(IllegalStateException.class, () -> service.getUser(randomString()));

    assertEquals(OPERATION_REQUIRES_AN_AUTHORIZED_CLIENT_MESSAGE, throwable.getMessage());
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenFetchingCustomerByCristinIdWithUnauthorizedHttpClient() {
    var service = new IdentityServiceClient(mock(HttpClient.class));
    var throwable =
        assertThrows(
            IllegalStateException.class, () -> service.getCustomerByCristinId(randomUri()));

    assertEquals(OPERATION_REQUIRES_AN_AUTHORIZED_CLIENT_MESSAGE, throwable.getMessage());
  }

  @Test
  void shouldThrowIllegalStateExceptionWhenFetchingExternalClientWithUnauthorizedHttpClient() {
    var service = new IdentityServiceClient(mock(HttpClient.class));
    var throwable =
        assertThrows(IllegalStateException.class, () -> service.getExternalClient(randomString()));

    assertEquals(OPERATION_REQUIRES_AN_AUTHORIZED_CLIENT_MESSAGE, throwable.getMessage());
  }

  private ChannelClaimDto channelClaimWithId(URI channelClaim) {
    return new ChannelClaimDto(
        channelClaim,
        new CustomerSummaryDto(randomUri(), randomUri()),
        new ChannelClaim(
            channelClaim,
            new ChannelConstraint(
                randomString(), randomString(), List.of(randomString(), randomString()))));
  }

  private static URI createFetchCustomerByCristinIdUri(URI customerCristinId) {
    var fetchCustomerUri =
        UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
            .addChild("customer")
            .addChild("cristinId")
            .getUri();
    return URI.create(
        fetchCustomerUri
            + "/"
            + URLEncoder.encode(customerCristinId.toString(), StandardCharsets.UTF_8));
  }

  private CustomerDto createCustomerWithCristinId(URI customerCristinId) {
    return new CustomerDto(
        randomUri(),
        UUID.randomUUID(),
        randomString(),
        randomString(),
        randomString(),
        customerCristinId,
        null,
        false,
        false,
        false,
        null,
        new RightsRetentionStrategy(randomString(), randomUri()),
        true,
        randomString());
  }

  private CustomerDto createCustomer(URI customerId) {
    return new CustomerDto(
        customerId,
        UUID.randomUUID(),
        randomString(),
        randomString(),
        randomString(),
        randomUri(),
        null,
        false,
        false,
        false,
        null,
        null,
        false,
        randomString());
  }

  private UserDto createUser(String userName) {
    return UserDto.builder()
        .withUsername(userName)
        .withInstitution(randomUri())
        .withGivenName("Test")
        .withFamilyName("Testing")
        .withViewingScope(
            ViewingScope.builder()
                .withType("ViewingScope")
                .withIncludedUnits(List.of(randomUri()))
                .withExcludedUnits(List.of(randomUri()))
                .build())
        .withRoles(
            List.of(
                Role.builder()
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
