package no.unit.nva.stubs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.clients.ChannelClaimDto;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.CustomerList;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.clients.IdentityServiceUnavailableException;
import no.unit.nva.clients.UserDto;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

/**
 * In-memory replacement for {@link IdentityServiceClient}.
 *
 * <p>The fake starts empty: every operation only returns what was given with the matching {@code
 * with...} method and throws {@link NotFoundException} for anything else. For tests that need some
 * customer but do not care which, {@link #withDefaultCustomers()} makes {@link
 * #getCustomerById(URI)} return a default customer for any customer ID that was not given its own.
 * {@link #withUnavailableIdentityService()} makes every operation throw {@link
 * IdentityServiceUnavailableException}, with the same request URI the real client would use.
 */
public class FakeIdentityServiceClient extends IdentityServiceClient {

  public static final String DEFAULT_PUBLICATION_WORKFLOW = "RegistratorPublishesMetadataOnly";
  public static final String DEFAULT_CUSTOMER_NAME = "Fake customer";
  private static final String API_HOST = "API_HOST";
  private static final String CUSTOMER_PATH = "customer";
  private static final String CRISTIN_ID_PATH = "cristinId";
  private static final String USERS_AND_ROLES_PATH = "users-roles";
  private static final String USERS_PATH = "users";
  private static final String EXTERNAL_CLIENTS_PATH = "external-clients";

  private final String apiHost;
  private final Map<URI, CustomerDto> customers = new HashMap<>();
  private final Map<String, UserDto> users = new HashMap<>();
  private final Map<String, GetExternalClientResponse> externalClients = new HashMap<>();
  private final Map<String, GetExternalClientResponse> externalClientsByToken = new HashMap<>();
  private final Map<URI, ChannelClaimDto> channelClaims = new HashMap<>();
  private boolean defaultCustomersEnabled;
  private String defaultPublicationWorkflow = DEFAULT_PUBLICATION_WORKFLOW;
  private boolean identityServiceUnavailable;

  public FakeIdentityServiceClient() {
    this(new PlaceholderEnvironment());
  }

  public FakeIdentityServiceClient(Environment environment) {
    super(null, environment);
    this.apiHost = environment.readEnv(API_HOST);
  }

  /**
   * Makes {@link #getCustomerById(URI)} return a default customer with the {@link
   * #DEFAULT_PUBLICATION_WORKFLOW} for any customer ID that was not given its own customer. Default
   * customers are not part of {@link #getAllCustomers()} or {@link #getCustomerByCristinId(URI)}.
   */
  public FakeIdentityServiceClient withDefaultCustomers() {
    return withDefaultCustomers(DEFAULT_PUBLICATION_WORKFLOW);
  }

  /**
   * Like {@link #withDefaultCustomers()}, with the given publication workflow on the default
   * customers.
   */
  public FakeIdentityServiceClient withDefaultCustomers(String publicationWorkflow) {
    this.defaultCustomersEnabled = true;
    this.defaultPublicationWorkflow = publicationWorkflow;
    return this;
  }

  public FakeIdentityServiceClient withCustomer(URI customerId, CustomerDto customer) {
    customers.put(customerId, customer);
    return this;
  }

  public FakeIdentityServiceClient withUser(UserDto user) {
    users.put(user.username(), user);
    return this;
  }

  public FakeIdentityServiceClient withExternalClient(GetExternalClientResponse externalClient) {
    externalClients.put(externalClient.getClientId(), externalClient);
    return this;
  }

  public FakeIdentityServiceClient withExternalClientForToken(
      String bearerToken, GetExternalClientResponse externalClient) {
    externalClientsByToken.put(bearerToken, externalClient);
    return this;
  }

  public FakeIdentityServiceClient withChannelClaim(ChannelClaimDto channelClaim) {
    channelClaims.put(channelClaim.id(), channelClaim);
    return this;
  }

  public FakeIdentityServiceClient withUnavailableIdentityService() {
    this.identityServiceUnavailable = true;
    return this;
  }

  @Override
  public CustomerDto getCustomerById(URI customerId) throws NotFoundException {
    throwIfUnavailable(customerId);
    if (customers.containsKey(customerId)) {
      return customers.get(customerId);
    }
    if (defaultCustomersEnabled) {
      return defaultCustomer(customerId);
    }
    throw new NotFoundException("Customer not found: " + customerId);
  }

