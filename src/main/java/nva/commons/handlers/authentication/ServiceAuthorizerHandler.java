package nva.commons.handlers.authentication;

import static java.util.Objects.nonNull;
import static nva.commons.utils.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.exceptions.ForbiddenException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.attempt.Failure;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.slf4j.LoggerFactory;

public abstract class ServiceAuthorizerHandler extends ApiGatewayHandler<Void, RequestAuthorizerResponse> {

    public static final String EXECUTE_API_ACTION = "execute-api:Invoke";
    public static final String ALLOW_EFFECT = "Allow";

    public ServiceAuthorizerHandler() {
        super(Void.class, LoggerFactory.getLogger(ServiceAuthorizerHandler.class));
    }

    @Override
    protected RequestAuthorizerResponse processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        String methodArn = requestInfo.getMethodArn();

        StatementElement statement = StatementElement.newBuilder()
            .withResource(methodArn)
            .withAction(EXECUTE_API_ACTION)
            .withEffect(ALLOW_EFFECT)
            .build();
        AuthPolicy authPolicy = AuthPolicy.newBuilder().withStatement(Collections.singletonList(statement)).build();

        return RequestAuthorizerResponse.newBuilder()
            .withPrincipalId(principalId())
            .withPolicyDocument(authPolicy)
            .build();
    }

    protected abstract String principalId();

    protected abstract String fetchSecret();

    protected void secretCheck(RequestInfo requestInfo) throws Exception {
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
    protected void writeOutput(Void input, RequestAuthorizerResponse output)
        throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            String responseJson = JsonUtils.objectMapper.writeValueAsString(output);
            writer.write(responseJson);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, RequestAuthorizerResponse output) {
        return HttpStatus.SC_ACCEPTED;
    }

    private RuntimeException throwExceptionLoggingTheError(Failure<String> failure) {
        // error logged by RequestHandler.
        return new RuntimeException(failure.getException());
    }
}
