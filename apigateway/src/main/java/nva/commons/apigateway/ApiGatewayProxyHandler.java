package nva.commons.apigateway;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.http.HttpClient;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

/**
 * An extension of ApiGatewayHandler where you are also able to specify the http status-code.
 *
 * @param <I> Class of the object in the body field of the ApiGateway message.
 * @param <O> Class of the response object.
 */
public abstract class ApiGatewayProxyHandler<I, O> extends ApiGatewayHandler<I, O> {

    private Integer statusCode;

    @JacocoGenerated
    protected ApiGatewayProxyHandler(Class<I> iclass) {
        this(iclass, new Environment(), HttpClient.newBuilder().build());
    }
    
    @JacocoGenerated
    protected ApiGatewayProxyHandler(Class<I> iclass, Environment environment, HttpClient httpClient) {
        super(iclass, environment, httpClient);
    }

    @Override
    protected O processInput(I input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var result = processProxyInput(input, requestInfo, context);
        statusCode = result.getStatusCode();
        return result.getBody();
    }

    @Override
    protected Integer getSuccessStatusCode(I input, O output) {
        if (statusCode == null) {
            throw new IllegalStateException("getSuccessStatusCode was called before processInput");
        }
        return statusCode;
    }

    /**
     * Implements the main logic of the handler. Any exception thrown by this method will be handled by {@link
     * RestRequestHandler#handleExpectedException} method.
     *
     * @param input       The input object to the method. Usually a deserialized json.
     * @param requestInfo Request headers and path.
     * @param context     the ApiGateway context.
     *
     * @return A Pair which consists of:
     *     - the Response body that is going to be serialized in json
     *     - the http response code
     * @throws ApiGatewayException all exceptions are caught by writeFailure and mapped to error codes through the
     *                             method {@link RestRequestHandler#getFailureStatusCode}
     */
    protected abstract ProxyResponse<O> processProxyInput(I input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException;
}
