package nva.commons.apigateway;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import java.net.HttpURLConnection;
import nva.commons.core.Environment;

public abstract class TestAuthorizedHandler extends AuthorizedApiGatewayHandler<Void, String> {

    public TestAuthorizedHandler(Environment environment,
                                 AWSSecurityTokenService stsClient) {
        super(Void.class, environment, stsClient);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return HttpURLConnection.HTTP_OK;
    }
}
