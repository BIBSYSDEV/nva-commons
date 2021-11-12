package no.unit.nva.events.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface EventBridgeEvent {

    @JsonProperty("topic")
    String getTopic();

    void setTopic(String topic);
}
