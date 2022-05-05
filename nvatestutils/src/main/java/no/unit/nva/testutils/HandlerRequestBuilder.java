package no.unit.nva.testutils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.apigateway.AccessRight;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@SuppressWarnings("PMD.GodClass")
public class HandlerRequestBuilder<T> {

    public static final String DELIMITER = System.lineSeparator();
    public static final String AUTHORIZER_NODE = "authorizer";
    public static final String CLAIMS_NODE = "claims";
    public static final String NVA_USERNAME_CLAIM = "custom:nvaUsername";
    public static final String ACCESS_RIGHTS_CLAIMS = "cognito:groups";
    public static final String APPLICATION_ROLES_CLAIM = "custom:applicationRoles";
    public static final String PERSON_CRISTIN_ID = "custom:cristinId";
    public static final String ENTRIES_DELIMITER = ",";
    private static final String TOP_LEVEL_ORG_CRISTIN_ID_CLAIM = "custom:topOrgCristinId";
    private final transient ObjectMapper objectMapper;
    @JsonProperty("body")
    private String body;
    @JsonProperty("headers")
    private Map<String, String> headers;
    @JsonProperty("queryStringParameters")
    private Map<String, String> queryParameters;
    @JsonProperty("pathParameters")
    private Map<String, String> pathParameters;
    @JsonProperty("requestContext")
    private ObjectNode requestContext;
    @JsonProperty("httpMethod")
    private String httpMethod;
    @JsonAnySetter
    private Map<String, Object> otherProperties;

