package nva.commons.handlers.authentication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

class EventTest {

    private static final String KEY1 = "key1";
    private static final String COMPOSITE_OBJECT_KEY = "key2";
    private static final String KEY3 = "key3";
    private static final String KEY4 = "key4";
    private static final String ARRAY_OBJECT_KEY = "key5";

    private static final String VALUE1 = "VALUE1";
    private static final String VALUE3 = "VALUE3";
    private static final String VALUE4 = "VALUE4";

    Map<String, String> compositeObject;
    List<Map<String, String>> arrayObject;

    public EventTest() {
        compositeObject = new HashMap<>();
        compositeObject.put(KEY3, VALUE3);
        compositeObject.put(KEY4, VALUE4);
        arrayObject = List.of(compositeObject, compositeObject, compositeObject);
    }

    @Test
    public void eventShouldContainAnyJsonObject() throws JsonProcessingException {
        Map<String, Object> sampleJson = sampleJson();
        String jsonString = JsonUtils.objectMapper.writeValueAsString(sampleJson);
        Event event = JsonUtils.objectMapper.readValue(jsonString, Event.class);
        assertThat(event.getProperties().size(), is(greaterThan(0)));
        assertThat(event.getProperties().get(KEY1), is(instanceOf(String.class)));
        assertThat(event.getProperties().get(COMPOSITE_OBJECT_KEY), is(instanceOf(Map.class)));

        Map<String, String> compositeObject = (Map<String, String>) event.getProperties().get(COMPOSITE_OBJECT_KEY);
        assertThat(compositeObject.size(), is(not(equalTo(0))));
        assertThat(event.getProperties().get(ARRAY_OBJECT_KEY), is(instanceOf(List.class)));

        List<String> arrayObject = (List<String>) event.getProperties().get(ARRAY_OBJECT_KEY);
        assertThat(arrayObject.size(), is(not(equalTo(0))));
    }

    private Map<String, Object> sampleJson() {
        Map<String, Object> sampleJson = new HashMap<>();
        sampleJson.put(KEY1, VALUE1);
        Map<String, String> compositeObject = new HashMap<>();
        compositeObject.put(KEY3, VALUE3);
        compositeObject.put(KEY3, VALUE4);
        sampleJson.put(COMPOSITE_OBJECT_KEY, compositeObject);
        sampleJson.put(ARRAY_OBJECT_KEY, arrayObject);
        return sampleJson;
    }
}