package nva.commons.commons;

import static nva.commons.commons.JsonUtils.objectMapper;

public interface JsonSerializable {

    /**
     * JsonString.
     *
     * @return JsonString
     */
    default String toJsonString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
