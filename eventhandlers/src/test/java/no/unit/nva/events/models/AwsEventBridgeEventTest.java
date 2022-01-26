package no.unit.nva.events.models;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.events.handlers.SampleEventDetail.propertyNamesOfEmptyFields;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.file.Path;
import no.unit.nva.events.handlers.SampleEventDetail;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

class AwsEventBridgeEventTest {

    private static final String EVENT_JSON = IoUtils.stringFromResources(Path.of("validEventBridgeEvent.json"));

    @Test
    void objectMapperReturnsAwsEverBridgeDetailObjectForValidJson() throws JsonProcessingException {
        var event = parseEvent();
        assertThat(event, is(not(nullValue())));
        assertThat(event,
                   doesNotHaveEmptyValuesIgnoringFields(propertyNamesOfEmptyFields("detail")));
    }

    @Test
    void equalsReturnsTrueForEquivalentFields() throws JsonProcessingException {
        var left = parseEvent();
        var right = parseEvent();
        assertThat(left, is(equalTo(right)));
    }

    @Test
    void toStringIsValidJsonString() throws JsonProcessingException {
        var expected = parseEvent();
        var actual = parseEvent(expected.toString());
        assertThat(actual.toString(), is(equalTo(expected.toString())));
    }

    private AwsEventBridgeEvent<SampleEventDetail> parseEvent()
        throws JsonProcessingException {

        return parseEvent(EVENT_JSON);
    }

    private AwsEventBridgeEvent<SampleEventDetail> parseEvent(String eventString)
        throws JsonProcessingException {
        TypeReference<AwsEventBridgeEvent<SampleEventDetail>> detailTypeReference =
            new TypeReference<>() {
            };
        return dtoObjectMapper.readValue(eventString, detailTypeReference);
    }
}