package nva.commons.utils;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;

public final class TestContext implements Context {

    public final TestLogger logger = new TestLogger();

    @Override
    @JacocoGenerated
    public String getAwsRequestId() {
        return null;
    }

    @Override
    @JacocoGenerated
    public String getLogGroupName() {
        return null;
    }

    @Override
    @JacocoGenerated
    public String getLogStreamName() {
        return null;
    }

    @Override
    @JacocoGenerated
    public String getFunctionName() {
        return null;
    }

    @Override
    @JacocoGenerated
    public String getFunctionVersion() {
        return null;
    }

    @Override
    @JacocoGenerated
    public String getInvokedFunctionArn() {
        return null;
    }

    @Override
    @JacocoGenerated
    public CognitoIdentity getIdentity() {
        return null;
    }

    @Override
    @JacocoGenerated
    public ClientContext getClientContext() {
        return null;
    }

    @Override
    @JacocoGenerated
    public int getRemainingTimeInMillis() {
        return 0;
    }

    @Override
    @JacocoGenerated
    public int getMemoryLimitInMB() {
        return 0;
    }

    @Override
    public TestLogger getLogger() {
        return logger;
    }
}
