package nva.commons.apigateway;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.exceptions.ExceptionUtils.stackTraceInSingleLine;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.GatewayResponseSerializingException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.Environment;
import nva.commons.core.attempt.Failure;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template class for implementing Lambda function handlers that get activated through a call to ApiGateway. This class
 * is for processing a HTTP query directly without the usage of a Jersey-server or a SpringBoot template.
 *
 * @param <I> Class of the object in the body field of the ApiGateway message.
 * @param <O> Class of the response object.
 * @see <a href="https://github.com/awslabs/aws-serverless-java-container">aws-serverless-container</a> for
 *     alternative solutions.
 */
public abstract class RestRequestHandler<I, O> implements RequestStreamHandler {

    public static final String REQUEST_ID = "RequestId:";
    public static final String SPACE = " ";
    public static final String EMPTY_STRING = "";
    public static final String COMMA = ",";
    public static final String PREFIX_SINGLE_WILDCARD_TYPE = "*/";
    private static final String COGNITO_AUTHORIZER_URLS = "COGNITO_AUTHORIZER_URLS";
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int READ_TIMEOUT = 2000;
    protected final Environment environment;
    private static final Logger logger = LoggerFactory.getLogger(RestRequestHandler.class);
    private final transient Class<I> iclass;
    private final transient ApiMessageParser<I> inputParser;
    protected final ObjectMapper objectMapper;
    private final String[] authorizerUrls;
    private final Map<String, JwkProvider> jwkProviders;

    protected transient OutputStream outputStream;
    protected transient String allowedOrigin;

    private static final List<MediaType> DEFAULT_SUPPORTED_MEDIA_TYPES = List.of(JSON_UTF_8);
    private static final String WILDCARD_TYPE = "*";
    public static final String ALLOW_ALL_ORIGINS = "*";

    /**
     * Calculates the Content MediaType of the response based on the supported Media Types and the requested Media
     * Types.
     *
     * @param requestInfo The request as sent by ApiGateway
     * @return the MediaType value of the Response. Basically the value of the Content header.
     * @throws UnsupportedAcceptHeaderException when no provided Accept header media types are supported in this
     *                                          handler.
     */
    protected MediaType calculateContentTypeHeaderReturnValue(RequestInfo requestInfo)
        throws UnsupportedAcceptHeaderException {
        if (requestInfo.getHeaders().containsKey(HttpHeaders.ACCEPT)) {
            String acceptHeader = requestInfo.getHeaderOptional(HttpHeaders.ACCEPT).orElseThrow();
            return bestMatchingMediaTypeBasedOnRequestAcceptHeader(acceptHeader);
        }
        return defaultResponseContentTypeWhenNotSpecifiedByClientRequest();
    }

    private MediaType bestMatchingMediaTypeBasedOnRequestAcceptHeader(String acceptHeader)
        throws UnsupportedAcceptHeaderException {
        List<MediaType> acceptMediaTypes = parseAcceptHeader(acceptHeader);

        List<MediaType> matches = findMediaTypeMatches(acceptMediaTypes);
        if (matches.isEmpty()) {
            throw new UnsupportedAcceptHeaderException(acceptMediaTypes, listSupportedMediaTypes());
        }
        return matches.get(0);
    }

    private List<MediaType> parseAcceptHeader(String header) {
        return Arrays.stream(header.replace(SPACE, EMPTY_STRING).split(COMMA))
                   .map(this::sanitizeMimeType)
                   .map(MediaType::parse)
                   .collect(Collectors.toList());
    }

    private String sanitizeMimeType(String mimeType) {
        int index = mimeType.indexOf(';');
        String fullType = (index >= 0 ? mimeType.substring(0, index) : mimeType).trim();
        if (!fullType.isEmpty() && WILDCARD_TYPE.equals(fullType)) {
            return PREFIX_SINGLE_WILDCARD_TYPE + mimeType;
        }
        return mimeType;
    }

    protected List<MediaType> findMediaTypeMatches(List<MediaType> acceptMediaTypes) {
        return listSupportedMediaTypes().stream()
                   .filter(mediaType -> inAcceptedMediaTypeRange(mediaType, acceptMediaTypes))
                   .collect(Collectors.toList());
    }

    private boolean inAcceptedMediaTypeRange(MediaType mediaType, List<MediaType> acceptMediaTypes) {
        return acceptMediaTypes.stream()
                   .map(MediaType::withoutParameters)
                   .anyMatch(mediaType::is);
    }

    /**
     * Override this method to change the supported media types for the handler.
     *
     * @return a list of supported media types
     */
    protected List<MediaType> listSupportedMediaTypes() {
        return DEFAULT_SUPPORTED_MEDIA_TYPES;
    }

    protected MediaType getDefaultResponseContentTypeHeaderValue(RequestInfo requestInfo)
        throws UnsupportedAcceptHeaderException {
        return calculateContentTypeHeaderReturnValue(requestInfo);
    }

    private MediaType defaultResponseContentTypeWhenNotSpecifiedByClientRequest() {
        return listSupportedMediaTypes().get(0);
    }