  /** Looks up a customer given with {@link #withCustomer(URI, CustomerDto)} by its Cristin ID. */
  @Override
  public CustomerDto getCustomerByCristinId(URI topLevelOrgCristinId) throws NotFoundException {
    throwIfUnavailable(customerByCristinIdUri(topLevelOrgCristinId));
    return customers.values().stream()
        .filter(customer -> topLevelOrgCristinId.equals(customer.cristinId()))
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Customer not found for Cristin ID: " + topLevelOrgCristinId));
  }

  /** Returns the customers given with {@link #withCustomer(URI, CustomerDto)}. */
  @Override
  public CustomerList getAllCustomers() {
    throwIfUnavailable(UriWrapper.fromHost(apiHost).addChild(CUSTOMER_PATH).getUri());
    return new CustomerList(List.copyOf(customers.values()));
  }

  @Override
  public UserDto getUser(String userName) throws NotFoundException {
    throwIfUnavailable(usersAndRolesUri().addChild(USERS_PATH).addChild(userName).getUri());
    return findOrThrowNotFound(users, userName, "User not found: ");
  }

  @Override
  public GetExternalClientResponse getExternalClient(String clientId) throws NotFoundException {
    throwIfUnavailable(
        usersAndRolesUri().addChild(EXTERNAL_CLIENTS_PATH).addChild(clientId).getUri());
    return findOrThrowNotFound(externalClients, clientId, "External client not found: ");
  }

  @Override
  public GetExternalClientResponse getExternalClientByToken(String bearerToken)
      throws NotFoundException {
    throwIfUnavailable(usersAndRolesUri().addChild(EXTERNAL_CLIENTS_PATH).getUri());
    if (!externalClientsByToken.containsKey(bearerToken)) {
      throw new NotFoundException("External client not found for the given token");
    }
    return externalClientsByToken.get(bearerToken);
  }

  @Override
  public ChannelClaimDto getChannelClaim(URI channelClaim) throws NotFoundException {
    throwIfUnavailable(channelClaim);
    return findOrThrowNotFound(channelClaims, channelClaim, "Channel claim not found: ");
  }

  private static <K, V> V findOrThrowNotFound(Map<K, V> values, K key, String notFoundMessage)
      throws NotFoundException {
    if (!values.containsKey(key)) {
      throw new NotFoundException(notFoundMessage + key);
    }
    return values.get(key);
  }

  private void throwIfUnavailable(URI requestUri) {
    if (identityServiceUnavailable) {
      throw unavailable(requestUri);
    }
  }

  private static IdentityServiceUnavailableException unavailable(URI requestUri) {
    return new IdentityServiceUnavailableException(
        requestUri, new IOException("Simulated identity service outage"));
  }

  private UriWrapper usersAndRolesUri() {
    return UriWrapper.fromHost(apiHost).addChild(USERS_AND_ROLES_PATH);
  }

  private URI customerByCristinIdUri(URI topLevelOrgCristinId) {
    var customerByCristinIdPath =
        UriWrapper.fromHost(apiHost).addChild(CUSTOMER_PATH).addChild(CRISTIN_ID_PATH).getUri();
    return URI.create(
        customerByCristinIdPath + "/" + URLEncoder.encode(topLevelOrgCristinId.toString(), UTF_8));
  }

  private CustomerDto defaultCustomer(URI customerId) {
    return new CustomerDto(
        customerId,
        UUID.nameUUIDFromBytes(customerId.toString().getBytes(UTF_8)),
        DEFAULT_CUSTOMER_NAME,
        DEFAULT_CUSTOMER_NAME,
        DEFAULT_CUSTOMER_NAME,
        null,
        defaultPublicationWorkflow,
        false,
        false,
        false,
        emptyList(),
        null,
        false,
        null);
  }

  /** Lets the fake be created without the environment variables the real client requires. */
  private static final class PlaceholderEnvironment extends Environment {

    @Override
    public Optional<String> readEnvOpt(String variableName) {
      return Optional.of("localhost");
    }
  }
}
