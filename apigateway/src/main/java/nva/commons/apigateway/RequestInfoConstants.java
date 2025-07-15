package nva.commons.apigateway;

import com.fasterxml.jackson.core.JsonPointer;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public final class RequestInfoConstants {

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
    public static final String AUTHORIZATION_FAILURE_WARNING = "Missing customerId or required access right";
    public static final String BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE = "https://api.nva.unit.no/scopes/backend";
    public static final String SCOPE = "scope";
    public static final String CLIENT_ID_CLAIM = "client_id";
    public static final String CLAIMS_PATH = "/authorizer/claims";

    public static final JsonPointer SCOPES_CLAIM = claimToJsonPointer(SCOPE);
    public static final JsonPointer CLIENT_ID = claimToJsonPointer(CLIENT_ID_CLAIM);

    private RequestInfoConstants() {

    }


    private static JsonPointer claimToJsonPointer(String claim) {
        return JsonPointer.compile(CLAIMS_PATH + "/" + claim);
    }
}
