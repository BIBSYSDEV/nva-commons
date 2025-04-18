package nva.commons.apigateway;

import static nva.commons.apigateway.RestConfig.defaultRestObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import nva.commons.apigateway.exceptions.GatewayResponseSerializingException;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings("PMD.ShortMethodName")
public class GatewayResponse<T> implements Serializable {

    private final String body;
    private final Map<String, String> headers;
    private final int statusCode;
    private final boolean isBase64Encoded;

    /**
     * Constructor for JSON deserializing.
     *
     * @param body       the body as JSON string
     * @param headers    the headers map.
     * @param statusCode the status code.
     */
    @JsonCreator
    public GatewayResponse(
        @JsonProperty("body") final String body,
        @JsonProperty("headers") final Map<String, String> headers,
        @JsonProperty("statusCode") final int statusCode,
        @JsonProperty("isBase64Encoded") final boolean isBase64Encoded) {
        this.body = body;
        this.headers = headers;
        this.statusCode = statusCode;
        this.isBase64Encoded = isBase64Encoded;
    }

    /**
     * Constructor for GatewayResponse.
     *
     * @param body         body of response
     * @param headers      http headers for response
     * @param statusCode   status code for response
     * @param objectMapper desired object mapper
     * @throws GatewayResponseSerializingException when serializing fails
     */
    @JacocoGenerated
    public GatewayResponse(T body, Map<String, String> headers, int statusCode, ObjectMapper objectMapper)
        throws GatewayResponseSerializingException {
        this(body, headers, statusCode, false, objectMapper);
    }

    /**
     * Constructor for GatewayResponse.
     *
     * @param body            body of response
     * @param headers         http headers for response
     * @param statusCode      status code for response
     * @param isBase64Encoded status code for response
     * @param objectMapper    desired object mapper
     * @throws GatewayResponseSerializingException when serializing fails
     */
    public GatewayResponse(T body, Map<String, String> headers, int statusCode, Boolean isBase64Encoded,
                           ObjectMapper objectMapper) throws GatewayResponseSerializingException {
        try {
            this.statusCode = statusCode;
            this.body = body instanceof String ? (String) body : objectMapper.writeValueAsString(body);
            this.headers = Map.copyOf(headers);
            this.isBase64Encoded = isBase64Encoded;
        } catch (JsonProcessingException e) {
            throw new GatewayResponseSerializingException(e);
        }
    }

    /**
     * Builder for GatewayResponse.
     *
     * @param outputStream content of response
     * @throws JsonProcessingException when serializing fails
     */
    public static <T> GatewayResponse<T> of(ByteArrayOutputStream outputStream) throws JsonProcessingException {
        var typeReference = new TypeReference<GatewayResponse<T>>() {
        };
        var json = outputStream.toString(StandardCharsets.UTF_8);
        return defaultRestObjectMapper.readValue(json, typeReference);
    }

    /**
     * Builder for GatewayResponse.
     *
     * @param inputStream content of response
     * @throws JsonProcessingException when serializing fails
     */
    public static <T> GatewayResponse<T> of(InputStream inputStream) throws IOException {
        var typeReference = new TypeReference<GatewayResponse<T>>() {
        };
        return defaultRestObjectMapper.readValue(inputStream, typeReference);
    }

    /**
     * Builder for GatewayResponse.
     *
     * @param jsonString content of response
     * @throws JsonProcessingException when serializing fails
     */
    public static <T> GatewayResponse<T> of(String jsonString) throws JsonProcessingException {
        var typeReference = new TypeReference<GatewayResponse<T>>() {
        };
        return defaultRestObjectMapper.readValue(jsonString, typeReference);
    }

