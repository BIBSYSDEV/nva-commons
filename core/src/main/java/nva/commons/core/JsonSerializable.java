package nva.commons.core;

import static nva.commons.core.JsonUtils.objectMapperSingleLine;

public interface JsonSerializable {

    /**
     * JsonString.
     *
     * @return JsonString
     */
    default String toJsonString() {
        try {
            return objectMapperSingleLine.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
