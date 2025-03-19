package no.unit.nva.testutils;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomAccessRight;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.auth.CognitoUserInfo;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiIoException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

class HandlerRequestBuilderTest {

    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String BODY = "body";
    public static final String HEADERS = "headers";
    public static final String PATH_PARAMETERS = "pathParameters";
    public static final String QUERY_PARAMETERS = "queryStringParameters";
    public static final String REQUEST_CONTEXT = "requestContext";
    public static final String SOME_METHOD = "POST";

    // copy-pasted values to avoid circular dependencies.
    public static final JsonPointer USER_NAME =
        JsonPointer.compile("/requestContext/authorizer/claims/custom:nvaUsername");
    public static final JsonPointer PERSON_CRISTIN_ID =
        JsonPointer.compile("/requestContext/authorizer/claims/custom:cristinId");
    public static final String TOP_ORG_CRISTIN_ID_CLAIM_PATH =
        "/requestContext/authorizer/claims/custom:topOrgCristinId";
    public static final JsonPointer PERSON_NIN =
        JsonPointer.compile("/requestContext/authorizer/claims/custom:nin");
    private static final String HTTP_METHOD = "httpMethod";
    // Can not use ObjectMapper from nva-commons because it would create a circular dependency
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildReturnsEmptyRequestOnNoArguments() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
                                  .build();