    /**
     * Create GatewayResponse object from an output stream. Used when we call the method {@code handleRequest()} of a
     * Handler directly, and we want to read the output.
     *
     * @param outputStream the outputStream updated by the lambda handler
     * @param className    the class of the returned object
     * @param <T>          The type of the interface or class returned
     * @return the GatewayResponse containing the output of the handler
     * @throws JsonProcessingException when the OutputStream cannot be parsed to a JSON object.
     */
    public static <T> GatewayResponse<T> fromOutputStream(ByteArrayOutputStream outputStream, Class<T> className)
        throws JsonProcessingException {
        var json = outputStream.toString(StandardCharsets.UTF_8);
        return fromString(json, className);
    }

    @Deprecated(forRemoval = true)
    @JacocoGenerated
    public static <T> GatewayResponse<T> fromOutputStream(ByteArrayOutputStream outputStream)
        throws JsonProcessingException {
        var json = outputStream.toString(StandardCharsets.UTF_8);
        return fromString(json);
    }

    /**
     * Create GatewayResponse object from a String. Used when we call the method {@code handleRequest()} of a Handler
     * directly and we want to read the output. Usually the String is the output of an OutputStream.
     *
     * @param responseString a String containing the serialized GatwayResponse
     * @return the GatewayResponse containing the output of the handler
     * @throws JsonProcessingException when the OutputStream cannot be parsed to a JSON object.
     */
    public static <T> GatewayResponse<T> fromString(String responseString, Class<T> className)
        throws JsonProcessingException {

        return isString(className)
                   ? constructGatewayResponseWithStringBody(responseString)
                   : constructResponseWithJsonObjectBody(responseString);
    }

    @Deprecated(forRemoval = true)
    @JacocoGenerated
    public static <T> GatewayResponse<T> fromString(String responseString)
        throws JsonProcessingException {
        return constructResponseWithJsonObjectBody(responseString);
    }

    public String getBody() {
        return body;
    }

    @JsonIgnore
    public T getBodyAsInstance() throws JsonProcessingException {
        return defaultRestObjectMapper.readValue(body, new TypeReference<>() {
        });
    }

    /**
     * Parses the JSON body to an object.
     *
     * @param clazz the class of the body object
     * @return the body object.
     * @throws JsonProcessingException when JSON processing fails
     */
    public T getBodyObject(Class<T> clazz) throws JsonProcessingException {
        return defaultRestObjectMapper.readValue(body, clazz);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @SuppressWarnings("PMD.BooleanGetMethodName")
    public Boolean getIsBase64Encoded() {
        return isBase64Encoded;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(body, headers, statusCode, isBase64Encoded);
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (GatewayResponse<?>) o;
        return statusCode == that.statusCode
               && Objects.equals(body, that.body)
               && Objects.equals(headers, that.headers)
               && Objects.equals(isBase64Encoded, that.isBase64Encoded);
    }

    private static <T> boolean isString(Class<T> className) {
        return className.getTypeName().equals(String.class.getTypeName());
    }

    private static <T> GatewayResponse<T> constructResponseWithJsonObjectBody(String responseString)
        throws JsonProcessingException {
        TypeReference<GatewayResponse<T>> typeref = new TypeReference<>() {
        };
        return defaultRestObjectMapper.readValue(responseString, typeref);
    }

    private static <T> GatewayResponse<T> constructGatewayResponseWithStringBody(String responseString)
        throws JsonProcessingException {
        var jsonNode = defaultRestObjectMapper.readTree(responseString);

        var body = jsonNode.get("body").asText();
        var typeRef = new TypeReference<Map<String, String>>() {
        };
        var headers = defaultRestObjectMapper.convertValue(jsonNode.get("headers"), typeRef);
        var statusCode = jsonNode.get("statusCode").asInt();
        var hasIsBase64EncodedNode = jsonNode.has("isBase64Encoded");
        var isBase64Encoded = hasIsBase64EncodedNode && jsonNode.get("isBase64Encoded").asBoolean();

        return (GatewayResponse<T>)
                   attempt(() -> new GatewayResponse(body, headers, statusCode, isBase64Encoded,
                                                     defaultRestObjectMapper)).orElseThrow();
    }
}
