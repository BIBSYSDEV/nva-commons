package nva.commons.handlers;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import nva.commons.utils.Environment;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

public abstract class TestAuthorizedHandler extends AuthorizedHandler<Void, String> {

    public TestAuthorizedHandler(Environment environment,
                                 AWSSecurityTokenService stsClient) {
        super(Void.class, environment, stsClient, LoggerFactory.getLogger(TestAuthorizedHandler.class));
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpStatus.SC_OK;
    }
}