package no.unit.nva.stubs;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import java.util.UUID;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class FakeContext implements Context {

    private final String awsRequestId = UUID.randomUUID().toString();

    @Override
    public String getAwsRequestId() {
        return awsRequestId;
    }

    @Override
    public String getLogGroupName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLogStreamName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFunctionName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFunctionVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getInvokedFunctionArn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CognitoIdentity getIdentity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientContext getClientContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRemainingTimeInMillis() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMemoryLimitInMB() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LambdaLogger getLogger() {
        throw new UnsupportedOperationException();
    }
}
