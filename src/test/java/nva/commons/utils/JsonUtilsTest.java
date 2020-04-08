package nva.commons.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.file.Path;
import nva.commons.RequestBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JsonUtilsTest {

    public static final String JSON_UTILS_RESOURCES = "jsonutils";

    @DisplayName("jsonParser serializes empty string as null")
    @Test
    public void jsonParserSerializedEmptyStringsAsNull() throws JsonProcessingException {
        String json = IoUtils.stringFromResources(Path.of(JSON_UTILS_RESOURCES, "emptyStringValues.json"));
        RequestBody requestBody = JsonUtils.jsonParser.readValue(json, RequestBody.class);
        assertThat(requestBody.getField2(), is(nullValue()));
    }

    @DisplayName("jsonParser serializes empty string as null")
    @Test
    public void jsonParserSerializedMissingValuesAsNull() throws JsonProcessingException {
        String json = IoUtils.stringFromResources(Path.of(JSON_UTILS_RESOURCES, "missingStringValues.json"));
        RequestBody requestBody = JsonUtils.jsonParser.readValue(json, RequestBody.class);
        assertThat(requestBody.getField2(), is(nullValue()));
    }
}