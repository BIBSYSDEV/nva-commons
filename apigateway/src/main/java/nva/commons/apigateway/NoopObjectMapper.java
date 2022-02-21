package nva.commons.apigateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This ObjectMapper is used when you want to return raw string data from apigateway handler.
 */
public class NoopObjectMapper extends ObjectMapper {

    @Override
    public String writeValueAsString(Object value) throws JsonProcessingException {
        if (value instanceof String) {
            return (String) value;
        }
        return super.writeValueAsString(value);
    }


}
