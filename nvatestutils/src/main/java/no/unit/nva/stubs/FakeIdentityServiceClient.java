package no.unit.nva.stubs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * <p>{@link #getCustomerById(URI)} returns a default customer for any customer ID, unless the ID
 * has been given its own customer, or made missing or unavailable. Every other operation only
 * returns what was given with the matching {@code with...} method and throws {@link
 * NotFoundException} for anything else. {@link #withUnavailableIdentityService()} makes every
 * operation throw {@link IdentityServiceUnavailableException}, with the same request URI the real
 * client would use.
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
  private final Set<URI> missingCustomers = new HashSet<>();
  private final Set<URI> unavailableCustomers = new HashSet<>();
  private final Map<String, UserDto> users = new HashMap<>();
  private final Map<String, GetExternalClientResponse> externalClients = new HashMap<>();
  private final Map<String, GetExternalClientResponse> externalClientsByToken = new HashMap<>();
  private final Map<URI, ChannelClaimDto> channelClaims = new HashMap<>();
  private String defaultPublicationWorkflow = DEFAULT_PUBLICATION_WORKFLOW;
  private boolean identityServiceUnavailable;

  public FakeIdentityServiceClient() {
    this(new PlaceholderEnvironment());
  }

  public FakeIdentityServiceClient(Environment environment) {
    super(null, environment);
    this.apiHost = environment.readEnv(API_HOST);
  }

  public FakeIdentityServiceClient withDefaultPublicationWorkflow(String publicationWorkflow) {
    this.defaultPublicationWorkflow = publicationWorkflow;
    return this;
  }

  public FakeIdentityServiceClient withCustomer(URI customerId, CustomerDto customer) {
    customers.put(customerId, customer);
    return this;
  }

  public FakeIdentityServiceClient withMissingCustomer(URI customerId) {
    missingCustomers.add(customerId);
    return this;
  }

  public FakeIdentityServiceClient withUnavailableCustomer(URI customerId) {
    unavailableCustomers.add(customerId);
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
    if (identityServiceUnavailable || unavailableCustomers.contains(customerId)) {
      throw unavailable(customerId);
    }
    if (missingCustomers.contains(customerId)) {
      throw new NotFoundException("Customer not found: " + customerId);
    }
    return customers.getOrDefault(customerId, defaultCustomer(customerId));
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
