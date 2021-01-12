package no.unit.commons.apigateway.authentication;

import static nva.commons.attempt.Try.attempt;
import static nva.commons.commons.JsonUtils.objectMapper;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Optional;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.RestRequestHandler;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.attempt.Failure;
import nva.commons.commons.Environment;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for implementing a Request Authorizer.
 * Implementation is based on the AWS examples found in the following page :
 * {@see <a href=https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-use-lambda-authorizer.html>}.
 */

public abstract class RequestAuthorizer extends RestRequestHandler<Void, AuthorizerResponse> {

    public static final String EXECUTE_API_ACTION = "execute-api:Invoke";
    public static final String ALLOW_EFFECT = "Allow";
    public static final String ANY_RESOURCE = "*";
    public static final String ANY_HTTP_METHOD = ANY_RESOURCE;
    public static final String ALL_PATHS = ANY_RESOURCE;
    public static final String PATH_DELIMITER = "/";
    public static final int API_GATEWAY_IDENTIFIER_INDEX = 0;
    public static final int STAGE_INDEX = 1;
    private static final String DENY_EFFECT = "Deny";
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public RequestAuthorizer(Environment environment) {
        super(Void.class, environment, LoggerFactory.getLogger(RequestAuthorizer.class));
    }

    @Override
    protected AuthorizerResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        logger.debug("Requesting authorizing: " + principalId());
        secretCheck(requestInfo);
        String resource = formatPolicyResource(requestInfo.getMethodArn());

        AuthPolicy authPolicy = createAllowAuthPolicy(resource);

        return createResponse(authPolicy);
    }

    /**
     * This method can be overridden to change the template of the accessed resource. The resource that access will be
     * allowed to. It can contain wildcards.
     *
     * <p>Example methodARN:
     * arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod/GET/some/path/to/resource Example output:
     * arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod\/*\/* <br/> Another possible output is: "*"
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

    protected AuthPolicy createAllowAuthPolicy(String methodArn) throws ForbiddenException {
        logger.info("Allowed to access: " + principalId());
        StatementElement statement = StatementElement.newBuilder()
            .withResource(methodArn)
            .withAction(EXECUTE_API_ACTION)
            .withEffect(ALLOW_EFFECT)
            .build();
        return AuthPolicy.newBuilder().withStatement(Collections.singletonList(statement)).build();
    }

    protected AuthPolicy createDenyAuthPolicy() throws ForbiddenException {
        logger.info("Denied access: " + principalId());
        StatementElement statement = StatementElement.newBuilder()
            .withResource(ANY_RESOURCE)
            .withAction(EXECUTE_API_ACTION)
            .withEffect(DENY_EFFECT)
            .build();
        return AuthPolicy.newBuilder().withStatement(Collections.singletonList(statement)).build();
    }

    protected abstract String principalId() throws ForbiddenException;

    protected abstract String fetchSecret() throws ForbiddenException;

    protected void secretCheck(RequestInfo requestInfo) throws ForbiddenException {
        Optional.ofNullable(requestInfo.getHeaders().get(AUTHORIZATION_HEADER))
            .map(this::validateSecret)
            .filter(this::validationSucceeded)
            .orElseThrow(ForbiddenException::new);
    }

    @Override
    protected void writeOutput(Void input, AuthorizerResponse output)
        throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String responseJson = objectMapper.writeValueAsString(output);
            writer.write(responseJson);
        }
    }

    @Override
    protected void writeExpectedFailure(Void input, ApiGatewayException exception, String requestId)
        throws IOException {
        try {
            writeFailure();
        } catch (ForbiddenException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void writeUnexpectedFailure(Void input, Exception exception, String requestId)
        throws IOException {
        try {
            writeFailure();
        } catch (ForbiddenException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, AuthorizerResponse output) {
        return HttpURLConnection.HTTP_OK;
    }

    private Boolean validationSucceeded(Boolean check) {
        return check;
    }

    private boolean validateSecret(String clientSecret) {
        String correctSecret = attempt(this::fetchSecret).orElseThrow(this::logErrorAndThrowException);
        return clientSecret.equals(correctSecret);
    }

    private void writeFailure() throws IOException, ForbiddenException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String principalId = attempt(this::principalId).orElseThrow(this::logErrorAndThrowException);
            AuthorizerResponse denyResponse = AuthorizerResponse
                .newBuilder()
                .withPrincipalId(principalId)
                .withPolicyDocument(createDenyAuthPolicy())
                .build();
            String response = objectMapper.writeValueAsString(denyResponse);
            writer.write(response);
        }
    }

    private AuthorizerResponse createResponse(AuthPolicy authPolicy) throws ForbiddenException {
        return AuthorizerResponse.newBuilder()
            .withPrincipalId(principalId())
            .withPolicyDocument(authPolicy)
            .build();
    }

    private RuntimeException logErrorAndThrowException(Failure<String> failure) {
        logger.error(failure.getException().getMessage(), failure.getException());
        return new RuntimeException(failure.getException());
    }
}
