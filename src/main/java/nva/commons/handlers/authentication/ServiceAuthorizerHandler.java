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
import nva.commons.utils.JsonUtils;
import nva.commons.utils.attempt.Failure;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

public abstract class ServiceAuthorizerHandler extends RestRequestHandler<Void, AuthorizerResponse> {

    public static final String EXECUTE_API_ACTION = "execute-api:Invoke";
    public static final String ALLOW_EFFECT = "Allow";
    public static final String ANY_RESOURCE = "*";
    private static final String DENY_EFFECT = "Deny";

    public ServiceAuthorizerHandler() {
        super(Void.class, LoggerFactory.getLogger(ServiceAuthorizerHandler.class));
    }

    @Override
    protected AuthorizerResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        secretCheck(requestInfo);

        String methodArn = requestInfo.getMethodArn();
        AuthPolicy authPolicy = createAllowAuthPolicy(methodArn);

        return createResponse(authPolicy);
    }

    protected static AuthPolicy createAllowAuthPolicy(String methodArn) {
        StatementElement statement = StatementElement.newBuilder()
            .withResource(methodArn)
            .withAction(EXECUTE_API_ACTION)
            .withEffect(ALLOW_EFFECT)
            .build();
        return AuthPolicy.newBuilder().withStatement(Collections.singletonList(statement)).build();
    }

    protected static AuthPolicy createDenyAuthPolicy() {
        StatementElement statement = StatementElement.newBuilder()
            .withResource(ANY_RESOURCE)
            .withAction(EXECUTE_API_ACTION)
            .withEffect(DENY_EFFECT)
            .build();
        return AuthPolicy.newBuilder().withStatement(Collections.singletonList(statement)).build();
    }

    protected abstract String principalId();

    protected abstract String fetchSecret();

    protected void secretCheck(RequestInfo requestInfo) throws ForbiddenException {
        if (requestInfo.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            String clientSecret = requestInfo.getHeaders().get(HttpHeaders.AUTHORIZATION);
            String correctSecret = attempt(this::fetchSecret)
                .orElseThrow(this::throwExceptionLoggingTheError);
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
        writeFailure();
    }

    @Override
    protected void writeUnexpectedFailure(Void input, Exception exception, String requestId) throws IOException {
        writeFailure();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, AuthorizerResponse output) {
        return HttpStatus.SC_OK;
    }

    private void writeFailure() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            AuthorizerResponse denyResponse = AuthorizerResponse
                .newBuilder()
                .withPrincipalId(principalId())
                .withPolicyDocument(createDenyAuthPolicy())
                .build();
            String response = objectMapper.writeValueAsString(denyResponse);
            writer.write(response);
        }
    }

    private AuthorizerResponse createResponse(AuthPolicy authPolicy) {
        return AuthorizerResponse.newBuilder()
            .withPrincipalId(principalId())
            .withPolicyDocument(authPolicy)
            .build();
    }

    private RuntimeException throwExceptionLoggingTheError(Failure<String> failure) {
        // error logged by RequestHandler.
        return new RuntimeException(failure.getException());
    }
}