    public HandlerRequestBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.otherProperties = new LinkedHashMap<>();
    }

    public static String toString(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining(DELIMITER));
    }

    public HandlerRequestBuilder<T> withBody(T body) throws JsonProcessingException {
        if (body instanceof String) {
            this.body = (String) body;
        } else {
            this.body = objectMapper.writeValueAsString(body);
        }
        return this;
    }

    public HandlerRequestBuilder<T> withHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public HandlerRequestBuilder<T> withQueryParameters(Map<String, String> queryParameters) {
        this.queryParameters = queryParameters;
        return this;
    }

    public HandlerRequestBuilder<T> withPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
        return this;
    }

    public HandlerRequestBuilder<T> withRequestContext(ObjectNode requestContext) {
        this.requestContext = requestContext;
        return this;
    }

    /**
     * Use the method that accepts an {@link ObjectNode} as a parameter.
     *
     * @param requestContext the requestContext object.
     * @return the builder.
     */
    @Deprecated
    public HandlerRequestBuilder<T> withRequestContext(Map<String, Object> requestContext) {
        this.requestContext = objectMapper.convertValue(requestContext, ObjectNode.class);
        return this;
    }

    public HandlerRequestBuilder<T> withHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public HandlerRequestBuilder<T> withOtherProperties(Map<String, Object> otherProperties) {
        this.otherProperties.putAll(otherProperties);
        return this;
    }

    public InputStream build() throws JsonProcessingException {
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(this));
    }

    public APIGatewayProxyRequestEvent buildRequestEvent() throws JsonProcessingException {
        var json = objectMapper.writeValueAsString(this);
        return objectMapper.readValue(json, APIGatewayProxyRequestEvent.class);
    }

    public T getBody(TypeReference<T> typeRef) throws JsonProcessingException {
        if (nonNull(body)) {
            return objectMapper.readValue(body, typeRef);
        }
        return null;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public Map<String, Object> getRequestContext() {
        JavaType mapType = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        return objectMapper.convertValue(requestContext, mapType);
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }

    public void setOtherProperties(Map<String, Object> otherProperties) {
        this.otherProperties = otherProperties;
    }

    public HandlerRequestBuilder<T> withNvaUsername(String nvaUsername) {
        ObjectNode claims = getAuthorizerClaimsNode();
        claims.put(NVA_USERNAME_CLAIM, nvaUsername);
        return this;
    }

    public HandlerRequestBuilder<T> withCustomerId(URI customerId) {
        var customerIdClaim = AccessRight.createUserAtCustomerGroup(customerId);
        addAccessRightToCognitoGroups(customerIdClaim);
        return this;
    }

    public HandlerRequestBuilder<T> withAuthorizerClaim(String claimName, String claimValue) {
        var authorizerClaimsNode = getAuthorizerClaimsNode();
        authorizerClaimsNode.put(claimName, claimValue);
        return this;
    }

    public HandlerRequestBuilder<T> withTopLevelCristinOrgId(URI topLevelCristinOrgId) {
        ObjectNode claims = getAuthorizerClaimsNode();
        claims.put(TOP_LEVEL_ORG_CRISTIN_ID_CLAIM, topLevelCristinOrgId.toString());
        return this;
    }

    public HandlerRequestBuilder<T> withPersonCristinId(URI personCristinId) {
        ObjectNode claims = getAuthorizerClaimsNode();
        claims.put(PERSON_CRISTIN_ID, personCristinId.toString());
        return this;
    }

    public HandlerRequestBuilder<T> withRoles(String roles) {
        ObjectNode claims = getAuthorizerClaimsNode();
        claims.put(APPLICATION_ROLES_CLAIM, roles);
        return this;
    }

    public HandlerRequestBuilder<T> withAccessRights(URI customerId, String... accessRights) {
        for (String accessRightString : accessRights) {
            var accessRight = new AccessRight(accessRightString, customerId);
            addAccessRightToCognitoGroups(accessRight);
        }
        return this;
    }

    public HandlerRequestBuilder<T> withRequestContextValue(String propertyName, String value) {
        initializeRequestContextIfNotExists();
        requestContext.put(propertyName, value);
        return this;
    }

    private void addAccessRightToCognitoGroups(AccessRight accessRight) {
        var claims = getAuthorizerClaimsNode();
        if (isPersonAtCustomerGroupClaim(accessRight)) {
            insertAndOverwriteExistingCustomerId(accessRight, claims);
        } else {
            appendAccessRightClaimToAccessRightClaims(claims, accessRight);
        }
    }

    private void appendAccessRightClaimToAccessRightClaims(ObjectNode claims, AccessRight accessRight) {
        var existingAccessRights = extractAccessRights(claims);
        existingAccessRights.add(accessRight);
        var newClaim = existingAccessRights.stream()
            .map(AccessRight::toString)
            .collect(Collectors.joining(ENTRIES_DELIMITER));
        claims.put(ACCESS_RIGHTS_CLAIMS, newClaim);
    }

    private void insertAndOverwriteExistingCustomerId(AccessRight accessRight, ObjectNode claims) {
        var existingAccessRights = extractExistingAccessRightsRemovingSpecialUserAtCustomerClaim(claims);
        var updatedAccessRights = Stream.of(existingAccessRights.stream(), Stream.of(accessRight))
            .flatMap(Function.identity())
            .filter(Objects::nonNull)
            .map(AccessRight::toString)
            .collect(Collectors.joining(ENTRIES_DELIMITER));
        claims.put(ACCESS_RIGHTS_CLAIMS, updatedAccessRights);
    }

    private Collection<AccessRight> extractExistingAccessRightsRemovingSpecialUserAtCustomerClaim(ObjectNode claims) {
        var existingAccessRights = extractAccessRights(claims);
        if (customerIdExists(existingAccessRights)) {
            existingAccessRights = removeCustomerIdFromAccessRights(existingAccessRights);
        }
        return existingAccessRights;
    }

    private Collection<AccessRight> extractAccessRights(ObjectNode claims) {
        return claims.has(ACCESS_RIGHTS_CLAIMS)
                   ? AccessRight.fromCsv(claims.get(ACCESS_RIGHTS_CLAIMS).textValue()).collect(Collectors.toList())
                   : new ArrayList<>();
    }

    private boolean customerIdExists(Collection<AccessRight> existingAccessRights) {
        return existingAccessRights.stream().anyMatch(AccessRight::describesCustomerUponLogin);
    }

    private List<AccessRight> removeCustomerIdFromAccessRights(Collection<AccessRight> existingAccessRights) {
        return existingAccessRights.stream()
            .filter(not(AccessRight::describesCustomerUponLogin))
            .collect(Collectors.toList());
    }

    private boolean isPersonAtCustomerGroupClaim(AccessRight group) {
        return group.describesCustomerUponLogin();
    }

    private ObjectNode getAuthorizerClaimsNode() {
        ObjectNode authorizerNode = populateAuthorizerNode();
        var claimsNode = getChildNode(authorizerNode, CLAIMS_NODE);
        authorizerNode.set(CLAIMS_NODE, claimsNode);
        return claimsNode;
    }

    private ObjectNode getChildNode(ObjectNode parentNode, String childNodeName) {
        return Optional.ofNullable(parentNode)
            .map(parent -> parent.get(childNodeName))
            .filter(JsonNode::isObject)
            .map(ObjectNode.class::cast)
            .orElse(objectMapper.createObjectNode());
    }

    private void initializeRequestContextIfNotExists() {
        if (isNull(requestContext)) {
            requestContext = objectMapper.createObjectNode();
        }
    }

    private ObjectNode populateAuthorizerNode() {
        initializeRequestContextIfNotExists();
        ObjectNode authorizerNode = getChildNode(requestContext, AUTHORIZER_NODE);
        requestContext.set(AUTHORIZER_NODE, authorizerNode);
        return authorizerNode;
    }
}
