package nva.commons.apigateway.testutils;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public class Base64Handler extends ApiGatewayHandler<Void,Void> {

    public Base64Handler() {
        this(new Environment());
    }

    public Base64Handler(Environment environment) {
        super(Void.class, environment);
    }

    @Override
    protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //no-op
    }

    @Override
    protected Void processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        setIsBase64Encoded(true);
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Void output) {
        return HttpURLConnection.HTTP_OK;
    }
}
