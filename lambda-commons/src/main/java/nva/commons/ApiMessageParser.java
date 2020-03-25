package nva.commons;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import nva.commons.utils.JsonUtils;

/**
 * Not intended for use outside the {@link ApiGatewayHandler}. Class for parsing a message from ApiGateway.
 *
 * @param <T> Class of the object we want to extract from the API message
 */
public class ApiMessageParser<T> {

    private final transient ObjectMapper mapper = JsonUtils.jsonParser;

    public RequestInfo getRequestInfo(String inputString) throws IOException {
        RequestInfo requestInfo = mapper.readValue(inputString,RequestInfo.class);
        return requestInfo;
    }

    public T getBodyElementFromJson(String inputString, Class<T> tclass) throws IOException {

        Optional<JsonNode> tree = Optional.ofNullable(mapper.readTree(new StringReader(inputString)));
        JsonNode body = tree.map(node -> node.get("body")).orElse(null);

        if (tclass.equals(String.class)) {
            return (T) body.asText();
        } else {
            T request = null;
            if (body != null) {
                // body should always be a string for a lambda function connected to the API
                if (body.isValueNode()) {
                    request = parseBody(mapper, body.asText(), tclass);
                } else {
                    request = parseBody(mapper, body, tclass);
                }
            }

            return request;
        }
    }

    private T parseBody(ObjectMapper mapper, JsonNode node, Class<T> tclass) throws IOException {
        return mapper.readValue(new TreeTraversingParser(node), tclass);
    }

    private T parseBody(ObjectMapper mapper, String json, Class<T> tclass) throws IOException {

        T object = mapper.readValue(json, tclass);
        return object;
    }
}
