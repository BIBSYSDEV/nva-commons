package no.unit.nva.testutils;

import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.awsjavakit.eventbridge.models.AwsEventBridgeDetail;
import com.github.awsjavakit.eventbridge.models.AwsEventBridgeEvent;
import java.io.InputStream;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.EventsConfig;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import software.amazon.awssdk.regions.Region;

@JacocoGenerated
@SuppressWarnings("FeatureEnvy")
public final class EventBridgeEventBuilder {

    public static final ObjectNode EMPTY_OBJECT = JsonUtils.dtoObjectMapper.createObjectNode();

    private EventBridgeEventBuilder() {

    }

    @JacocoGenerated
    public static <T> InputStream sampleLambdaDestinationsEvent(T eventBody) {
        var detail = createDestinationsEventDetailBody(eventBody);
        var event = sampleEventObject(detail);
        return Try.of(event)
                   .map(EventBridgeEventBuilder::toJson)
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }

    @JacocoGenerated
    public static <T> InputStream sampleEvent(T detail) {
        return Try.of(sampleEventObject(detail))
                   .map(EventBridgeEventBuilder::toJson)
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }

    private static <T> String toJson(AwsEventBridgeEvent<T> event) {
        return attempt(()->EventsConfig.objectMapper.writeValueAsString(event)).orElseThrow();
    }

    @JacocoGenerated
    public static <T> AwsEventBridgeEvent<T> sampleEventObject(T detail) {
        var event = new AwsEventBridgeEvent<T>();
        event.setDetail(detail);
        event.setVersion(randomString());
        event.setResources(List.of(randomString()));
        event.setId(randomString());
        event.setRegion(randomElement(Region.regions()));
        event.setTime(randomInstant());
        event.setSource(randomString());
        event.setAccount(randomString());
        return event;
    }

    @JacocoGenerated
    private static <T> AwsEventBridgeDetail<T> createDestinationsEventDetailBody(T eventBody) {
        return AwsEventBridgeDetail.<T>newBuilder()
                   .withRequestPayload(EMPTY_OBJECT)
                   .withTimestamp(randomInstant().toString())
                   .withVersion(randomString())
                   .withResponsePayload(eventBody)
                   .build();
    }
}
