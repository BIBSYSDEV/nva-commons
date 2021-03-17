package nva.commons.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAnd;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;

public class JsonUtilsTest {

    public static final String JSON_UTILS_RESOURCES = "jsonutils";

    public static final String SAMPLE_VALUE = "foo";
    public static final String JSON_KEY = "keyInsideObject";
    public static final JsonNode JSON_OBJECT_WITH_VALUE = sampleJsonObjectWithSomeValue();
    public static final JsonNode JSON_OBJECT_WITHOUT_VALUE = sampleJsonObjectWithoutValue();
    public static final String UPPER_CASE_ENUM_JSON = "\"ANOTHER_ENUM\"";
    public static final String MIXED_CASE_ENUM_JSON = "\"SomE_enUm\"";

    public static final String EMPTY_STRING = "emptyString";
    public static final String NON_EMPTY_STRING = "nonEmptyString";
    public static final String NULL_LIST = "nullList";
    public static final String EMPTY_LIST = "emptyList";
    public static final String NULL_MAP = "nullMap";
    public static final String EMPTY_MAP = "emptyMap";
    public static final String NULL_STRING = "nullString";
    private static final String POJO_WITH_MISSING_VALUES = "missingStringValues.json";
    private static final String POJO_WITH_EMPTY_VALUES = "emptyStringValues.json";

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
        SamplePojo samplePojo = objectMapper.readValue(json, SamplePojo.class);
        assertThat(samplePojo.getField2(), is(nullValue()));
    }

    @Test
    public void jsonParserSerializesOptionalPresentAsContainedObject() {
        JsonNode actual = serialize(objectWithSomeValue());
        assertThat(actual, is(equalTo(JSON_OBJECT_WITH_VALUE)));
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
    public void objectMapperSerializesInstantAsIsoStrings() throws JsonProcessingException {

        String instantString = "2020-12-29T19:23:09.357248Z";

        Instant timestamp = Instant.parse(instantString);
        String jsonNow = objectMapper.writeValueAsString(timestamp);

        String expectedString = "\"" + instantString + "\"";
        assertThat(jsonNow, is(equalTo(expectedString)));
    }

    @Test
    public void objectMapperSerializesNullStringAsNull() throws JsonProcessingException {
        String expectedJson = IoUtils.stringFromResources(Path.of(JSON_UTILS_RESOURCES, POJO_WITH_MISSING_VALUES));
        SamplePojo sampleWithMissingValuesPojo = objectMapper.readValue(expectedJson, SamplePojo.class);
        String actualJson = objectMapper.writeValueAsString(sampleWithMissingValuesPojo);
        assertThat(actualJson, is(equalTo(expectedJson)));
    }

    @Test
    public void objectMapperSerializesEmptyStringAsNull() throws JsonProcessingException {
        String expectedJson = IoUtils.stringFromResources(Path.of(JSON_UTILS_RESOURCES, POJO_WITH_MISSING_VALUES));
        String sampleWithEmptyValueJson =
                IoUtils.stringFromResources(Path.of(JSON_UTILS_RESOURCES, POJO_WITH_EMPTY_VALUES));
        SamplePojo sampleWithMissingValuesPojo = objectMapper.readValue(sampleWithEmptyValueJson, SamplePojo.class);
        String actualJson = objectMapper.writeValueAsString(sampleWithMissingValuesPojo);
        assertThat(actualJson, is(equalTo(expectedJson)));
    }

    @ParameterizedTest
    @ValueSource(strings={POJO_WITH_MISSING_VALUES, POJO_WITH_EMPTY_VALUES})
    public void objectMapperDeserializesEmptyStringAsNull(String resourceInput) throws JsonProcessingException {
        String jsonString = IoUtils.stringFromResources(Path.of(JSON_UTILS_RESOURCES,resourceInput));
        SamplePojo actualObject = objectMapper.readValue(jsonString,SamplePojo.class);
        assertThat(actualObject.getField2(),is(nullValue()));
        assertThat(actualObject.getField1(),is(not(nullValue())));
    }

    @Test
    public void objectMapperWithEmptySerializesAllEmptyFields() throws JsonProcessingException {
        TestForEmptyFields testObj = new TestForEmptyFields();
        String json = objectMapper.writeValueAsString(testObj);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has(EMPTY_STRING), is(true));
        assertThat(node.has(NULL_STRING), is(true));

        assertThat(node.has(EMPTY_LIST), is(true));
        assertThat(node.has(NULL_LIST), is(true));

        assertThat(node.has(EMPTY_MAP), is(true));
        assertThat(node.has(NULL_MAP), is(true));
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

    private enum TestEnum {
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

    private static class TestForEmptyFields {

        @JsonProperty(EMPTY_STRING)
        private final String emptyString = "";

        @JsonProperty(NULL_STRING)
        private final String nullString = null;

        @JsonProperty(NON_EMPTY_STRING)
        private final String nonEmptyString = "nonEmptyString";

        @JsonProperty(NULL_LIST)
        private final List<String> nullList = null;

        @JsonProperty(EMPTY_LIST)
        private final List<String> emptyList = Collections.emptyList();

        @JsonProperty(NULL_MAP)
        private final Map<String, Object> nullMap = null;
        @JsonProperty(EMPTY_MAP)
        private final Map<String, Object> emptyMap = Collections.emptyMap();

        public String getNullString() {
            return nullString;
        }

        public String getNonEmptyString() {
            return nonEmptyString;
        }

        public List<String> getNullList() {
            return nullList;
        }

        public List<String> getEmptyList() {
            return emptyList;
        }
    }
}
