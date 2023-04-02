package no.unit.nva.events.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public interface EventBody {

    String TOPIC_FIELD = "topic";
    String IDENTIFIER_FIELD = "identifier";

    @JsonProperty(TOPIC_FIELD)
    String getTopic();

    @JsonProperty(IDENTIFIER_FIELD)
    UUID getIdentifier();
}
