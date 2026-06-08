package no.unit.nva.events.models;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface EventBody {

  String TOPIC = "topic";

  @JsonProperty(TOPIC)
  String getTopic();
}
