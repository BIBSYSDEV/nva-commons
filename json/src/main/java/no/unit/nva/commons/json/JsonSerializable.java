package no.unit.nva.commons.json;

public interface JsonSerializable {

    /**
     * JsonString.
     *
     * @return JsonString
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    default String toJsonString() {
        try {
            return JsonUtils.singleLineObjectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
