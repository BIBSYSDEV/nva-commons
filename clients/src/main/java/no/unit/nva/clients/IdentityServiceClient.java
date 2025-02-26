package no.unit.nva.clients;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
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
    private final AuthorizedBackendClient authorizedClient;
    private final HttpClient unauthorizedClient;

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

    @JacocoGenerated
    public static IdentityServiceClient prepare() {
        var credentials = fetchCredentials();
        return new IdentityServiceClient(HttpClient.newBuilder().build(), null, credentials);
    }

    public GetExternalClientResponse getExternalClient(String clientId) throws NotFoundException {
        var request = getRequestBuilderFromUri(constructExternalClientsGetPath(clientId));
        return attempt(getHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(r -> mapResponse(GetExternalClientResponse.class, r))
                   .orElseThrow(this::handleFailure);
    }

    public GetExternalClientResponse getExternalClientByToken(String bearerToken)
        throws NotFoundException {
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(constructExternalClientsUserinfoGetPath())
                          .setHeader(AUTHORIZATION_HEADER, bearerToken);

        return attempt(() -> unauthorizedClient.send(request.build(), ofString(UTF_8)))
                   .map(this::validateResponse)
                   .map(r -> mapResponse(GetExternalClientResponse.class, r))
                   .orElseThrow(this::handleFailure);
    }

    public UserDto getUser(String userName) throws NotFoundException {
        var request = getRequestBuilderFromUri(constructUserGetPath(userName));
        return attempt(getHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(r -> mapResponse(UserDto.class, r))
                   .orElseThrow(this::handleFailure);
    }

    public CustomerDto getCustomerByCristinId(URI topLevelOrgCristinId) throws NotFoundException {
        var request = getRequestBuilderFromUri(constructCustomerGetPath(topLevelOrgCristinId));
        return attempt(getHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(r -> mapResponse(CustomerDto.class, r))
                   .orElseThrow(this::handleFailure);
    }

    public CustomerDto getCustomerById(URI customerId) throws NotFoundException {
        var request = getRequestBuilderFromUri(customerId);
        return attempt(getHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(r -> mapResponse(CustomerDto.class, r))
                   .orElseThrow(this::handleFailure);
    }

    private static Builder getRequestBuilderFromUri(URI customerId) {
        return HttpRequest.newBuilder()
                   .GET()
                   .uri(customerId);
    }

    public CustomerList getAllCustomers() throws ApiGatewayException {
        var request = getRequestBuilderFromUri(constructListCustomerUri());
        return attempt(getHttpResponseCallable(request))
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

        throw new RuntimeException();
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

    private Callable<HttpResponse<String>> getHttpResponseCallable(HttpRequest.Builder request) {
        return () -> authorizedClient.send(request, ofString(UTF_8));
    }
}