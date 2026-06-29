package nva.commons.apigateway;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class RestConfig {

  @SuppressWarnings("PMD.FieldNamingConventions")
  static final ObjectMapper defaultRestObjectMapper = dtoObjectMapper;

  private RestConfig() {}
}
