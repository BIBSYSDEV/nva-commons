package nva.commons.handlers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import nva.commons.exceptions.GatewayResponseSerializingException;
import nva.commons.utils.Environment;
import nva.commons.utils.JsonUtils;
import org.slf4j.Logger;

public abstract class ApiGatewayHandler<I, O> extends RestRequestHandler<I, O> {

    public ApiGatewayHandler(Class<I> iclass, Logger logger) {
        super(iclass, logger);
    }

    public ApiGatewayHandler(Class<I> iclass, Environment environment, Logger logger) {
        super(iclass, environment, logger);
    }

    /**
     * This is the message for the success case. Sends a JSON string containing the response that APIGateway will send
     * to the user.
     *
     * @param input  the input object of class I
     * @param output the output object of class O
     * @throws IOException when serializing fails
     */
    @Override
    protected void writeOutput(I input, O output)
        throws IOException, GatewayResponseSerializingException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            GatewayResponse<O> gatewayResponse = new GatewayResponse<>(output, getSuccessHeaders(),
                getSuccessStatusCode(input, output));
            String responseJson = JsonUtils.objectMapper.writeValueAsString(gatewayResponse);
            writer.write(responseJson);
        }
    }
}