    /**
     * The input class should be set explicitly by the inheriting class.
     *
     * @param iclass      The class object of the input class.
     * @param environment the Environment from where the handler will read ENV variables.
     */
    public RestRequestHandler(Class<I> iclass, Environment environment, ObjectMapper objectMapper) {
        this.iclass = iclass;
        this.environment = environment;
        this.inputParser = new ApiMessageParser<>(objectMapper);
        this.objectMapper = objectMapper;
        this.authorizerUrls = this.environment.readEnv(COGNITO_AUTHORIZER_URLS).split(",");
        this.jwkProviders = getJwkProviders(authorizerUrls);
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        logger.info(REQUEST_ID + context.getAwsRequestId());
        I inputObject = null;
        try {
            init(outputStream, context);
            String inputString = IoUtils.streamToString(inputStream);
            inputObject = attempt(() -> parseInput(inputString))
                              .orElseThrow(this::parsingExceptionToBadRequestException);

            RequestInfo requestInfo = RequestInfo.fromString(inputString);

            validateAuthorization(requestInfo);

            setAllowedOrigin(requestInfo);

            validateRequest(inputObject, requestInfo, context);

            O response = processInput(inputObject, requestInfo, context);

            writeOutput(inputObject, response, requestInfo);
        } catch (ApiGatewayException e) {
            handleExpectedException(context, inputObject, e);
        } catch (Exception e) {
            handleUnexpectedException(context, inputObject, e);
        }
    }

    private Map<String, JwkProvider> getJwkProviders(String... authorizerUrls) {
        return Stream.of(authorizerUrls)
                   .collect(Collectors.toMap(
                       domain -> domain,
                       domain -> new JwkProviderBuilder(domain)
                                     .cached(10, 1, TimeUnit.HOURS)
                                     .rateLimited(10, 1, TimeUnit.MINUTES)
                                     .timeouts(CONNECT_TIMEOUT, READ_TIMEOUT)
                                     .build()
                   ));
    }

    private void validateAuthorization(RequestInfo requestInfo) throws UnauthorizedException {
        var bearerToken = requestInfo.getBearerToken();
        if (bearerToken.isPresent() && !requestInfo.isGatewayAuthorized()) {
            var decodedJWT = JWT.decode(bearerToken.get());
            var jwkProvider = getJwkProvider(decodedJWT.getIssuer());

            try {
                var jwk = jwkProvider.get(decodedJWT.getKeyId());
                var algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey());
                var jwtVerifier = JWT.require(algorithm)
                                      .withIssuer(authorizerUrls)
                                      .build();

                jwtVerifier.verify(decodedJWT);
            }
            catch (JwkException | JWTVerificationException e) {
                logger.error("Failed to verify token", e);
                throw new UnauthorizedException("Failed to verify token");
            }
        }
    }

    private JwkProvider getJwkProvider(String issuer) throws UnauthorizedException {
        var jwkProvider = jwkProviders.get(issuer);
        if (jwkProvider == null) {
            throw new UnauthorizedException("No JWK provider found for issuer: " + issuer);
        }
        return jwkProvider;
    }

    protected abstract void setAllowedOrigin(RequestInfo requestInfo);

    /**
     * Implements input validation and access control. {@link RestRequestHandler#handleExpectedException} method.
     *
     * @param input       The input object to the method. Usually a deserialized json.
     * @param requestInfo Request headers and path.
     * @param context     the ApiGateway context.
     * @throws ApiGatewayException all exceptions are caught by writeFailure and mapped to error codes through the
     *                             method {@link RestRequestHandler#getFailureStatusCode}
     */
    protected abstract void validateRequest(I input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException;

    protected ApiGatewayException parsingExceptionToBadRequestException(Failure<I> fail) {
        return new BadRequestException(fail.getException().getMessage(), fail.getException());
    }

    protected void handleUnexpectedException(Context context, I inputObject, Exception e)
        throws IOException {
        logger.error(e.getMessage());
        logger.error(stackTraceInSingleLine(e));
        writeUnexpectedFailure(inputObject, e, context.getAwsRequestId());
    }

    protected void handleExpectedException(Context context, I inputObject, ApiGatewayException e)
        throws IOException {
        logger.warn(e.getMessage());
        logger.warn(stackTraceInSingleLine(e));
        writeExpectedFailure(inputObject, e, context.getAwsRequestId());
    }

    protected void init(OutputStream outputStream, Context context) {
        this.outputStream = outputStream;
        this.allowedOrigin = ALLOW_ALL_ORIGINS; //anti-pattern: this will be overwritten later on, but in the event
        // that we get an exception before we get there, there is this.
    }

    /**
     * Implements the main logic of the handler. Any exception thrown by this method will be handled by
     * {@link RestRequestHandler#handleExpectedException} method.
     *
     * @param input       The input object to the method. Usually a deserialized json.
     * @param requestInfo Request headers and path.
     * @param context     the ApiGateway context.
     * @return the Response body that is going to be serialized in json
     * @throws ApiGatewayException all exceptions are caught by writeFailure and mapped to error codes through the
     *                             method {@link RestRequestHandler#getFailureStatusCode}
     */
    protected abstract O processInput(I input, RequestInfo requestInfo, Context context) throws ApiGatewayException;

    /**
     * Define the response statusCode in case of failure.
     *
     * @param input The request input.
     * @param error The exception that caused the failure.
     * @return the failure status code.
     */
    protected int getFailureStatusCode(I input, ApiGatewayException error) {
        return error.getStatusCode();
    }

    /**
     * Method for parsing the input object from the ApiGateway message.
     *
     * @param inputString the ApiGateway message.
     * @return an object of class I.
     * @throws IOException when parsing fails.
     */
    protected I parseInput(String inputString) throws IOException {
        return inputParser.getBodyElementFromJson(inputString, getIClass());
    }

    /**
     * Define the success status code.
     *
     * @param input  The request input.
     * @param output The response output
     * @return the success status code.
     */
    protected abstract Integer getSuccessStatusCode(I input, O output);

    protected abstract void writeOutput(I input, O output, RequestInfo requestInfo)
        throws IOException, GatewayResponseSerializingException, UnsupportedAcceptHeaderException;

    protected abstract void writeExpectedFailure(I input, ApiGatewayException exception, String requestId)
        throws IOException;

    protected abstract void writeUnexpectedFailure(I input, Exception exception, String requestId) throws IOException;

    private Class<I> getIClass() {
        return iclass;
    }
}

