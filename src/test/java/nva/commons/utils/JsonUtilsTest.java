package nva.commons.utils;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAnd;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nva.commons.RequestBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JsonUtilsTest {

    public static final String JSON_UTILS_RESOURCES = "jsonutils";

    public static final String SAMPLE_VALUE = "foo";
    public static final String JSON_KEY = "keyInsideObject";
    public static final JsonNode JSON_OBJECT_WITH_VALUE = sampleJsonObjectWithSomeValue();
    public static final JsonNode JSON_OBJECT_WITHOUT_VALUE = sampleJsonObjectWithoutValue();
    public static final String UPPER_CASE_ENUM_JSON = "\"ANOTHER_ENUM\"";
    public static final String MIXED_CASE_ENUM_JSON = "\"SomE_enUm\"";

    @DisplayName("jsonParser serializes empty string as null")
    @Test
    public void jsonParserSerializedEmptyStringsAsNull() throws JsonProcessingException {
        readFieldFromJson("emptyStringValues.json");
    }

    @DisplayName("jsonParser serializes missing string as null")
    @Test
    public void jsonParserSerializedMissingValuesAsNull() throws JsonProcessingException {
        readFieldFromJson("missingStringValues.json");
    }

    private void readFieldFromJson(String fileName) throws JsonProcessingException {
        String json = IoUtils.stringFromResources(Path.of(JSON_UTILS_RESOURCES, fileName));
        RequestBody requestBody = objectMapper.readValue(json, RequestBody.class);
        assertThat(requestBody.getField2(), is(nullValue()));
    }

    @Test
    public void jsonParserSerializesOptionalPresentAsContainedObject() {
        JsonNode actual = serialize(objectWithSomeValue());
        assertThat(actual, is(equalTo(JSON_OBJECT_WITH_VALUE)));
    }

    @Test
    public void jsonParserSerializesOptionalNotPresentAsNull() {
        JsonNode actual = serialize(objectWithoutValue());
        assertThat(actual, is(equalTo(JSON_OBJECT_WITHOUT_VALUE)));
    }

    @Test
    public void jsonParserDeserializesOptionalNotPresent() {
        TestObjectForOptionals actual = deserialize(JSON_OBJECT_WITHOUT_VALUE);
        assertThat(actual.getTest(), is(equalTo(Optional.empty())));
        assertThat(actual.getTest(), isEmpty());
    }

    @Test
    public void jsonParserDeserializesOptionalPresent() {
        TestObjectForOptionals sut = deserialize(JSON_OBJECT_WITH_VALUE);
        assertThat(sut.getTest(), isPresentAnd(containsString(SAMPLE_VALUE)));
    }

    @Test
    public void canParseEnumIgnoringCase() throws JsonProcessingException {
        TestEnum testEnum = objectMapper.readValue(MIXED_CASE_ENUM_JSON, TestEnum.class);
        assertThat(testEnum, is(equalTo(TestEnum.SOME_ENUM)));
    }

    @Test
    public void writeEnumAsUpperCaseJsonString() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(TestEnum.ANOTHER_ENUM);
        assertThat(json, is(equalTo(UPPER_CASE_ENUM_JSON)));
    }

    @Test
    public void objectMapperSerializesEmptyStringAsNull() throws JsonProcessingException {
        Map<String,Object> mapToSerialize = new HashMap<>();
        mapToSerialize.put("emptyString", "");
        mapToSerialize.put("nullValue", null);
        String actualJson = objectMapper.writeValueAsString(mapToSerialize);
        ObjectNode sampleJsonObjectWithoutValue = objectMapper.createObjectNode();
        String expectedJson = objectMapper.writeValueAsString(sampleJsonObjectWithoutValue);
        assertThat(actualJson, is(equalTo(expectedJson)));
    }

    private TestObjectForOptionals objectWithoutValue() {
        return new TestObjectForOptionals(null);
    }

    private TestObjectForOptionals deserialize(JsonNode jsonObjectWithoutValue) {
        return objectMapper.convertValue(jsonObjectWithoutValue,
            TestObjectForOptionals.class);
    }

    private TestObjectForOptionals objectWithSomeValue() {
        return new TestObjectForOptionals(SAMPLE_VALUE);
    }

    private <T> JsonNode serialize(T objectWithNullValue) {
        return objectMapper.convertValue(objectWithNullValue, JsonNode.class);
    }

    private static JsonNode sampleJsonObjectWithSomeValue() {
        return objectMapper.createObjectNode().put(JSON_KEY, SAMPLE_VALUE);
    }

    private static JsonNode sampleJsonObjectWithoutValue() {
        return objectMapper.createObjectNode();
    }

    private  enum TestEnum {
        SOME_ENUM,
        ANOTHER_ENUM;
    }

    private static class TestObjectForOptionals {

        private final String test;

        @JsonCreator
        public TestObjectForOptionals(@JsonProperty(JSON_KEY) String test) {
            this.test = test;
        }

        @JsonProperty(JSON_KEY)
        public Optional<String> getTest() {
            return Optional.ofNullable(test);
        }
    }
}
