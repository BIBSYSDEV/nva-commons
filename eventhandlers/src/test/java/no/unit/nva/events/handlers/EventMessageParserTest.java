package no.unit.nva.events.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class EventMessageParserTest {

    @Test
    public void parseThrowsRuntimeExceptionWhenParsingFails() {
        String invalidJson = "invalidJson";
        EventParser<SampleEventDetail> eventParser = new EventParser<>(invalidJson);
        Executable action = () -> eventParser.parse(SampleEventDetail.class);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getCause(), is(instanceOf(JsonParseException.class)));
    }

    @Test
    public void parseParsesCorrectlyNestedGenericTypes() throws JsonProcessingException {
        OuterClass<MiddleClass<InnerClass<String>>> expectedDetail = createdNestedGenericsObject();
        AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> event = createEventWithDetail(expectedDetail);

        String eventJson = JsonUtils.objectMapper.writeValueAsString(event);

        EventParser<OuterClass<MiddleClass<InnerClass<String>>>> parser = new EventParser<>(eventJson);

        AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> eventWithNestedTypes =
            parser.parse(OuterClass.class, MiddleClass.class, InnerClass.class, String.class);

        assertThat(eventWithNestedTypes.getDetail(), is(equalTo(expectedDetail)));
    }

    private AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> createEventWithDetail(
        OuterClass<MiddleClass<InnerClass<String>>> expectedDetail) {
        AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> event = new AwsEventBridgeEvent<>();
        event.setDetail(expectedDetail);
        event.setAccount("someAccount");
        event.setId("SomeId");
        return event;
    }

    private OuterClass<MiddleClass<InnerClass<String>>> createdNestedGenericsObject() {
        InnerClass<String> bottom = new InnerClass<>();
        bottom.setFieldC("Hello");
        MiddleClass<InnerClass<String>> middle = new MiddleClass<>();
        middle.setFieldB(bottom);
        OuterClass<MiddleClass<InnerClass<String>>> top = new OuterClass<>();
        top.setFieldA(middle);
        return top;
    }

    private static class OuterClass<InputType> implements WithType {

        private InputType fieldA;

        public InputType getFieldA() {
            return fieldA;
        }

        public void setFieldA(InputType fieldA) {
            this.fieldA = fieldA;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            OuterClass<?> outerClass = (OuterClass<?>) o;
            return Objects.equals(getFieldA(), outerClass.getFieldA());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFieldA());
        }
    }

    private static class MiddleClass<InputType> implements WithType {

        private InputType fieldB;

        public InputType getFieldB() {
            return fieldB;
        }

        public void setFieldB(InputType fieldB) {
            this.fieldB = fieldB;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MiddleClass<?> middleClass = (MiddleClass<?>) o;
            return Objects.equals(getFieldB(), middleClass.getFieldB());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFieldB());
        }
    }

    private static class InnerClass<InputType> implements WithType {

        private InputType fieldC;

        public InputType getFieldC() {
            return fieldC;
        }

        public void setFieldC(InputType fieldC) {
            this.fieldC = fieldC;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InnerClass<?> innerClass = (InnerClass<?>) o;
            return Objects.equals(getFieldC(), innerClass.getFieldC());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFieldC());
        }
    }
}