        Map<String, Object> mapWithNullBody = toMap(request);
        assertThat(mapWithNullBody.get(BODY), nullValue());
    }

    @Test
    void buildReturnsRequestWithBodyWhenStringInput() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
                                  .withBody(VALUE)
                                  .build();

        Map<String, Object> mapWithBody = toMap(request);
        assertThat(mapWithBody.get(BODY), equalTo(VALUE));
    }

    @Test
    void buildReturnsRequestWithBodyWhenMapInput() throws Exception {
        InputStream request = new HandlerRequestBuilder<Map<String, Object>>(objectMapper)
                                  .withBody(Map.of(KEY, VALUE))
                                  .build();

        Map<String, Object> mapWithBody = toMap(request);
        assertThat(mapWithBody.get(BODY), notNullValue());
    }

    @Test
    void buildReturnsRequestWithHeadersWhenWithHeaders() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
                                  .withHeaders(Map.of(KEY, VALUE))
                                  .build();

        Map<String, Object> mapWithHeaders = toMap(request);
        assertThat(mapWithHeaders.get(HEADERS), notNullValue());
    }

    @Test
    void buildReturnsRequestWithQueryParametersWhenWithQueryParameters() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
                                  .withQueryParameters(Map.of(KEY, VALUE))
                                  .build();

        Map<String, Object> mapWithQueryParameters = toMap(request);
        assertThat(mapWithQueryParameters.get(QUERY_PARAMETERS), notNullValue());
    }

    @Test
    void buildReturnsRequestWithPathParametersWhenWithPathParameters() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
                                  .withPathParameters(Map.of(KEY, VALUE))
                                  .build();

        Map<String, Object> mapWthPathParameters = toMap(request);
        assertThat(mapWthPathParameters.get(PATH_PARAMETERS), notNullValue());
    }

    @Test
    void buildReturnsRequestWithRequestContextWhenWithRequestContext() throws Exception {
        var requestContext = JsonUtils.dtoObjectMapper.createObjectNode();
        requestContext.put(KEY, VALUE);
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
                                  .withRequestContext(requestContext)
                                  .build();

        Map<String, Object> mapWithRequestContext = toMap(request);
        assertThat(mapWithRequestContext.get(REQUEST_CONTEXT), notNullValue());
    }

    @Test
    void buildReturnsRequestWithRequestContextWithUserNameClaimWhenWithNvaUserClaim()
        throws JsonProcessingException {
        var expectedUsername = randomString();
        InputStream requestStream = new HandlerRequestBuilder<String>(objectMapper).withUserName(expectedUsername)
                                        .build();
        JsonNode request = toJsonNode(requestStream);
        String actualUsername = request.at(USER_NAME).textValue();
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @Test
    void buildReturnsRequestWithRequestContextWithCustomerIdClaimWhenWithCustomerId()
        throws JsonProcessingException, UnauthorizedException {
        var expectedCustomerId = randomUri();
        var requestStream = new HandlerRequestBuilder<String>(objectMapper)
                                .withCurrentCustomer(expectedCustomerId)
                                .withAllowedCustomers(Set.of(expectedCustomerId))
                                .build();
        var request = IoUtils.streamToString(requestStream);
        var requestInfo = JsonUtils.dtoObjectMapper.readValue(request, RequestInfo.class);
        assertThat(requestInfo.getCurrentCustomer(), is(equalTo(expectedCustomerId)));
    }

    @Test
    void buildReturnsPersonsFeideIdWhenSet() throws JsonProcessingException, ApiIoException, UnauthorizedException {
        var expectedFeideId = randomString();
        var request = new HandlerRequestBuilder<String>(objectMapper)
                          .withFeideId(expectedFeideId)
                          .build();
        var requestInfo = RequestInfo.fromRequest(request);

        assertThat(requestInfo.getFeideId().isPresent(), is(true));
        assertThat(requestInfo.getFeideId().orElseThrow(), is(equalTo(expectedFeideId)));
    }

    @Test
    void buildReturnsEmptyOptionalWhenFeideIdNotSet()
        throws JsonProcessingException, ApiIoException, UnauthorizedException {
        var request = new HandlerRequestBuilder<String>(objectMapper).build();
        var requestInfo = RequestInfo.fromRequest(request);

        requestInfo.setRequestContext(getRequestContext());

        assertThat(requestInfo.getFeideId().isPresent(), is(false));
        assertThat(requestInfo.getFeideId(), is(equalTo(Optional.empty())));
    }

    private static ObjectNode getRequestContext() {
        var claims = dtoObjectMapper.createObjectNode();
        var authorizer = dtoObjectMapper.createObjectNode();
        var requestContext = dtoObjectMapper.createObjectNode();
        claims.put(CognitoUserInfo.USER_NAME_CLAIM, "someUsername");
        authorizer.set("claims", claims);
        requestContext.set("authorizer", authorizer);
        return requestContext;
    }

    @Test
    void buildReturnsRequestWithRequestContextWithClaimsWhenWithClaims()
        throws JsonProcessingException {
        var expectedUsername = randomString();
        var expectedCustomerId = randomUri();
        var expectedApplicationRoles = "role1,role2";

        InputStream requestStream = new HandlerRequestBuilder<String>(objectMapper).withUserName(expectedUsername)
                                        .withAccessRights(expectedCustomerId, randomAccessRight(),
                                                          AccessRight.MANAGE_DEGREE,
                                                          AccessRight.MANAGE_RESOURCES_STANDARD)
                                        .withRoles(expectedApplicationRoles)
                                        .build();
        JsonNode request = toJsonNode(requestStream);

        String actualUsername = request.at(USER_NAME).textValue();
        assertThat(actualUsername, is(equalTo(expectedUsername)));
    }

    @Test
    void buildReturnsRequestWithMethodWhenWithMethod() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
                                  .withHttpMethod(SOME_METHOD)
                                  .build();

        Map<String, Object> mapWithMethod = toMap(request);
        assertThat(mapWithMethod.get(HTTP_METHOD).toString(), is(equalTo(SOME_METHOD)));
    }

    @Test
    void buildReturnsCustomPropertiesSetInRequest() throws IOException {
        String expectedKey = "someKey";
        String expectedValue = "someValue";
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
                                  .withOtherProperties(Map.of(expectedKey, expectedValue))
                                  .build();

        Map<String, Object> mapWithCustomField = toMap(request);
        assertThat(mapWithCustomField, hasEntry(expectedKey, expectedValue));
    }

    @Test
    void shouldInsertPersonsCristinIdWhenSet() throws JsonProcessingException {
        var expectedCristinId = randomUri();
        var request = new HandlerRequestBuilder<String>(objectMapper)
                          .withPersonCristinId(expectedCristinId)
                          .build();

        JsonNode requestJson = toJsonNode(request);
        String actualCristinId = requestJson.at(PERSON_CRISTIN_ID).asText();
        assertThat(actualCristinId, is(equalTo(expectedCristinId.toString())));
    }

    @Test
    void shouldReturnRequestContextWithSetValues() throws JsonProcessingException {
        var expectedPath = "/path";
        var expectedDomainName = "localhost";
        var request = new HandlerRequestBuilder<String>(objectMapper)
                          .withRequestContextValue("path", expectedPath)
                          .withRequestContextValue("domainName", expectedDomainName)
                          .build();

        JsonNode requestJson = toJsonNode(request);
        String actualPath = requestJson.at("/requestContext/path").asText();
        assertThat(actualPath, is(equalTo(expectedPath)));

        String actualDomainName = requestJson.at("/requestContext/domainName").asText();
        assertThat(actualDomainName, is(equalTo(expectedDomainName)));
    }

    @Test
    void shouldReturnRequestWithTopLevelOrgCristinId() throws JsonProcessingException {
        URI expectedUri = randomUri();
        var request = new HandlerRequestBuilder<String>(objectMapper)
                          .withTopLevelCristinOrgId(expectedUri)
                          .build();
        var requestJson = toJsonNode(request);
        var actualClaim = requestJson.at(TOP_ORG_CRISTIN_ID_CLAIM_PATH).textValue();
        assertThat(URI.create(actualClaim), is(equalTo(expectedUri)));
    }

    @Test
    void shouldReturnApiProxyEventWithAllFieldsFilledIn() throws JsonProcessingException {
        var request = new HandlerRequestBuilder<String>(objectMapper)
                          .withPersonCristinId(randomUri())
                          .withBody(randomJson())
                          .withAccessRights(randomUri(), randomAccessRight(), randomAccessRight())
                          .withPathParameters(Map.of(randomString(), randomString()))
                          .buildRequestEvent();
        assertThat(request.getPathParameters().keySet(), is(not(empty())));
    }

    @Test
    void shouldInsertPersonsNinWhenSet() throws JsonProcessingException {
        var expectedPersonNin = randomString();
        var request = new HandlerRequestBuilder<String>(objectMapper)
                          .withPersonNin(expectedPersonNin)
                          .build();

        JsonNode requestJson = toJsonNode(request);
        String actualPersonNin = requestJson.at(PERSON_NIN).asText();
        assertThat(actualPersonNin, is(equalTo(expectedPersonNin)));
    }

    private Map<String, Object> toMap(InputStream inputStream) throws JsonProcessingException {
        TypeReference<Map<String, Object>> type = new TypeReference<>() {
        };
        return objectMapper.readValue(HandlerRequestBuilder.toString(inputStream), type);
    }

    private JsonNode toJsonNode(InputStream inputStream) throws JsonProcessingException {
        return objectMapper.readTree(HandlerRequestBuilder.toString(inputStream));
    }
}
