package nva.commons.core;

import static nva.commons.core.JsonUtils.dtoObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class JsonSerializableTest {

    public static final String EXAMPLE_NAME = "Ola Nordmann";
    private static final ObjectNode JSON_OBJECT_WITH_NAME = dtoObjectMapper.createObjectNode().put("name",
                                                                                                   EXAMPLE_NAME);

    @Test
    void toJsonStringReturnsJsonObjectWithNameAsStringWhenFieldNameIsSet() throws JsonProcessingException {
        String dummyObject = new Dummy(EXAMPLE_NAME).toJsonString();
        JsonNode jsonNode = dtoObjectMapper.readValue(dummyObject, JsonNode.class);
        assertThat(jsonNode, is(equalTo(JSON_OBJECT_WITH_NAME)));
    }

    private static class Dummy implements JsonSerializable {

        private final String name;

        public Dummy() {
            this(null);
        }

        public Dummy(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}