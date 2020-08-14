package nva.commons.handlers.authentication;

import static java.util.Objects.nonNull;
import static nva.commons.utils.JsonUtils.objectMapper;
import static nva.commons.utils.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.exceptions.ForbiddenException;
import nva.commons.handlers.RequestInfo;
import nva.commons.handlers.RestRequestHandler;
import nva.commons.utils.Environment;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.attempt.Failure;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

public abstract class ServiceAuthorizerHandler extends RestRequestHandler<Void, AuthorizerResponse> {

    public static final String EXECUTE_API_ACTION = "execute-api:Invoke";
    public static final String ALLOW_EFFECT = "Allow";
    public static final String ANY_RESOURCE = "*";
    public static final String WILDCARD = ANY_RESOURCE;
    public static final String PATH_DELIMITER = "/";
    public static final int API_GATEWAY_IDENTIFIER_INDEX = 0;
    public static final int STAGE_INDEX = 1;
    private static final String DENY_EFFECT = "Deny";

    public ServiceAuthorizerHandler(Environment environment) {
        super(Void.class, environment, LoggerFactory.getLogger(ServiceAuthorizerHandler.class));
    }

    @Override
    protected AuthorizerResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        logger.debug("Requesting authorizing: " + principalId());
        secretCheck(requestInfo);
        String resource = formatResource(requestInfo.getMethodArn());

        AuthPolicy authPolicy = createAllowAuthPolicy(resource);

        return createResponse(authPolicy);
    }

    /**
     * This method can be overridden to change the template of the accessed resource. The resource that access will be
     * allowed to. It can contain wildcards.
     *
     * <p>Example methodARN:
     * arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod/GET/service/users/orestis@unit.no Example output:
     * arn:aws:execute-api:eu-west-1:884807050265:2lcqynkwke/Prod\/*\/* <br/> Another possible output is: "*"
     *
     * @param methodArn the method ARN as provided by the API gateway
     * @return a resource for the policy
     */
    protected String formatResource(String methodArn) {
        String[] resource2 = methodArn.split(PATH_DELIMITER);
        String apiGateway = resource2[API_GATEWAY_IDENTIFIER_INDEX];
        String stage = resource2[STAGE_INDEX];
        String method = WILDCARD;
        String functionPath = WILDCARD;
        return String.join(PATH_DELIMITER, apiGateway, stage, method, functionPath);
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
        if (requestInfo.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            String clientSecret = requestInfo.getHeaders().get(HttpHeaders.AUTHORIZATION);
            String correctSecret = attempt(this::fetchSecret)
                .orElseThrow(this::logErrorAndThrowException);
            if (nonNull(clientSecret) && clientSecret.equals(correctSecret)) {
                return;
            }
        }

        throw new ForbiddenException();
    }

    @Override
    protected void writeOutput(Void input, AuthorizerResponse output)
        throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String responseJson = JsonUtils.objectMapper.writeValueAsString(output);
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
        return HttpStatus.SC_OK;
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
