package no.unit.nva.events.models;

import static no.unit.nva.events.handlers.EventHandlersConfig.defaultEventObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.JacocoGenerated;

/**
 * An {@link EventReference} is a reference to an event that has happened and the associated data are stored to a
 * location. The location is stored in the EventReference as a URI. The {@link EventReference} contains also the topic
 * of the event
 */
public class EventReference implements JsonSerializable, EventBody {

    public static final String TOPIC = "topic";
    public static final String URI = "uri";
    public static final String SUBTOPIC = "subtopic";
    @JsonProperty(TOPIC)
    private final String topic;
    @JsonProperty(SUBTOPIC)
    private final String subtopic;
    @JsonProperty(URI)
    private final URI uri;

    @JsonCreator
    public EventReference(@JsonProperty(TOPIC) String topic,
                          @JsonProperty(SUBTOPIC) String subtopic,
                          @JsonProperty(URI) URI uri) {
        this.topic = topic;
        this.subtopic = subtopic;
        this.uri = uri;
    }

    public EventReference(String topic, URI uri) {
        this(topic, null, uri);
    }

    public static EventReference fromJson(String json) {
        return attempt(() -> defaultEventObjectMapper.readValue(json, EventReference.class)).orElseThrow();
    }

    @JacocoGenerated
    public String getSubtopic() {
        return subtopic;
    }

    @Override
    @JacocoGenerated
    public String getTopic() {
        return topic;
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
