package nva.commons.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.Tag;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeStsClient;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.utils.Environment;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthorizedApiGatewayHandlerTest {

    public static final String SAMPLE_FEIDE_ID = "sampleFeideId";
    public static final String EMPTY_STRING = "";
    public static final String USERNAME_TAG = "username";
    public static final String SESSION_TAGS_PRIVATE_FIELD = "sessionTags";
    public static final String NECESSARY_ALLOWED_ORIGIN = "*";
    public static final String ROLE_ARN_PRIVATE_FIELD = "roleArn";
    public static final String SAMPLE_ROLE_ARN = "someRoleArn";
    private static final String SAMPLE_CUSTOMER_ID = "sampleCustomerId";
    private static final String SAMPLE_ROLES = "role1, role2";
    private static final String CUSTOMER_TAG = "customer";
    public static final String SESSION_NAME_PRIVATE_FIELD = "roleSessionName";
    private final AWSSecurityTokenService mockStsClient;
    private final Environment environment;
    private final Context context;
    private ByteArrayOutputStream outputStream;

    public AuthorizedApiGatewayHandlerTest() {
        this.environment = setupEnvironment();
        this.mockStsClient = new FakeStsClient();

        context = new FakeContext();
    }

    @BeforeEach
    public void init() {
        this.outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void processInputHasNonNullCredentialsWhenCalled() throws IOException {

        var credentialsBuffer = new AtomicReference<STSAssumeRoleSessionCredentialsProvider>();
        assertThat(credentialsBuffer.get(), is(nullValue()));

        handlerCopyingCredentials(credentialsBuffer).handleRequest(requestWithClaims(), outputStream, context);
        assertThat(credentialsBuffer.get(), is(notNullValue()));
    }

    @Test
    public void processInputHasCredentialsWithSessionTagsWhenCalled()
        throws IOException, NoSuchFieldException, IllegalAccessException {

        STSAssumeRoleSessionCredentialsProvider credentials = extractFilledInCredentialsDuringHandlerExecution();
        List<Tag> actualTags = collectionWithDeepEquals(extractTagsFromCredentials(credentials));
        List<Tag> expectedTags = collectionWithDeepEquals(createTags(SAMPLE_FEIDE_ID, SAMPLE_CUSTOMER_ID));

        assertThat(actualTags, is(equalTo(expectedTags)));
    }

    @Test
    public void processInputReadsAssumedRoleArnFromEnvironment()
        throws IOException, NoSuchFieldException, IllegalAccessException {

        STSAssumeRoleSessionCredentialsProvider credentials = extractFilledInCredentialsDuringHandlerExecution();
        String actualRoleArn = extractRoleArnFromCredentials(credentials);
        assertThat(actualRoleArn, is(equalTo(SAMPLE_ROLE_ARN)));
    }

    @Test
    public void processInputSetsSessionNameBasedOnContextsRequestId()
        throws IOException, NoSuchFieldException, IllegalAccessException {

        STSAssumeRoleSessionCredentialsProvider credentials = extractFilledInCredentialsDuringHandlerExecution();
        String actualRoleArn = extractSessionNameFromCredentials(credentials);
        assertThat(actualRoleArn, is(equalTo(context.getAwsRequestId())));
    }

    private String extractSessionNameFromCredentials(STSAssumeRoleSessionCredentialsProvider credentials)
        throws NoSuchFieldException, IllegalAccessException {
        Field field = credentials.getClass().getDeclaredField(SESSION_NAME_PRIVATE_FIELD);
        field.setAccessible(true);
        return (String) field.get(credentials);
    }

    private STSAssumeRoleSessionCredentialsProvider extractFilledInCredentialsDuringHandlerExecution()
        throws IOException {

        var credentialsBuffer = new AtomicReference<STSAssumeRoleSessionCredentialsProvider>();
        handlerCopyingCredentials(credentialsBuffer).handleRequest(requestWithClaims(), outputStream, context);
        return credentialsBuffer.get();
    }

    private List<Tag> collectionWithDeepEquals(Collection<Tag> extractTagsFromCredentialsProvider) {
        return new ArrayList<>(extractTagsFromCredentialsProvider);
    }

    @SuppressWarnings("unchecked")
    private Collection<Tag> extractTagsFromCredentials(
        STSAssumeRoleSessionCredentialsProvider credentialsProvider)
        throws NoSuchFieldException, IllegalAccessException {

        Field field = credentialsProvider.getClass().getDeclaredField(SESSION_TAGS_PRIVATE_FIELD);
        field.setAccessible(true);
        return (Collection<Tag>) field.get(credentialsProvider);
    }

    private String extractRoleArnFromCredentials(
        STSAssumeRoleSessionCredentialsProvider credentialsProvider)
        throws NoSuchFieldException, IllegalAccessException {

        Field field = credentialsProvider.getClass().getDeclaredField(ROLE_ARN_PRIVATE_FIELD);
        field.setAccessible(true);
        return (String) field.get(credentialsProvider);
    }

    private InputStream requestWithClaims() throws com.fasterxml.jackson.core.JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.objectMapper)
            .withFeideId(SAMPLE_FEIDE_ID)
            .withCustomerId(SAMPLE_CUSTOMER_ID)
            .withRoles(SAMPLE_ROLES)
            .build();
    }

    private TestAuthorizedHandler handlerCopyingCredentials(
        AtomicReference<STSAssumeRoleSessionCredentialsProvider> credentialsBuffer) {
        return new TestAuthorizedHandler(environment, mockStsClient) {
            @Override
            protected String processInput(Void input, RequestInfo requestInfo,
                                          STSAssumeRoleSessionCredentialsProvider credentialsProvider,
                                          Context context) {
                credentialsBuffer.set(credentialsProvider);
                return EMPTY_STRING;
            }

            @Override
            protected List<Tag> sessionTags(RequestInfo requestInfo) {
                String feideId = requestInfo.getFeideId().orElseThrow();
                String customerId = requestInfo.getCustomerId().orElseThrow();
                return createTags(feideId, customerId);
            }
        };
    }

    private List<Tag> createTags(String feideid, String customerId) {
        Tag usernameTag = new Tag().withKey(USERNAME_TAG).withValue(feideid);
        Tag customerTag = new Tag().withKey(CUSTOMER_TAG).withValue(customerId);
        return List.of(usernameTag, customerTag);
    }

    private Environment setupEnvironment() {
        var environment = mock(Environment.class);
        when(environment.readEnv(AuthorizedApiGatewayHandler.ASSUMED_ROLE_ARN_ENV_VAR)).thenReturn(SAMPLE_ROLE_ARN);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(NECESSARY_ALLOWED_ORIGIN);
        return environment;
    }
}