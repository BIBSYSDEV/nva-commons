package nva.commons.apigateway;

import static no.unit.nva.auth.CognitoUserInfo.NVA_USERNAME_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.PERSON_CRISTIN_ID_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.PERSON_NIN_CLAIM;
import static no.unit.nva.auth.CognitoUserInfo.TOP_LEVEL_ORG_CRISTIN_ID_CLAIM;
import com.fasterxml.jackson.core.JsonPointer;
import java.net.URI;
import java.util.function.Supplier;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JacocoGenerated
public final class RequestInfoConstants {

    public static final Environment ENVIRONMENT = new Environment();
    public static final Supplier<URI> DEFAULT_COGNITO_HOST = () -> createUri(ENVIRONMENT.readEnv("COGNITO_HOST"));
    public static final String QUERY_STRING_PARAMETERS_FIELD = "queryStringParameters";
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
    public static final String HTTPS = "https"; // Api Gateway only supports HTTPS
    public static final String DOMAIN_NAME_FIELD = "domainName";
    public static final String PERSON_GROUPS_CLAIM = "cognito:groups";
    public static final String AUTHORIZATION_FAILURE_WARNING = "Missing customerId or required access right";
    public static final String BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE = "https://api.nva.unit.no/scopes/backend";
    public static final String SCOPE = "scope";
    private static final String CLAIMS_PATH = "/authorizer/claims/";
    public static final JsonPointer PERSON_GROUPS = claimToJsonPointer(PERSON_GROUPS_CLAIM);
    public static final JsonPointer NVA_USERNAME = claimToJsonPointer(NVA_USERNAME_CLAIM);
    public static final JsonPointer TOP_LEVEL_ORG_CRISTIN_ID = claimToJsonPointer(TOP_LEVEL_ORG_CRISTIN_ID_CLAIM);
    public static final JsonPointer PERSON_CRISTIN_ID = claimToJsonPointer(PERSON_CRISTIN_ID_CLAIM);
    public static final JsonPointer SCOPES_CLAIM = claimToJsonPointer(SCOPE);
    public static final JsonPointer PERSON_NIN = claimToJsonPointer(PERSON_NIN_CLAIM);

    private RequestInfoConstants() {

    }

    private static URI createUri(String cognitoHost) {
        return UriWrapper.fromHost(cognitoHost).getUri();
    }

    private static JsonPointer claimToJsonPointer(String claim) {
        return JsonPointer.compile(CLAIMS_PATH + claim);
    }
}
