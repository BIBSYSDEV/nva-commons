package no.unit.nva.clients.cristin;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.HttpStatusCode;

public class CristinClient {

    public static final String CRISTIN = "cristin";
    public static final String PERSON = "person";
    public static final String ORGANIZATION = "organization";
    private static final Logger LOGGER = LoggerFactory.getLogger(CristinClient.class);
    private static final String API_HOST = new Environment().readEnv("API_HOST");
    private final HttpClient httpClient;

    public CristinClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @JacocoGenerated
    public static CristinClient prepareNotAuthorizedClient() {
        return new CristinClient(HttpClient.newBuilder().build());
    }

    public Optional<CristinOrganizationDto> getOrganization(String identifier) {
        return getOrganization(createCristinOrganizationUri(identifier));
    }

    public Optional<CristinPersonDto> getPerson(String identifier) {
        return getPerson(createCristinPeronUri(identifier));
    }

    public Optional<CristinPersonDto> getPerson(URI uri) {
        var request = HttpRequest.newBuilder().GET().uri(uri).build();
        return attempt(() -> httpClient.send(request, ofString(UTF_8))).map(this::validateResponse)
                   .map(response -> mapResponse(CristinPersonDto.class, response))
                   .toOptional();
    }

    public Optional<CristinOrganizationDto> getOrganization(URI uri) {
        var request = HttpRequest.newBuilder().GET().uri(uri).build();
        return attempt(() -> httpClient.send(request, ofString(UTF_8))).map(this::validateResponse)
                   .map(response -> mapResponse(CristinOrganizationDto.class, response))
                   .toOptional();
    }

    private static URI createCristinPeronUri(String identifier) {
        return UriWrapper.fromHost(API_HOST).addChild(CRISTIN).addChild(PERSON).addChild(identifier).getUri();
    }

    private static URI createCristinOrganizationUri(String identifier) {
        return UriWrapper.fromHost(API_HOST).addChild(CRISTIN).addChild(ORGANIZATION).addChild(identifier).getUri();
    }

    private HttpResponse<String> validateResponse(HttpResponse<String> response) throws NotFoundException {
        var requestUri = response.request().uri();
        if (response.statusCode() == HttpStatusCode.NOT_FOUND) {
            LOGGER.error("Cristin responded with not found: {}", requestUri);
            throw new NotFoundException("Not found " + requestUri);
        }

        if (response.statusCode() != HttpStatusCode.OK) {
            LOGGER.error("Cristin responded with {} when fetching: {}", response.statusCode(), requestUri);
            throw new RuntimeException("Something went wrong fetching: " + requestUri);
        }
        return response;
    }

    private <T> T mapResponse(Class<T> clazz, HttpResponse<String> response) throws JsonProcessingException {
        return dtoObjectMapper.readValue(response.body(), clazz);
    }
}
