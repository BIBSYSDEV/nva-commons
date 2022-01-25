package no.unit.commons.apigateway.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

public final class AuthorizerObjectMapperConfig {

    /* default */ static final ObjectMapper authorizerObjectMapper = JsonUtils.dtoObjectMapper;

    private AuthorizerObjectMapperConfig() {
    }
}
