package no.unit.nva.clients;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static no.unit.nva.auth.FetchUserInfo.AUTHORIZATION_HEADER;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.concurrent.Callable;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import software.amazon.awssdk.http.HttpStatusCode;

public class IdentityServiceClient {

    public static final String CREDENTIALS_SECRET_NAME = "BackendCognitoClientCredentials";
    public static final String API_PATH_USERS_AND_ROLES = "users-roles";
    public static final String API_PATH_EXTERNAL_CLIENTS = "external-clients";
    private static final String API_PATH_USERS = "users";
    private static final String AUTH_HOST = new Environment().readEnv("BACKEND_CLIENT_AUTH_URL");
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    public static final String CUSTOMER_PATH_PARAM = "customer";
    public static final String CRISTIN_ID_PATH_PARAM = "cristinId";
    public static final String OPERATION_REQUIRES_AUTHORIZED_CLIENT_MESSAGE = "This operation requires an authorized client";
    private final AuthorizedBackendClient authorizedClient;
    private final HttpClient unauthorizedClient;

    /**
     * Creates an IdentityServiceClient with authorization support.
     * This client can make both authorized and unauthorized API calls.
     *
     * @param httpClient the HTTP client to use for requests
     * @param bearerToken the bearer token for authorization
     * @param cognitoCredentials the credentials for backend authentication
     */
    public IdentityServiceClient(HttpClient httpClient,
                                 String bearerToken,
                                 CognitoCredentials cognitoCredentials) {
        this.unauthorizedClient = httpClient;
        this.authorizedClient = AuthorizedBackendClient.prepareWithBearerTokenAndCredentials(
            httpClient,
            bearerToken,
            cognitoCredentials
        );
    }

    /**
     * Creates an IdentityServiceClient without authorization support.
     * This client can only make unauthorized API calls.
     * Methods requiring authorization will throw {@link IllegalStateException}.
     *
     * @param httpClient the HTTP client to use for requests
     */
    public IdentityServiceClient(HttpClient httpClient) {
        this.unauthorizedClient = httpClient;
        this.authorizedClient = null;
    }

    /**
     * Creates an IdentityServiceClient with authorization support using default credentials.
     *
     * @return a configured IdentityServiceClient with authorization
     */
    @JacocoGenerated
    public static IdentityServiceClient prepare() {
        var credentials = fetchCredentials();
        return new IdentityServiceClient(HttpClient.newBuilder().build(), null, credentials);
    }

    /**
     * Creates an IdentityServiceClient without authorization support.
     * The returned client can only be used for API calls that do not require authorization.
     *
     * @return a configured IdentityServiceClient without authorization
     */
    @JacocoGenerated
    public static IdentityServiceClient unauthorizedIdentityServiceClient() {
        return new IdentityServiceClient(HttpClient.newBuilder().build());
    }

