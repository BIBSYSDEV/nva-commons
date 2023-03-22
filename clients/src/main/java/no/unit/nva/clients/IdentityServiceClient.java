package no.unit.nva.clients;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Callable;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
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
    private final AuthorizedBackendClient authorizedClient;
    private static final String AUTH_HOST = new Environment().readEnv("BACKEND_CLIENT_AUTH_URL");
    private static final String API_HOST = new Environment().readEnv("API_HOST");

    public IdentityServiceClient(HttpClient httpClient,
                                 String bearerToken,
                                 CognitoCredentials cognitoCredentials) {
        this.authorizedClient = AuthorizedBackendClient.prepareWithBearerTokenAndCredentials(
            httpClient,
            bearerToken,
            cognitoCredentials
        );
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

    private <T> T mapResponse(Class<T> clazz, HttpResponse<String> response)
        throws JsonProcessingException {
        return dtoObjectMapper.readValue(
            response.body(),
            clazz);
    }

    public GetExternalClientResponse getExternalClient(String clientId) throws NotFoundException {
        var request = HttpRequest.newBuilder()
                          .GET()
                          .uri(constructExternalClientsGetPath(clientId));
        return attempt(getHttpResponseCallable(request))
                   .map(this::validateResponse)
                   .map(r -> mapResponse(GetExternalClientResponse.class, r))
                   .orElseThrow(this::handleFailure);
    }

    private NotFoundException handleFailure(Failure<GetExternalClientResponse> responseFailure) {
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

    @JacocoGenerated
    private static CognitoCredentials fetchCredentials() {
        var secretsReader = new SecretsReader(SecretsReader.defaultSecretsManagerClient());

        var credentials = secretsReader.fetchClassSecret(CREDENTIALS_SECRET_NAME,
                                                         BackendClientCredentials.class);
        var uri = UriWrapper.fromHost(AUTH_HOST).getUri();
        return new CognitoCredentials(credentials::getId, credentials::getSecret, uri);
    }

    @JacocoGenerated
    public static IdentityServiceClient prepare() {
        var credentials = fetchCredentials();
        return new IdentityServiceClient(HttpClient.newBuilder().build(), null, credentials);
    }
}