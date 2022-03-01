package no.unit.commons.apigateway.authentication;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayCustomAuthorizerEvent;
import java.util.Collections;
import java.util.Optional;
import nva.commons.core.attempt.Failure;
import nva.commons.core.exceptions.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for implementing a Request Authorizer. Implementation is based on the AWS examples found in the
 * following page : "https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-use-lambda-authorizer
 * .html".
 */

public abstract class RequestAuthorizer implements RequestHandler<APIGatewayCustomAuthorizerEvent, AuthorizerResponse> {

    public static final String EXECUTE_API_ACTION = "execute-api:Invoke";
    public static final String ALLOW_EFFECT = "Allow";
    public static final String ANY_RESOURCE = "*";
    public static final String ANY_HTTP_METHOD = ANY_RESOURCE;
    public static final String ALL_PATHS = ANY_RESOURCE;
    public static final String PATH_DELIMITER = "/";
    public static final int API_GATEWAY_IDENTIFIER_INDEX = 0;
    public static final int STAGE_INDEX = 1;
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String COULD_NOT_READ_PRINCIPAL_ID_ERROR = "Error while trying to get the principal ID.";
    private static final String DENY_EFFECT = "Deny";
    private static final Logger logger = LoggerFactory.getLogger(RequestAuthorizer.class);

    protected RequestAuthorizer() {

    }

    @Override
    public AuthorizerResponse handleRequest(APIGatewayCustomAuthorizerEvent input, Context context) {
        return attempt(() -> callerIsAllowedToPerformAction(input))
            .map(callerIsAuthorized -> formatPolicyResource(input.getMethodArn()))
            .map(this::createAllowAuthPolicy)
            .map(this::createResponse)
            .orElse(fail -> createForbiddenResponse(fail.getException()));
    }

    /**
     * This method can be overridden to change the template of the accessed resource. The resource that access will be
     * allowed to. It can contain wildcards.
     *
     * <p>Example methodARN:
     * arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod/GET/some/path/to/resource Example output:
     * arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod\/*\/*
     * <p>
     * Another possible output is: "*"
     * </p>
     *
     * @param methodArn the method ARN as provided by the API gateway
     * @return a resource for the policy
     */
    protected String formatPolicyResource(String methodArn) {
        String[] resourcePathComponents = methodArn.split(PATH_DELIMITER);
        String apiGateway = resourcePathComponents[API_GATEWAY_IDENTIFIER_INDEX];
        String stage = resourcePathComponents[STAGE_INDEX];

        return String.join(PATH_DELIMITER, apiGateway, stage, ANY_HTTP_METHOD, ALL_PATHS);
    }

    protected AuthPolicy createAllowAuthPolicy(String methodArn) {
        StatementElement statement = StatementElement.newBuilder()
            .withResource(methodArn)
            .withAction(EXECUTE_API_ACTION)
            .withEffect(ALLOW_EFFECT)
            .build();
        return AuthPolicy.newBuilder().withStatement(Collections.singletonList(statement)).build();
    }

    protected AuthPolicy createDenyAuthPolicy() {
        StatementElement statement = StatementElement.newBuilder()
            .withResource(ANY_RESOURCE)
            .withAction(EXECUTE_API_ACTION)
            .withEffect(DENY_EFFECT)
            .build();
        return AuthPolicy.newBuilder().withStatement(Collections.singletonList(statement)).build();
    }

    protected abstract String principalId();

    protected abstract String fetchSecret() throws ForbiddenException;

    protected boolean callerIsAllowedToPerformAction(APIGatewayCustomAuthorizerEvent requestInfo)
        throws ForbiddenException {
        return Optional.ofNullable(requestInfo.getHeaders().get(AUTHORIZATION_HEADER))
            .map(this::validateSecret)
            .filter(this::validationSucceeded)
            .orElseThrow(ForbiddenException::new);
    }

    private String readPrincipalId() {
        try {
            return principalId();
        } catch (Exception e) {
            throw new RuntimeException(COULD_NOT_READ_PRINCIPAL_ID_ERROR, e);
        }
    }

    private AuthorizerResponse createForbiddenResponse(Exception exception) {
        logger.warn(ExceptionUtils.stackTraceInSingleLine(exception));
        return AuthorizerResponse
            .newBuilder()
            .withPrincipalId(readPrincipalId())
            .withPolicyDocument(createDenyAuthPolicy())
            .build();
    }

    private Boolean validationSucceeded(Boolean check) {
        return check;
    }

    private boolean validateSecret(String clientSecret) {
        String correctSecret = attempt(this::fetchSecret).orElseThrow(this::logErrorAndThrowException);
        return clientSecret.equals(correctSecret);
    }

    private AuthorizerResponse createResponse(AuthPolicy authPolicy) {
        return AuthorizerResponse.newBuilder()
            .withPrincipalId(readPrincipalId())
            .withPolicyDocument(authPolicy)
            .build();
    }

    private RuntimeException logErrorAndThrowException(Failure<String> failure) {
        logger.error(failure.getException().getMessage(), failure.getException());
        return new RuntimeException(failure.getException());
    }
}
