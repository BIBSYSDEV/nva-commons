package no.unit.nva.events.models;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.file.Path;
import no.unit.nva.events.handlers.SampleEventDetail;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

public class AwsEventBridgeEventTest {

    private static final String EVENT_JSON = IoUtils.stringFromResources(Path.of("validEventBridgeEvent.json"));

    @Test
    public void objectMapperReturnsAwsEverBridgeDetailObjectForValidJson() throws JsonProcessingException {
        var event = parseEvent();
        assertThat(event, is(not(nullValue())));
        assertThat(event, doesNotHaveNullOrEmptyFields());
    }

    @Test
    public void objectMapperSerialized() throws JsonProcessingException {
        var event = parseEvent();
        assertThat(event, is(not(nullValue())));
        assertThat(event, doesNotHaveNullOrEmptyFields());
    }

    @Test
    public void equalsReturnsTrueForEquivalentFields() throws JsonProcessingException {
        var left = parseEvent();
        var right = parseEvent();
        assertThat(left, is(equalTo(right)));
    }

    @Test
    public void toStringIsValidJsonString() throws JsonProcessingException {
        var expected = parseEvent();
        var actual = parseEvent(expected.toString());
        assertThat(actual.toString(), is(equalTo(expected.toString())));
    }

    private AwsEventBridgeEvent<AwsEventBridgeDetail<SampleEventDetail>> parseEvent()
        throws JsonProcessingException {

        return parseEvent(EVENT_JSON);
    }

    private AwsEventBridgeEvent<AwsEventBridgeDetail<SampleEventDetail>> parseEvent(String eventString)
        throws JsonProcessingException {
        TypeReference<AwsEventBridgeEvent<AwsEventBridgeDetail<SampleEventDetail>>> detailTypeReference =
            new TypeReference<>() {};
        return JsonUtils.objectMapper.readValue(eventString, detailTypeReference);
    }
}