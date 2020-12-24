package nva.commons.apigateway;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import nva.commons.commons.Environment;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

public abstract class TestAuthorizedHandler extends AuthorizedApiGatewayHandler<Void, String> {

    public TestAuthorizedHandler(Environment environment,
                                 AWSSecurityTokenService stsClient) {
        super(Void.class, environment, stsClient, LoggerFactory.getLogger(TestAuthorizedHandler.class));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpStatus.SC_OK;
    }
}
