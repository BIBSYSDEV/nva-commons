package nva.commons.apigateway;

import static nva.commons.core.JsonUtils.dtoObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class RestConfig {

    /* default */ static final ObjectMapper defaultRestObjectMapper = dtoObjectMapper;

    private RestConfig() {
    }
}
