package no.unit.nva.testutils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.apigateway.PersonGroup;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@SuppressWarnings("PMD.GodClass")
public class HandlerRequestBuilder<T> {

    public static final String DELIMITER = System.lineSeparator();
    public static final String AUTHORIZER_NODE = "authorizer";
    public static final String CLAIMS_NODE = "claims";
    public static final String NVA_USERNAME_CLAIM = "custom:nvaUsername";
    public static final String PERSON_GROUP_CLAIMS = "cognito:groups";
    public static final String APPLICATION_ROLES_CLAIM = "custom:applicationRoles";
    public static final String PERSON_CRISTIN_ID = "custom:cristinId";
    private static final String TOP_LEVEL_ORG_CRISTIN_ID_CLAIM = "custom:topOrgCristinId";

    public static final String ENTRIES_DELIMITER = ",";

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
        ObjectNode claims = getOrCreateAuthorizerClaimsNode();
        claims.put(NVA_USERNAME_CLAIM, nvaUsername);
        return this;
    }

    public HandlerRequestBuilder<T> withCustomerId(URI customerId) {
        var customerIdClaim = PersonGroup.createUserAtCustomerGroup(customerId);
        addClaimToPersonGroupClaims(customerIdClaim);
        return this;
    }

    public HandlerRequestBuilder withAuthorizerClaim(String claimName, String claimValue) {
        var authorizerClaimsNode = getOrCreateAuthorizerClaimsNode();
        authorizerClaimsNode.put(claimName, claimValue);
        return this;
    }

    private void addClaimToPersonGroupClaims(PersonGroup personGroup) {
        var claims = getOrCreateAuthorizerClaimsNode();
        if (isPersonAtCustomerGroupClaim(personGroup)) {
            insertAndOverwriteExistingCustomerId(personGroup, claims);
        } else {
            appendAccessRightClaimToPersonGroupClaims(claims, personGroup);
        }
    }

    private void appendAccessRightClaimToPersonGroupClaims(ObjectNode claims, PersonGroup accessRight) {
        var existingGroups = extractPersonGroups(claims);
        existingGroups.add(accessRight);
        var newClaim = existingGroups.stream()
            .map(PersonGroup::toString)
            .collect(Collectors.joining(ENTRIES_DELIMITER));
        claims.put(PERSON_GROUP_CLAIMS, newClaim);
    }

    private void insertAndOverwriteExistingCustomerId(PersonGroup personGroup, ObjectNode claims) {
        var existingPersonGroups = extractExistingPersonGroupsRemovingUserAtCustomerGroup(claims);
        var updatedPersonGroups = Stream.of(existingPersonGroups.stream(), Stream.of(personGroup))
            .flatMap(Function.identity())
            .filter(Objects::nonNull)
            .map(PersonGroup::toString)
            .collect(Collectors.joining(ENTRIES_DELIMITER));
        claims.put(PERSON_GROUP_CLAIMS, updatedPersonGroups);
    }

    private Collection<PersonGroup> extractExistingPersonGroupsRemovingUserAtCustomerGroup(ObjectNode claims) {
        var existingPersonGroups = extractPersonGroups(claims);
        if (customerIdHasAlreadyBeenSet(existingPersonGroups)) {
            existingPersonGroups = removeCustomerIdFromPersonGroups(existingPersonGroups);
        }
        return existingPersonGroups;
    }

    private Collection<PersonGroup> extractPersonGroups(ObjectNode claims) {
        return claims.has(PERSON_GROUP_CLAIMS)
                   ? PersonGroup.fromCsv(claims.get(PERSON_GROUP_CLAIMS).textValue()).collect(Collectors.toList())
                   : Collections.emptyList();
    }

    private boolean customerIdHasAlreadyBeenSet(Collection<PersonGroup> existingPersonGroups) {
        return existingPersonGroups.stream().anyMatch(PersonGroup::describesCustomerUponLogin);
    }

    private List<PersonGroup> removeCustomerIdFromPersonGroups(Collection<PersonGroup> existingPersonGroups) {
        return existingPersonGroups.stream()
            .filter(not(PersonGroup::describesCustomerUponLogin))
            .collect(Collectors.toList());
    }

    private boolean isPersonAtCustomerGroupClaim(PersonGroup group) {
        return group.describesCustomerUponLogin();
    }

    public HandlerRequestBuilder<T> withTopLevelCristinOrgId(URI topLevelCristinOrgId) {
        ObjectNode claims = getOrCreateAuthorizerClaimsNode();
        claims.put(TOP_LEVEL_ORG_CRISTIN_ID_CLAIM, topLevelCristinOrgId.toString());
        return this;
    }

    public HandlerRequestBuilder<T> withPersonCristinId(URI personCristinId) {
        ObjectNode claims = getOrCreateAuthorizerClaimsNode();
        claims.put(PERSON_CRISTIN_ID, personCristinId.toString());
        return this;
    }

    public HandlerRequestBuilder<T> withRoles(String roles) {
        ObjectNode claims = getOrCreateAuthorizerClaimsNode();
        claims.put(APPLICATION_ROLES_CLAIM, roles);
        return this;
    }

    public HandlerRequestBuilder<T> withAccessRights(URI customerId, String... accessRights) {
        for (String accessRight : accessRights) {
            var personGroup = new PersonGroup(accessRight, customerId);
            addClaimToPersonGroupClaims(personGroup);
        }
        return this;
    }

    public HandlerRequestBuilder<T> withRequestContextValue(String propertyName, String value) {
        initializeRequestContextIfNotExists();
        requestContext.put(propertyName, value);
        return this;
    }

    private ObjectNode getOrCreateAuthorizerClaimsNode() {
        ObjectNode authenticationNode = getOrCreateAuthorizerNode();
        var claimsNode = getOrCreateChildNode(authenticationNode, CLAIMS_NODE);
        authenticationNode.set(CLAIMS_NODE, claimsNode);
        return claimsNode;
    }

    private ObjectNode getOrCreateChildNode(ObjectNode parentNode, String childNodeName) {
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

    private ObjectNode getOrCreateAuthorizerNode() {
        initializeRequestContextIfNotExists();
        ObjectNode authorizerNode = getOrCreateChildNode(requestContext, AUTHORIZER_NODE);
        requestContext.set(AUTHORIZER_NODE, authorizerNode);
        return authorizerNode;
    }
}
