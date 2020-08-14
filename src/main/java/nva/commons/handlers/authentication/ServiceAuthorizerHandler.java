package nva.commons.handlers.authentication;

import static java.util.Objects.nonNull;
import static nva.commons.utils.JsonUtils.objectMapper;
import static nva.commons.utils.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
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

public abstract class ServiceAuthorizerHandler extends RestRequestHandler<Event, AuthorizerResponse> {

    public static final String EXECUTE_API_ACTION = "execute-api:Invoke";
    public static final String ALLOW_EFFECT = "Allow";
    public static final String ANY_RESOURCE = "*";
    private static final String DENY_EFFECT = "Deny";

    public ServiceAuthorizerHandler(Environment environment) {
        super(Event.class, environment, LoggerFactory.getLogger(ServiceAuthorizerHandler.class));
    }

    @Override
    protected AuthorizerResponse processInput(Event input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        logger.info("Service requests access: " + principalId());
        String event = serializeEvent(input);
        logger.info(event);
        String requestInfoStr = getRequestInfoStr(requestInfo);
        logger.info(requestInfoStr);
        secretCheck(requestInfo);

        String methodArn = requestInfo.getMethodArn();
        AuthPolicy authPolicy = createAllowAuthPolicy(methodArn);

        return createResponse(authPolicy);
    }

    private String getRequestInfoStr(RequestInfo requestInfo) {
        try {
            return objectMapper.writeValueAsString(requestInfo);
        } catch (JsonProcessingException e) {
            logger.error("Could not serialize requestInfo");
        }
        return null;
    }

    private String serializeEvent(Event input) {
        String event;
        try {
            event = objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            logger.error("Could not serialize input");
            throw new RuntimeException("Event serializing failed");
        }
        return event;
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
            logger.info("Client secret: " + clientSecret);
            String correctSecret = attempt(this::fetchSecret)
                .orElseThrow(this::logErrorAndThrowException);
            logger.info("Correct secret: " + correctSecret);
            if (nonNull(clientSecret) && clientSecret.equals(correctSecret)) {
                return;
            }
        }

        throw new ForbiddenException();
    }

    @Override
    protected void writeOutput(Event input, AuthorizerResponse output)
        throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String responseJson = JsonUtils.objectMapper.writeValueAsString(output);
            writer.write(responseJson);
        }
    }

    @Override
    protected void writeExpectedFailure(Event input, ApiGatewayException exception, String requestId)
        throws IOException {
        try {
            writeFailure();
        } catch (ForbiddenException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void writeUnexpectedFailure(Event input, Exception exception, String requestId)
        throws IOException {
        try {
            writeFailure();
        } catch (ForbiddenException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Event input, AuthorizerResponse output) {
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
