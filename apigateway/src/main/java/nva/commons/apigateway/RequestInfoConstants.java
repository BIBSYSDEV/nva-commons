package nva.commons.apigateway;

import static no.unit.nva.auth.CognitoUserInfo.PERSON_AFFILIATION_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.PERSON_NIN_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.TOP_LEVEL_ORG_CRISTIN_ID_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.USER_NAME_CLAIM;
import static no.unit.nva.auth.OAuthConstants.OAUTH_USER_INFO;
import com.fasterxml.jackson.core.JsonPointer;
import java.net.URI;
import java.util.function.Supplier;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JacocoGenerated
public final class RequestInfoConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final Supplier<URI> DEFAULT_COGNITO_URI =
        RequestInfoConstants::lazyInitializationForDefaultCognitoUri;
    public static final Supplier<String> EXTERNAL_USER_POOL_URI =
        RequestInfoConstants::lazyInitializationForDefaultExternalUserPoolUri;
    public static final String IDENTITY_SERVICE_PATH = "users-roles";
    public static final String IDENTITY_SERVICE_USER_INFO_PATH = "userinfo";
    public static final Supplier<URI> E2E_TESTING_USER_INFO_ENDPOINT =
        RequestInfoConstants::lazyInitializationForE2EUserInfoEndpoint;
    public static final String QUERY_STRING_PARAMETERS_FIELD = "queryStringParameters";
    public static final String MULTI_VALUE_QUERY_STRING_PARAMETERS_FIELD = "multiValueQueryStringParameters";
    public static final String PATH_PARAMETERS_FIELD = "pathParameters";
    public static final String PATH_FIELD = "path";
    public static final String HEADERS_FIELD = "headers";
    public static final String METHOD_ARN_FIELD = "methodArn";
    public static final String REQUEST_CONTEXT_FIELD = "requestContext";
    public static final String PROXY_TAG = "proxy";
    public static final String MISSING_FROM_HEADERS = "Missing from headers: ";
    public static final String MISSING_FROM_QUERY_PARAMETERS = "Missing from query parameters: ";
    public static final String MISSING_FROM_PATH_PARAMETERS = "Missing from pathParameters: ";
    public static final String MISSING_FROM_REQUEST_CONTEXT = "Missing from requestContext: ";
    public static final String DOMAIN_NAME_FIELD = "domainName";
    public static final String PERSON_GROUPS_CLAIM = "cognito:groups";
    public static final String CURRENT_CUSTOMER_ID_CLAIM = "custom:customerId";
    public static final String AUTHORIZATION_FAILURE_WARNING = "Missing customerId or required access right";
    public static final String BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE = "https://api.nva.unit.no/scopes/backend";
    public static final String SCOPE = "scope";
    public static final String FEIDE_ID_CLAIM = "custom:feideId";
    public static final String CLIENT_ID_CLAIM = "client_id";
    public static final String ISS_CLAIM = "iss";
    private static final String CLAIMS_PATH = "/authorizer/claims/";
    public static final JsonPointer PERSON_GROUPS = claimToJsonPointer(PERSON_GROUPS_CLAIM);
    public static final JsonPointer USER_NAME = claimToJsonPointer(USER_NAME_CLAIM);
    public static final JsonPointer CURRENT_CUSTOMER_ID = claimToJsonPointer(CURRENT_CUSTOMER_ID_CLAIM);
    public static final JsonPointer TOP_LEVEL_ORG_CRISTIN_ID = claimToJsonPointer(TOP_LEVEL_ORG_CRISTIN_ID_CLAIM);
    public static final JsonPointer PERSON_CRISTIN_ID = claimToJsonPointer(PERSON_CRISTIN_ID_CLAIM);
    public static final JsonPointer PERSON_AFFILIATION = claimToJsonPointer(PERSON_AFFILIATION_CLAIM);
    public static final JsonPointer SCOPES_CLAIM = claimToJsonPointer(SCOPE);
    public static final JsonPointer PERSON_NIN = claimToJsonPointer(PERSON_NIN_CLAIM);
    public static final JsonPointer FEIDE_ID = claimToJsonPointer(FEIDE_ID_CLAIM);
    public static final JsonPointer CLIENT_ID = claimToJsonPointer(CLIENT_ID_CLAIM);
    public static final JsonPointer ISS = claimToJsonPointer(ISS_CLAIM);

    private RequestInfoConstants() {

    }

    private static URI lazyInitializationForE2EUserInfoEndpoint() {
        var apiHost = ENVIRONMENT.readEnv("API_HOST");
        return UriWrapper.fromHost(apiHost)
            .addChild(IDENTITY_SERVICE_PATH)
            .addChild(IDENTITY_SERVICE_USER_INFO_PATH)
            .getUri();
    }

    private static URI lazyInitializationForDefaultCognitoUri() {
        String cognitoHost = ENVIRONMENT.readEnv("COGNITO_HOST");
        return createUri(cognitoHost, OAUTH_USER_INFO);
    }

    private static String lazyInitializationForDefaultExternalUserPoolUri() {
        return ENVIRONMENT.readEnv("EXTERNAL_USER_POOL_URI");
    }

    private static URI createUri(String cognitoHost, String... path) {
        return UriWrapper.fromHost(cognitoHost).addChild(path).getUri();
    }

    private static JsonPointer claimToJsonPointer(String claim) {
        return JsonPointer.compile(CLAIMS_PATH + claim);
    }
}
