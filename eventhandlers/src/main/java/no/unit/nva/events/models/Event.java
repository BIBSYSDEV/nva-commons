package no.unit.nva.events.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Event {

    @JsonProperty("topic")
    String getTopic();

    void setTopic(String topic);
}
