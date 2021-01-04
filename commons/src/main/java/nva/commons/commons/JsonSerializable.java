package nva.commons.commons;

public interface JsonSerializable {

    /**
     * JsonString.
     *
     * @return JsonString
     */
    default String toJsonString() {
        try {
            return JsonUtils.objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
