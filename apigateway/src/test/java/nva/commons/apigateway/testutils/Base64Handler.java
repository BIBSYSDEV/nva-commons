package nva.commons.apigateway.testutils;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class Base64Handler extends ApiGatewayHandler<Void,Void> {

    public Base64Handler() {
        super(Void.class);
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
