package no.unit.nva.testutils;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class HandlerRequestBuilderTest {

    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String BODY = "body";
    public static final String HEADERS = "headers";
    public static final String PATH_PARAMETERS = "pathParameters";
    public static final String QUERY_PARAMETERS = "queryStringParameters";
    public static final String REQUEST_CONTEXT = "requestContext";
    public static final String SOME_METHOD = "POST";

    // copy-pasted values to avoid circular dependencies.
    public static final JsonPointer FEIDE_ID = JsonPointer.compile("/requestContext/authorizer/claims/custom:feideId");
    public static final JsonPointer CUSTOMER_ID = JsonPointer.compile(
        "/requestContext/authorizer/claims/custom:customerId");
    public static final JsonPointer APPLICATION_ROLES =
        JsonPointer.compile("/requestContext/authorizer/claims/custom:applicationRoles");
    public static final JsonPointer ACCESS_RIGHTS =
        JsonPointer.compile("/requestContext/authorizer/claims/custom:accessRights");
    public static final JsonPointer CRISTIN_ID =
        JsonPointer.compile("/requestContext/authorizer/claims/custom:cristinId");

    private static final String HTTP_METHOD = "httpMethod";

    // Can not use ObjectMapper from nva-commons because it would create a circular dependency
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void buildReturnsEmptyRequestOnNoArguments() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
            .build();

        Map<String, Object> mapWithNullBody = toMap(request);
        assertThat(mapWithNullBody.get(BODY), nullValue());
    }

    @Test
    public void buildReturnsRequestWithBodyWhenStringInput() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
            .withBody(VALUE)
            .build();

        Map<String, Object> mapWithBody = toMap(request);
        assertThat(mapWithBody.get(BODY), equalTo(VALUE));
    }

    @Test
    public void buildReturnsRequestWithBodyWhenMapInput() throws Exception {
        InputStream request = new HandlerRequestBuilder<Map<String, Object>>(objectMapper)
            .withBody(Map.of(KEY, VALUE))
            .build();

        Map<String, Object> mapWithBody = toMap(request);
        assertThat(mapWithBody.get(BODY), notNullValue());
    }

    @Test
    public void buildReturnsRequestWithHeadersWhenWithHeaders() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
            .withHeaders(Map.of(KEY, VALUE))
            .build();

        Map<String, Object> mapWithHeaders = toMap(request);
        assertThat(mapWithHeaders.get(HEADERS), notNullValue());
    }

    @Test
    public void buildReturnsRequestWithQueryParametersWhenWithQueryParameters() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
            .withQueryParameters(Map.of(KEY, VALUE))
            .build();

        Map<String, Object> mapWithQueryParameters = toMap(request);
        assertThat(mapWithQueryParameters.get(QUERY_PARAMETERS), notNullValue());
    }

    @Test
    public void buildReturnsRequestWithPathParametersWhenWithPathParameters() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
            .withPathParameters(Map.of(KEY, VALUE))
            .build();

        Map<String, Object> mapWthPathParameters = toMap(request);
        assertThat(mapWthPathParameters.get(PATH_PARAMETERS), notNullValue());
    }

    @Test
    public void buildReturnsRequestWithRequestContextWhenWithRequestContext() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
            .withRequestContext(Map.of(KEY, VALUE))
            .build();

        Map<String, Object> mapWithRequestContext = toMap(request);
        assertThat(mapWithRequestContext.get(REQUEST_CONTEXT), notNullValue());
    }

    @Test
    public void buildReturnsRequestWithRequestContextWithFeideIdClaimWhenWithFeideId() throws JsonProcessingException {
        var expectedFeideId = "someFeide@id";
        InputStream requestStream = new HandlerRequestBuilder<String>(objectMapper)
            .withFeideId(expectedFeideId)
            .build();
        JsonNode request = toJsonNode(requestStream);
        String actualFeideId = request.at(FEIDE_ID).textValue();
        assertThat(actualFeideId, is(equalTo(expectedFeideId)));
    }

    @Test
    public void buildReturnsRequestWithRequestContextWithCustomerIdClaimWhenWithCustomerId()
        throws JsonProcessingException {
        var expectedCustomerId = "SomeCustomerId";
        InputStream requestStream = new HandlerRequestBuilder<String>(objectMapper)
            .withCustomerId(expectedCustomerId)
            .build();
        JsonNode request = toJsonNode(requestStream);
        String actualCustomerId = request.at(CUSTOMER_ID).textValue();
        assertThat(actualCustomerId, is(equalTo(actualCustomerId)));
    }

    @Test
    public void buildReturnsRequestWithRequestContextWithApplicationRolesClaimWhenWithApplicationRoles()
        throws JsonProcessingException {
        var expectedApplicationRoles = "role1,role2";
        InputStream requestStream = new HandlerRequestBuilder<String>(objectMapper)
            .withRoles(expectedApplicationRoles)
            .build();
        JsonNode request = toJsonNode(requestStream);
        String actualRoles = request.at(APPLICATION_ROLES).textValue();
        assertThat(actualRoles, is(equalTo(expectedApplicationRoles)));
    }

    @Test
    public void buildReturnsRequestWithRequestContextWithClaimsWhenWithClaims()
        throws JsonProcessingException {
        var expectedFeideId = "SomeFeideId";
        var expectedCustomerId = "SomeCustomerId";
        var expectedApplicationRoles = "role1,role2";

        InputStream requestStream = new HandlerRequestBuilder<String>(objectMapper)
            .withFeideId(expectedFeideId)
            .withCustomerId(expectedCustomerId)
            .withRoles(expectedApplicationRoles)
            .build();
        JsonNode request = toJsonNode(requestStream);

        String actualFeideId = request.at(FEIDE_ID).textValue();
        assertThat(actualFeideId, is(equalTo(expectedFeideId)));

        String actualCustomerId = request.at(CUSTOMER_ID).textValue();
        assertThat(actualCustomerId, is(equalTo(expectedCustomerId)));

        String actualRoles = request.at(APPLICATION_ROLES).textValue();
        assertThat(actualRoles, is(equalTo(expectedApplicationRoles)));
    }

    @Test
    public void buildReturnsRequestWithMethodWhenWithMethod() throws Exception {
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
            .withHttpMethod(SOME_METHOD)
            .build();

        Map<String, Object> mapWithMethod = toMap(request);
        assertThat(mapWithMethod.get(HTTP_METHOD).toString(), is(equalTo(SOME_METHOD)));
    }

    @Test
    public void buildReturnsCustomPropertiesSetInRequest() throws IOException {
        String expectedKey = "someKey";
        String expectedValue = "someValue";
        InputStream request = new HandlerRequestBuilder<String>(objectMapper)
            .withOtherProperties(Map.of(expectedKey, expectedValue))
            .build();

        Map<String, Object> mapWithCustomField = toMap(request);

        assertThat(mapWithCustomField, hasEntry(expectedKey, expectedValue));
    }

    @Test
    public void buildReturnsAccessRightsWhenAccessRightsHaveBeenSet() throws JsonProcessingException {
        String accessRight1 = "AccessRight1";
        String accessRight2 = "AccessRight2";
        InputStream inputStream = new HandlerRequestBuilder<String>(objectMapper)
            .withAccessRight(accessRight1)
            .withAccessRight(accessRight2)
            .build();
        JsonNode requestJson = toJsonNode(inputStream);

        String accessRights = requestJson.at(ACCESS_RIGHTS).textValue();
        assertThat(accessRights, containsString(accessRight1));
        assertThat(accessRights, containsString(accessRight2));
    }

    @Test
    void shouldInsertCustomersCristinIdWhenSet() throws JsonProcessingException {
        var expectedCristinId = randomUri().toString();
        var request = new HandlerRequestBuilder<String>(objectMapper)
            .withCustomerCristinId(expectedCristinId)
            .build();

        JsonNode requestJson = toJsonNode(request);
        String actualCristinId = requestJson.at(CRISTIN_ID).asText();
        assertThat(actualCristinId, is(equalTo(expectedCristinId)));
    }

    @Test
    public void shouldReturnRequestContextWithSetValues() throws JsonProcessingException {
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

    private Map<String, Object> toMap(InputStream inputStream) throws JsonProcessingException {
        TypeReference<Map<String, Object>> type = new TypeReference<>() {
        };
        return objectMapper.readValue(HandlerRequestBuilder.toString(inputStream), type);
    }

    private JsonNode toJsonNode(InputStream inputStream) throws JsonProcessingException {
        return objectMapper.readTree(HandlerRequestBuilder.toString(inputStream));
    }
}
