package no.unit.nva.auth;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class JsonConfig {

  private static final JSON MAPPER =
      JSON.builder().register(JacksonAnnotationExtension.std).build();

  private JsonConfig() {}

  public static <T> T beanFrom(Class<T> type, String source) {
    return attempt(() -> MAPPER.beanFrom(type, source)).orElseThrow();
  }

  public static <T> String asString(T object) {
    return attempt(() -> MAPPER.asString(object)).orElseThrow();
  }
}
