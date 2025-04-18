package no.unit.nva.testutils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.auth.CognitoUserInfo.ACCESS_RIGHTS_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.ALLOWED_CUSTOMERS;
import static no.unit.nva.auth.CognitoUserInfo.COGNITO_USER_NAME;
import static no.unit.nva.auth.CognitoUserInfo.EMPTY_STRING;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.AccessRightEntry;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@SuppressWarnings("PMD.GodClass")
public class HandlerRequestBuilder<T> {

    public static final String DELIMITER = System.lineSeparator();
    public static final String AUTHORIZER_NODE = "authorizer";
    public static final String CLAIMS_NODE = "claims";
    public static final String USER_NAME_CLAIM = "custom:nvaUsername";
    public static final String GROUPS_CLAIM = "cognito:groups";
    public static final String APPLICATION_ROLES_CLAIM = "custom:applicationRoles";
    public static final String PERSON_CRISTIN_ID = "custom:cristinId";
    public static final String CUSTOMER_ID = "custom:customerId";
    public static final String FEIDE_ID_CLAIM = "custom:feideId";
    public static final String ENTRIES_DELIMITER = ",";
    public static final String SCOPE_CLAIM = "scope";
    public static final String ISS_CLAIM = "iss";
    public static final String CLIENT_ID_CLAIM = "client_id";
    public static final String PERSON_NIN_CLAIM = "custom:nin";
    private static final String TOP_LEVEL_ORG_CRISTIN_ID_CLAIM = "custom:topOrgCristinId";
    private static final String COMMA = ",";
    private final ObjectMapper objectMapper;
    @JsonProperty("body")
    private String body;
    @JsonProperty("headers")
    private Map<String, String> headers;
    @JsonProperty("queryStringParameters")
    private Map<String, String> queryParameters;
    @JsonProperty("multiValueQueryStringParameters")
    private Map<String, List<String>> multiValueQueryParameters;
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

    public HandlerRequestBuilder<T> withMultiValueQueryParameters(Map<String, List<String>> multiValueQueryParameters) {
        this.multiValueQueryParameters = multiValueQueryParameters;
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

    public HandlerRequestBuilder<T> withHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    /**
     * @param requestContext the requestContext object.
     * @return the builder.
     * @deprecated Use the method that accepts an {@link ObjectNode} as a parameter.
     */
    @Deprecated(since = "1.25.0")
    public HandlerRequestBuilder<T> withRequestContext(Map<String, Object> requestContext) {
        this.requestContext = objectMapper.convertValue(requestContext, ObjectNode.class);
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

    public Map<String, List<String>> getMultiValueQueryParameters() {
        return multiValueQueryParameters;
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

    public HandlerRequestBuilder<T> withUserName(String userName) {
        var claims = getAuthorizerClaimsNode();
        claims.put(USER_NAME_CLAIM, userName);
        return this;
    }

    @Deprecated(since = "1.25.5")
    @JacocoGenerated
    public HandlerRequestBuilder<T> withNvaUsername(String nvaUsername) {
        return withUserName(nvaUsername);
    }

    public HandlerRequestBuilder<T> withCurrentCustomer(URI customerId) {
        var claims = getAuthorizerClaimsNode();
        claims.put(CUSTOMER_ID, customerId.toString());
        return this;
    }

    public HandlerRequestBuilder<T> withAllowedCustomers(Set<URI> customers) {
        var claims = getAuthorizerClaimsNode();
        claims.put(ALLOWED_CUSTOMERS, customers.stream().map(URI::toString).collect(Collectors.joining(",")));
        return this;
    }

    public HandlerRequestBuilder<T> withAuthorizerClaim(String claimName, String claimValue) {
        var authorizerClaimsNode = getAuthorizerClaimsNode();
        authorizerClaimsNode.put(claimName, claimValue);
        return this;
    }

    public HandlerRequestBuilder<T> withTopLevelCristinOrgId(URI topLevelCristinOrgId) {
        var claims = getAuthorizerClaimsNode();
        claims.put(TOP_LEVEL_ORG_CRISTIN_ID_CLAIM, topLevelCristinOrgId.toString());
        return this;
    }

    public HandlerRequestBuilder<T> withPersonCristinId(URI personCristinId) {
        var claims = getAuthorizerClaimsNode();
        claims.put(PERSON_CRISTIN_ID, personCristinId.toString());
        return this;
    }

    public HandlerRequestBuilder<T> withPersonNin(String personNin) {
        var claims = getAuthorizerClaimsNode();
        claims.put(PERSON_NIN_CLAIM, personNin);
        return this;
    }

    public HandlerRequestBuilder<T> withFeideId(String feideId) {
        var claims = getAuthorizerClaimsNode();
        claims.put(FEIDE_ID_CLAIM, feideId);
        return this;
    }

    public HandlerRequestBuilder<T> withRoles(String roles) {
        var claims = getAuthorizerClaimsNode();
        claims.put(APPLICATION_ROLES_CLAIM, roles);
        return this;
    }

    public HandlerRequestBuilder<T> withAccessRights(URI customerId, AccessRight... accessRights) {
        for (AccessRight accessRight : accessRights) {
            var accessRightEntry = new AccessRightEntry(accessRight, customerId);
            addAccessRightToCognitoGroups(accessRightEntry);
        }

        var claims = getAuthorizerClaimsNode();
        var accessRightsString = Arrays.stream(accessRights)
                                     .map(AccessRight::toPersistedString)
                                     .collect(Collectors.joining(COMMA));
        claims.put(ACCESS_RIGHTS_CLAIM, accessRightsString);

        return this;
    }

    public HandlerRequestBuilder<T> withRequestContextValue(String propertyName, String value) {
        initializeRequestContextIfNotExists();
        requestContext.put(propertyName, value);
        return this;
    }

    public HandlerRequestBuilder<T> withScope(String scope) {
        var authorizerClaims = getAuthorizerClaimsNode();
        authorizerClaims.put(SCOPE_CLAIM, scope);
        return this;
    }

    public HandlerRequestBuilder<T> withIssuer(String issuer) {
        var authorizerClaims = getAuthorizerClaimsNode();
        authorizerClaims.put(ISS_CLAIM, issuer);
        return this;
    }

    public HandlerRequestBuilder<T> withCognitoUsername(String cognitoUsername) {
        var authorizerClaims = getAuthorizerClaimsNode();
        authorizerClaims.put(COGNITO_USER_NAME, cognitoUsername);
        return this;
    }

    public HandlerRequestBuilder<T> withClientId(String clientId) {
        var authorizerClaims = getAuthorizerClaimsNode();
        authorizerClaims.put(CLIENT_ID_CLAIM, clientId);
        return this;
    }

    private void addAccessRightToCognitoGroups(AccessRightEntry accessRight) {
        var claims = getAuthorizerClaimsNode();
        appendAccessRightClaimToAccessRightClaims(claims, accessRight);
    }

    private void appendAccessRightClaimToAccessRightClaims(ObjectNode claims, AccessRightEntry accessRight) {
        var existingAccessRights = extractAccessRights(claims);
        existingAccessRights.add(accessRight);
        var newClaim = existingAccessRights.stream()
                           .map(AccessRightEntry::toString)
                           .collect(Collectors.joining(ENTRIES_DELIMITER));
        claims.put(GROUPS_CLAIM, newClaim);
    }

    private Collection<AccessRightEntry> extractAccessRights(ObjectNode claims) {
        return new ArrayList<>(
            AccessRightEntry.fromCsv(Optional.ofNullable(claims.get(GROUPS_CLAIM)).map(JsonNode::asText).orElse(EMPTY_STRING))
                .toList());
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
