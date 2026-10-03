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
import no.unit.nva.clients.CustomerDto.RightsRetentionStrategy;
import no.unit.nva.clients.CustomerList;
import no.unit.nva.clients.GetExternalClientResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.clients.IdentityServiceNotFoundException;
import no.unit.nva.clients.IdentityServiceUnavailableException;
import no.unit.nva.clients.UserDto;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

/**
 * In-memory {@link IdentityServiceClient} for tests. It starts empty: lookups return only what was
 * given with the {@code with...} methods and throw {@link IdentityServiceNotFoundException}
 * otherwise, and the inherited {@code find...} methods return an empty Optional.
 */
public class FakeIdentityServiceClient extends IdentityServiceClient {

  public static final String DEFAULT_PUBLICATION_WORKFLOW = "RegistratorPublishesMetadataOnly";
  public static final String DEFAULT_CUSTOMER_NAME = "Fake customer";
  public static final String DEFAULT_SECTOR = "UHI";
  public static final RightsRetentionStrategy DEFAULT_RIGHTS_RETENTION_STRATEGY =
      new RightsRetentionStrategy("NullRightsRetentionStrategy", null);
  private static final String API_HOST = "API_HOST";
  private static final String CUSTOMER_PATH = "customer";
  private static final String CRISTIN_PATH = "cristin";
  private static final String ORGANIZATION_PATH = "organization";
  private static final String TOP_LEVEL_ORGANIZATION_SUFFIX = ".0.0.0";
  private static final int CRISTIN_INSTITUTION_NUMBER_RANGE = 100_000;
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
   * Makes customer lookups return a default customer instead of not found. The missing ID or
   * Cristin ID is derived from the requested one, so repeated lookups return equal customers. The
   * two lookups are not linked and default customers are not in {@link #getAllCustomers()}, so use
   * {@link #withCustomer(URI, CustomerDto)} when a test needs related customers.
   */
  public FakeIdentityServiceClient withDefaultCustomers() {
    return withDefaultCustomers(DEFAULT_PUBLICATION_WORKFLOW);
  }

  /** Like {@link #withDefaultCustomers()}, with the given publication workflow. */
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

  /**
   * Makes every operation throw {@link IdentityServiceUnavailableException} for the request URI the
   * real client would use.
   */
  public FakeIdentityServiceClient withUnavailableIdentityService() {
    this.identityServiceUnavailable = true;
    return this;
  }

  @Override
  public CustomerDto getCustomerById(URI customerId) {
    throwIfUnavailable(customerId);
    if (customers.containsKey(customerId)) {
      return customers.get(customerId);
    }
    if (defaultCustomersEnabled) {
      return defaultCustomer(customerId);
    }
    throw notFound(customerId);
  }

  @Override
  public CustomerDto getCustomerByCristinId(URI topLevelOrgCristinId) {
    var requestUri = customerByCristinIdUri(topLevelOrgCristinId);
    throwIfUnavailable(requestUri);
    var givenCustomer =
        customers.values().stream()
            .filter(customer -> topLevelOrgCristinId.equals(customer.cristinId()))
            .findFirst();
    if (givenCustomer.isPresent()) {
      return givenCustomer.get();
    }
    if (defaultCustomersEnabled) {
      return defaultCustomerForCristinId(topLevelOrgCristinId);
    }
    throw notFound(requestUri);
  }

  /** Returns the customers given with {@link #withCustomer(URI, CustomerDto)}. */
  @Override
  public CustomerList getAllCustomers() {
    throwIfUnavailable(UriWrapper.fromHost(apiHost).addChild(CUSTOMER_PATH).getUri());
    return new CustomerList(List.copyOf(customers.values()));
  }

  @Override
  public UserDto getUser(String userName) {
    var requestUri = usersAndRolesUri().addChild(USERS_PATH).addChild(userName).getUri();
    return lookUp(users, userName, requestUri);
  }

  @Override
  public GetExternalClientResponse getExternalClient(String clientId) {
    var requestUri = usersAndRolesUri().addChild(EXTERNAL_CLIENTS_PATH).addChild(clientId).getUri();
    return lookUp(externalClients, clientId, requestUri);
  }

  @Override
  public GetExternalClientResponse getExternalClientByToken(String bearerToken) {
    var requestUri = usersAndRolesUri().addChild(EXTERNAL_CLIENTS_PATH).getUri();
    return lookUp(externalClientsByToken, bearerToken, requestUri);
  }

  @Override
  public ChannelClaimDto getChannelClaim(URI channelClaim) {
    return lookUp(channelClaims, channelClaim, channelClaim);
  }

  private <K, V> V lookUp(Map<K, V> values, K key, URI requestUri) {
    throwIfUnavailable(requestUri);
    if (!values.containsKey(key)) {
      throw notFound(requestUri);
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

  private static IdentityServiceNotFoundException notFound(URI requestUri) {
    return new IdentityServiceNotFoundException(
        requestUri, new NotFoundException("Simulated 404 from identity service"));
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
    var identifier = deterministicUuid(customerId);
    return defaultCustomer(customerId, identifier, derivedCristinId(identifier));
  }

  private URI derivedCristinId(UUID identifier) {
    var institutionNumber =
        Math.floorMod(identifier.getLeastSignificantBits(), CRISTIN_INSTITUTION_NUMBER_RANGE);
    return UriWrapper.fromHost(apiHost)
        .addChild(CRISTIN_PATH)
        .addChild(ORGANIZATION_PATH)
        .addChild(institutionNumber + TOP_LEVEL_ORGANIZATION_SUFFIX)
        .getUri();
  }

  private CustomerDto defaultCustomerForCristinId(URI cristinId) {
    var identifier = deterministicUuid(cristinId);
    var customerId =
        UriWrapper.fromHost(apiHost)
            .addChild(CUSTOMER_PATH)
            .addChild(identifier.toString())
            .getUri();
    return defaultCustomer(customerId, identifier, cristinId);
  }

  private static UUID deterministicUuid(URI source) {
    return UUID.nameUUIDFromBytes(source.toString().getBytes(UTF_8));
  }

  private CustomerDto defaultCustomer(URI customerId, UUID identifier, URI cristinId) {
    return new CustomerDto(
        customerId,
        identifier,
        DEFAULT_CUSTOMER_NAME,
        DEFAULT_CUSTOMER_NAME,
        DEFAULT_CUSTOMER_NAME,
        cristinId,
        defaultPublicationWorkflow,
        false,
        false,
        false,
        emptyList(),
        DEFAULT_RIGHTS_RETENTION_STRATEGY,
        false,
        DEFAULT_SECTOR);
  }

  /** Lets the fake be created without the environment variables the real client requires. */
  private static final class PlaceholderEnvironment extends Environment {

    @Override
    public Optional<String> readEnvOpt(String variableName) {
      return Optional.of("localhost");
    }
  }
}
