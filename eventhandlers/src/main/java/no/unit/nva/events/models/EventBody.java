package no.unit.nva.events.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface EventBody {

    String TOPIC = "topic";

    @JsonProperty(TOPIC)
    String getTopic();

    void setTopic(String topic);
}
