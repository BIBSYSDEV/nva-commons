package no.unit.nva.events.models;

import static no.unit.nva.events.handlers.EventHandlersConfig.defaultEventObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonSerializable;

/**
 * An {@link EventReference} is a reference to an event that has happened and the associated data are stored to a
 * location. The location is stored in the EventReference as a URI. The {@link EventReference} contains also the topic
 * of the event
 */
public class EventReference implements JsonSerializable, Event {

    public static final String TOPIC = "topic";
    public static final String URI = "uri";
    @JsonProperty(TOPIC)
    private final String topic;
    @JsonProperty(URI)
    private final URI uri;

    @JsonCreator
    public EventReference(@JsonProperty(TOPIC) String topic,
                          @JsonProperty(URI) URI uri) {
        this.topic = topic;
        this.uri = uri;
    }

    public static EventReference fromJson(String json) {
        return attempt(() -> defaultEventObjectMapper.readValue(json, EventReference.class)).orElseThrow();
    }

    @Override
    @JacocoGenerated
    public String getTopic() {
        return topic;
    }

    @JacocoGenerated
    @Override
    public void setTopic(String topic) {
        //do nothing;
    }

    @JacocoGenerated
    public URI getUri() {
        return uri;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getTopic(), getUri());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventReference)) {
            return false;
        }
        EventReference that = (EventReference) o;
        return Objects.equals(getTopic(), that.getTopic()) && Objects.equals(getUri(), that.getUri());
    }
}
