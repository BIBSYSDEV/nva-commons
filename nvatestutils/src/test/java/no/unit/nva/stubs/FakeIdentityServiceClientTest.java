package no.unit.nva.stubs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static no.unit.nva.stubs.FakeIdentityServiceClient.DEFAULT_PUBLICATION_WORKFLOW;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim;
import no.unit.nva.clients.ChannelClaimDto.ChannelClaim.ChannelConstraint;
import no.unit.nva.clients.ChannelClaimDto.CustomerSummaryDto;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceUnavailableException;
import no.unit.nva.clients.UserDto;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FakeIdentityServiceClientTest {

  private static final String UNAVAILABLE_MESSAGE_PREFIX = "Request to identity service failed: ";
  private static final String PLACEHOLDER_API_HOST = "https://localhost";

  @Test
  void shouldReturnDefaultCustomerWithRequestedIdForUnknownCustomer() throws NotFoundException {
    var customerId = randomUri();

    var customer = new FakeIdentityServiceClient().getCustomerById(customerId);

    assertEquals(customerId, customer.id());
    assertEquals(DEFAULT_PUBLICATION_WORKFLOW, customer.publicationWorkflow());
  }

  @Test
  void shouldUseApiHostFromProvidedEnvironmentInRequestUri() {
    var environment = mock(Environment.class);
    when(environment.readEnv(anyString())).thenReturn("api.example.org");
    var userName = randomString();
    var client = new FakeIdentityServiceClient(environment).withUnavailableIdentityService();

    var exception =
        assertThrows(IdentityServiceUnavailableException.class, () -> client.getUser(userName));

    assertEquals(
        UNAVAILABLE_MESSAGE_PREFIX + "https://api.example.org/users-roles/users/" + userName,
        exception.getMessage());
  }

  @Test
  void shouldReturnSameDefaultCustomerForRepeatedRequests() throws NotFoundException {
    var customerId = randomUri();
    var client = new FakeIdentityServiceClient();

    assertEquals(client.getCustomerById(customerId), client.getCustomerById(customerId));
  }

  @Test
  void shouldReturnDefaultCustomerWithConfiguredPublicationWorkflow() throws NotFoundException {
    var publicationWorkflow = randomString();
    var client =
        new FakeIdentityServiceClient().withDefaultPublicationWorkflow(publicationWorkflow);

    var customer = client.getCustomerById(randomUri());

    assertEquals(publicationWorkflow, customer.publicationWorkflow());
  }

  @Test
  void shouldReturnGivenCustomerForItsId() throws NotFoundException {
    var customer = randomCustomer();
    var client = new FakeIdentityServiceClient().withCustomer(customer.id(), customer);

    assertEquals(customer, client.getCustomerById(customer.id()));
  }

  @Test
  void shouldThrowNotFoundForMissingCustomer() {
    var customerId = randomUri();
    var client = new FakeIdentityServiceClient().withMissingCustomer(customerId);

    assertThrows(NotFoundException.class, () -> client.getCustomerById(customerId));
  }

  @Test
  void shouldThrowIdentityServiceUnavailableForUnavailableCustomer() {
    var customerId = randomUri();
    var client = new FakeIdentityServiceClient().withUnavailableCustomer(customerId);

    var exception =
        assertThrows(
            IdentityServiceUnavailableException.class, () -> client.getCustomerById(customerId));

    assertEquals(UNAVAILABLE_MESSAGE_PREFIX + customerId, exception.getMessage());
  }

  @Test
  void shouldReturnGivenCustomerByCristinId() throws NotFoundException {
    var customer = randomCustomer();
    var client = new FakeIdentityServiceClient().withCustomer(customer.id(), customer);

    assertEquals(customer, client.getCustomerByCristinId(customer.cristinId()));
  }

  @Test
  void shouldThrowNotFoundForUnknownCristinId() {
    var client = new FakeIdentityServiceClient();

    assertThrows(NotFoundException.class, () -> client.getCustomerByCristinId(randomUri()));
  }

  @Test
  void shouldReturnAllGivenCustomers() {
    var customer = randomCustomer();
    var client = new FakeIdentityServiceClient().withCustomer(customer.id(), customer);

    assertEquals(List.of(customer), client.getAllCustomers().customers());
  }

  @Test
  void shouldReturnGivenUser() throws NotFoundException {
    var user = randomUser();
    var client = new FakeIdentityServiceClient().withUser(user);

    assertEquals(user, client.getUser(user.username()));
  }

  @Test
  void shouldReturnGivenExternalClient() throws NotFoundException {
    var externalClient = randomExternalClient();
    var client = new FakeIdentityServiceClient().withExternalClient(externalClient);

    assertEquals(externalClient, client.getExternalClient(externalClient.getClientId()));
  }

  @Test
  void shouldReturnExternalClientGivenForToken() throws NotFoundException {
    var bearerToken = randomString();
    var externalClient = randomExternalClient();
    var client =
        new FakeIdentityServiceClient().withExternalClientForToken(bearerToken, externalClient);

    assertEquals(externalClient, client.getExternalClientByToken(bearerToken));
  }

  @Test
  void shouldReturnGivenChannelClaim() throws NotFoundException {
    var channelClaim = randomChannelClaim();
    var client = new FakeIdentityServiceClient().withChannelClaim(channelClaim);

    assertEquals(channelClaim, client.getChannelClaim(channelClaim.id()));
  }

  @Test
  void shouldThrowNotFoundForUnknownNonCustomerResources() {
    var client = new FakeIdentityServiceClient();

    assertThrows(NotFoundException.class, () -> client.getUser(randomString()));
    assertThrows(NotFoundException.class, () -> client.getExternalClient(randomString()));
    assertThrows(NotFoundException.class, () -> client.getExternalClientByToken(randomString()));
    assertThrows(NotFoundException.class, () -> client.getChannelClaim(randomUri()));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("operationsWithExpectedRequestUri")
  void shouldThrowIdentityServiceUnavailableWithRealRequestUriWhenServiceIsUnavailable(
      String operationName, Executable operation, URI expectedRequestUri) {
    var exception = assertThrows(IdentityServiceUnavailableException.class, operation);

    assertEquals(UNAVAILABLE_MESSAGE_PREFIX + expectedRequestUri, exception.getMessage());
  }

  private static Stream<Arguments> operationsWithExpectedRequestUri() {
    var client = new FakeIdentityServiceClient().withUnavailableIdentityService();
    var customerId = randomUri();
    var cristinId = randomUri();
    var userName = randomString();
    var clientId = randomString();
    var channelClaimId = randomUri();
    return Stream.of(
        Arguments.of(
            "getCustomerById", (Executable) () -> client.getCustomerById(customerId), customerId),
        Arguments.of(
            "getCustomerByCristinId",
            (Executable) () -> client.getCustomerByCristinId(cristinId),
            URI.create(
                PLACEHOLDER_API_HOST
                    + "/customer/cristinId/"
                    + URLEncoder.encode(cristinId.toString(), UTF_8))),
        Arguments.of(
            "getAllCustomers",
            (Executable) client::getAllCustomers,
            URI.create(PLACEHOLDER_API_HOST + "/customer")),
        Arguments.of(
            "getUser",
            (Executable) () -> client.getUser(userName),
            URI.create(PLACEHOLDER_API_HOST + "/users-roles/users/" + userName)),
        Arguments.of(
            "getExternalClient",
            (Executable) () -> client.getExternalClient(clientId),
            URI.create(PLACEHOLDER_API_HOST + "/users-roles/external-clients/" + clientId)),
        Arguments.of(
            "getExternalClientByToken",
            (Executable) () -> client.getExternalClientByToken(randomString()),
            URI.create(PLACEHOLDER_API_HOST + "/users-roles/external-clients")),
        Arguments.of(
            "getChannelClaim",
            (Executable) () -> client.getChannelClaim(channelClaimId),
            channelClaimId));
  }

  private static CustomerDto randomCustomer() {
    return new CustomerDto(
        randomUri(),
        UUID.randomUUID(),
        randomString(),
        randomString(),
        randomString(),
        randomUri(),
        randomString(),
        false,
        false,
        false,
        emptyList(),
        null,
        false,
        randomString());
  }

  private static UserDto randomUser() {
    return UserDto.builder().withUsername(randomString()).withCristinId(randomUri()).build();
  }

  private static GetExternalClientResponse randomExternalClient() {
    return new GetExternalClientResponse(randomString(), randomString(), randomUri(), randomUri());
  }

  private static ChannelClaimDto randomChannelClaim() {
    var channelClaimId = randomUri();
    return new ChannelClaimDto(
        channelClaimId,
        new CustomerSummaryDto(randomUri(), randomUri()),
        new ChannelClaim(
            randomUri(), new ChannelConstraint(randomString(), randomString(), emptyList())));
  }
}
