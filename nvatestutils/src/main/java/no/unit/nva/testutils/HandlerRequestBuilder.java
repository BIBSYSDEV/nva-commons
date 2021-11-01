package no.unit.nva.testutils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class HandlerRequestBuilder<T> {

    public static final String DELIMITER = System.lineSeparator();
    public static final String AUTHORIZER_NODE = "authorizer";
    public static final String CLAIMS_NODE = "claims";
    public static final String FEIDE_ID_CLAIM = "custom:feideId";
    public static final String CUSTOMER_ID_CLAIM = "custom:customerId";
    public static final String APPLICATION_ROLES_CLAIM = "custom:applicationRoles";
    public static final String ACCESS_RIGHTS_CLAIM = "custom:accessRights";
    public static final String ACCESS_RIGHTS_SEPARATOR = ",";
    public static final String EMPTY_STRING = "";
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

    public HandlerRequestBuilder<T> withFeideId(String feideId) {
        ObjectNode claims = getOrCreateClaimsNode();
        claims.put(FEIDE_ID_CLAIM, feideId);
        return this;
    }

    public HandlerRequestBuilder<T> withCustomerId(String customerId) {
        ObjectNode claims = getOrCreateClaimsNode();
        claims.put(CUSTOMER_ID_CLAIM, customerId);
        return this;
    }

    public HandlerRequestBuilder<T> withRoles(String roles) {
        ObjectNode claims = getOrCreateClaimsNode();
        claims.put(APPLICATION_ROLES_CLAIM, roles);
        return this;
    }

    public HandlerRequestBuilder<T> withAccessRight(String accessRight) {
        ObjectNode claims = getOrCreateClaimsNode();
        String accessRights = Optional.ofNullable(claims.get(ACCESS_RIGHTS_CLAIM))
            .map(JsonNode::textValue)
            .orElse(EMPTY_STRING);
        List<String> accessRightsList = toList(accessRights);
        accessRightsList.add(accessRight);
        String newAccessRights = toAccessRightsString(accessRightsList);
        claims.put(ACCESS_RIGHTS_CLAIM, newAccessRights);
        return this;
    }

    private String toAccessRightsString(List<String> accessRightsList) {
        return String.join(ACCESS_RIGHTS_SEPARATOR, accessRightsList);
    }

    private List<String> toList(String accessRights) {
        if (isNull(accessRights) || accessRights.isBlank()) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(Arrays.asList(accessRights.split(ACCESS_RIGHTS_SEPARATOR)));
        }
    }

    private ObjectNode getOrCreateClaimsNode() {
        ObjectNode authenticationNode = getOrCreateAuthorizerNode();
        ObjectNode claimsNode = createChildNode(authenticationNode, CLAIMS_NODE);
        authenticationNode.set(CLAIMS_NODE, claimsNode);
        return claimsNode;
    }

    private ObjectNode createChildNode(ObjectNode parentNode, String childNodeName) {
        return Optional.ofNullable(parentNode)
            .map(parent -> parent.get(childNodeName))
            .filter(JsonNode::isObject)
            .map(node -> (ObjectNode) node)
            .orElse(objectMapper.createObjectNode());
    }

    private void initializeRequestContextIfNotExists() {
        if (isNull(requestContext)) {
            requestContext = objectMapper.createObjectNode();
        }
    }

    private ObjectNode getOrCreateAuthorizerNode() {
        initializeRequestContextIfNotExists();
        ObjectNode authorizerNode = createChildNode(requestContext, AUTHORIZER_NODE);
        requestContext.set(AUTHORIZER_NODE, authorizerNode);
        return authorizerNode;
    }
}
