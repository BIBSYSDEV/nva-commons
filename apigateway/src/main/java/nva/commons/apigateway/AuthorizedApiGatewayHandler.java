package nva.commons.apigateway;

import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.Tag;
import java.util.List;
import java.util.Optional;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;

public abstract class AuthorizedApiGatewayHandler<I, O> extends ApiGatewayHandler<I, O> {

    public static final String ASSUMED_ROLE_ARN_ENV_VAR = "ASSUMED_ROLE_ARN";
    public static final String UNIDENTIFIED_SESSION = "Unidentified session";
    private static final int MIN_SESSION_DURATION_SECONDS = 900;
    private final AWSSecurityTokenService stsClient;

    protected AuthorizedApiGatewayHandler(Class<I> iclass,
                                          Environment environment,
                                          AWSSecurityTokenService stsClient
    ) {
        super(iclass, environment);
        this.stsClient = stsClient;
    }

    @Override
    protected final O processInput(I input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        try (STSAssumeRoleSessionCredentialsProvider credentialsProvider = credentialsProvider(requestInfo, context)) {
            return processInput(input, requestInfo, credentialsProvider, context);
        }
    }

    protected abstract O processInput(I input,
                                      RequestInfo requestInfo,
                                      STSAssumeRoleSessionCredentialsProvider credentialsProvider,
                                      Context context) throws ApiGatewayException;

    protected abstract List<Tag> sessionTags(RequestInfo requestInfo) throws ApiGatewayException;

    protected String assumedRoleArn() {
        return environment.readEnv(ASSUMED_ROLE_ARN_ENV_VAR);
    }

    private STSAssumeRoleSessionCredentialsProvider credentialsProvider(RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        return new STSAssumeRoleSessionCredentialsProvider.Builder(assumedRoleArn(), session(context))
            .withSessionTags(sessionTags(requestInfo))
            .withRoleSessionDurationSeconds(MIN_SESSION_DURATION_SECONDS)
            .withStsClient(stsClient).build();
    }

    private String session(Context context) {
        return Optional.ofNullable(context).map(Context::getAwsRequestId).orElse(UNIDENTIFIED_SESSION);
    }
}