    /**
     * Retrieves an external client by client ID.
     * Requires an authorized client.
     *
     * @param clientId the client ID to retrieve
     * @return the external client response
     * @throws NotFoundException if the client is not found
     * @throws IllegalStateException if the client was created without authorization support
     */
    public GetExternalClientResponse getExternalClient(String clientId) throws NotFoundException {
        var request = getRequestBuilderFromUri(constructExternalClientsGetPath(clientId));
        return attempt(getAuthorizedHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(response -> mapResponse(GetExternalClientResponse.class, response))
                   .orElseThrow(this::handleFailure);
    }

    /**
     * Retrieves an external client using a bearer token.
     * Does not require an authorized client.
     *
     * @param bearerToken the bearer token to use for authentication
     * @return the external client response
     * @throws NotFoundException if the client is not found
     */
    public GetExternalClientResponse getExternalClientByToken(String bearerToken)
        throws NotFoundException {
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(constructExternalClientsUserinfoGetPath())
                          .setHeader(AUTHORIZATION_HEADER, bearerToken);

        return attempt(getUnauthorizedHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(response -> mapResponse(GetExternalClientResponse.class, response))
                   .orElseThrow(this::handleFailure);
    }

    /**
     * Retrieves a user by username.
     * Requires an authorized client.
     *
     * @param userName the username to retrieve
     * @return the user data
     * @throws NotFoundException if the user is not found
     * @throws IllegalStateException if the client was created without authorization support
     */
    public UserDto getUser(String userName) throws NotFoundException {
        var request = getRequestBuilderFromUri(constructUserGetPath(userName));
        return attempt(getAuthorizedHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(response -> mapResponse(UserDto.class, response))
                   .orElseThrow(this::handleFailure);
    }

    /**
     * Retrieves a customer by Cristin ID.
     * Requires an authorized client.
     *
     * @param topLevelOrgCristinId the Cristin ID of the top-level organization
     * @return the customer data
     * @throws NotFoundException if the customer is not found
     * @throws IllegalStateException if the client was created without authorization support
     */
    public CustomerDto getCustomerByCristinId(URI topLevelOrgCristinId) throws NotFoundException {
        var request = getRequestBuilderFromUri(constructCustomerGetPath(topLevelOrgCristinId));
        return attempt(getAuthorizedHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(response -> mapResponse(CustomerDto.class, response))
                   .orElseThrow(this::handleFailure);
    }

    /**
     * Retrieves a customer by customer ID.
     * Requires an authorized client.
     *
     * @param customerId the customer ID URI
     * @return the customer data
     * @throws NotFoundException if the customer is not found
     * @throws IllegalStateException if the client was created without authorization support
     */
    public CustomerDto getCustomerById(URI customerId) throws NotFoundException {
        var request = getRequestBuilderFromUri(customerId);
        return attempt(getAuthorizedHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(response -> mapResponse(CustomerDto.class, response))
                   .orElseThrow(this::handleFailure);
    }

    /**
     * Retrieves a channel claim.
     * Does not require an authorized client.
     *
     * @param channelClaim the channel claim URI
     * @return the channel claim data
     * @throws NotFoundException if the channel claim is not found
     */
    public ChannelClaimDto getChannelClaim(URI channelClaim) throws NotFoundException {
        var request = getRequestBuilderFromUri(channelClaim);
        return attempt(getUnauthorizedHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(response -> mapResponse(ChannelClaimDto.class, response))
                   .orElseThrow(this::handleFailure);
    }

    private static Builder getRequestBuilderFromUri(URI uri) {
        return HttpRequest.newBuilder()
                   .GET()
                   .uri(uri);
    }

    /**
     * Retrieves all customers.
     * Does not require an authorized client.
     *
     * @return a list of all customers
     * @throws ApiGatewayException if the request fails
     */
    public CustomerList getAllCustomers() throws ApiGatewayException {
        var request = getRequestBuilderFromUri(constructListCustomerUri());
        return attempt(getUnauthorizedHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(HttpResponse::body)
                   .map(value -> dtoObjectMapper.readValue(value, CustomerList.class))
                   .orElseThrow(this::handleFailure);
    }

    private URI constructListCustomerUri() {
        return UriWrapper.fromHost(API_HOST).addChild(CUSTOMER_PATH_PARAM).getUri();
    }

    private URI constructCustomerGetPath(URI topLevelOrgCristinId) {
        var customerByCristinIdUri = UriWrapper.fromHost(API_HOST)
                                    .addChild(CUSTOMER_PATH_PARAM)
                                    .addChild(CRISTIN_ID_PATH_PARAM)
                                    .getUri();
        return URI.create(
            customerByCristinIdUri + "/" + URLEncoder.encode(topLevelOrgCristinId.toString(), UTF_8));
    }

    @JacocoGenerated
    private static CognitoCredentials fetchCredentials() {
        var secretsReader = new SecretsReader(SecretsReader.defaultSecretsManagerClient());

        var credentials = secretsReader.fetchClassSecret(CREDENTIALS_SECRET_NAME,
                                                         BackendClientCredentials.class);
        var uri = UriWrapper.fromHost(AUTH_HOST).getUri();
        return new CognitoCredentials(credentials::getId, credentials::getSecret, uri);
    }

    private UriWrapper usersAndRolesURI() {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(API_PATH_USERS_AND_ROLES);
    }

    private URI constructExternalClientsGetPath(String clientId) {
        return usersAndRolesURI()
                   .addChild(API_PATH_EXTERNAL_CLIENTS)
                   .addChild(clientId)
                   .getUri();
    }

    private URI constructUserGetPath(String userName) {
        return usersAndRolesURI()
                   .addChild(API_PATH_USERS)
                   .addChild(userName)
                   .getUri();
    }

    private URI constructExternalClientsUserinfoGetPath() {
        return usersAndRolesURI()
                   .addChild(API_PATH_EXTERNAL_CLIENTS)
                   .getUri();
    }

    private <T> T mapResponse(Class<T> clazz, HttpResponse<String> response)
        throws JsonProcessingException {
        return dtoObjectMapper.readValue(
            response.body(),
            clazz);
    }

    private NotFoundException handleFailure(Failure<?> responseFailure) {
        var exception = responseFailure.getException();
        if (exception instanceof NotFoundException) {
            return new NotFoundException(exception);
        }

        throw new RuntimeException("Something went wrong!");
    }

    private <S> HttpResponse<String> validateResponse(HttpResponse<String> response) throws NotFoundException {
        if (response.statusCode() == HttpStatusCode.NOT_FOUND) {
            throw new NotFoundException("Client not found");
        }

        if (response.statusCode() != HttpStatusCode.OK) {
            throw new IllegalStateException("Received " + response.statusCode() + " from identity service");
        }
        return response;
    }


    private Callable<HttpResponse<String>> getAuthorizedHttpResponseCallable(HttpRequest.Builder request) {
        if (isNull(authorizedClient)) {
            throw new IllegalStateException(OPERATION_REQUIRES_AUTHORIZED_CLIENT_MESSAGE);
        }
        return () -> authorizedClient.send(request, ofString(UTF_8));
    }

    private Callable<HttpResponse<String>> getUnauthorizedHttpResponseCallable(HttpRequest.Builder request) {
        return () -> unauthorizedClient.send(request.build(), ofString(UTF_8));
    }
}