package no.unit.nva.events.handlers;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.events.EventsConfig.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class EventMessageParserTest {

  @Test
  void parseThrowsRuntimeExceptionWhenParsingFails() {
    String invalidJson = "invalidJson";
    EventParser<SampleEventDetail> eventParser = new EventParser<>(invalidJson, objectMapper);
    Executable action = () -> eventParser.parse(SampleEventDetail.class);
    RuntimeException exception = assertThrows(RuntimeException.class, action);
    assertThat(exception.getCause(), is(instanceOf(JsonParseException.class)));
  }

  @Test
  void parseParsesCorrectlyNestedGenericTypes() throws JsonProcessingException {
    OuterClass<MiddleClass<InnerClass<String>>> expectedDetail = createdNestedGenericsObject();
    AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> event =
        createEventWithDetail(expectedDetail);

    String eventJson = dtoObjectMapper.writeValueAsString(event);

    EventParser<OuterClass<MiddleClass<InnerClass<String>>>> parser =
        new EventParser<>(eventJson, objectMapper);

    AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> eventWithNestedTypes =
        parser.parse(OuterClass.class, MiddleClass.class, InnerClass.class, String.class);

    assertThat(eventWithNestedTypes.getDetail(), is(equalTo(expectedDetail)));
  }

  private AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> createEventWithDetail(
      OuterClass<MiddleClass<InnerClass<String>>> expectedDetail) {
    AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> event =
        new AwsEventBridgeEvent<>();
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

  /** Getters must stay public for Jackson to serialize them. */
  @SuppressWarnings("PMD.PublicMemberInNonPublicType")
  private static final class OuterClass<T> implements WithType {

    private T fieldA;

    public T getFieldA() {
      return fieldA;
    }

    public void setFieldA(T fieldA) {
      this.fieldA = fieldA;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getFieldA());
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
  }

  /** Getters must stay public for Jackson to serialize them. */
  @SuppressWarnings("PMD.PublicMemberInNonPublicType")
  private static final class MiddleClass<T> implements WithType {

    private T fieldB;

    public T getFieldB() {
      return fieldB;
    }

    public void setFieldB(T fieldB) {
      this.fieldB = fieldB;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getFieldB());
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
  }

  /** Getters must stay public for Jackson to serialize them. */
  @SuppressWarnings("PMD.PublicMemberInNonPublicType")
  private static final class InnerClass<T> implements WithType {

    private T fieldC;

    public T getFieldC() {
      return fieldC;
    }

    public void setFieldC(T fieldC) {
      this.fieldC = fieldC;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getFieldC());
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
  }
}
