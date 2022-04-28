package no.unit.nva.auth;

import java.net.URI;

public class CognitoCredentials {

    private final String cognitoAppClientId;
    private final String cognitoAppClientSecret;
    private final URI cognitoOAuthServerUri;

    public CognitoCredentials(String cognitoAppClientId,
                              String cognitoAppClientSecret,
                              URI cognitoOAuthServerUri) {
        this.cognitoAppClientId = cognitoAppClientId;
        this.cognitoAppClientSecret = cognitoAppClientSecret;
        this.cognitoOAuthServerUri = cognitoOAuthServerUri;
    }

    public URI getCognitoOAuthServerUri() {
        return cognitoOAuthServerUri;
    }

    public String getCognitoAppClientId() {
        return cognitoAppClientId;
    }

    public String getCognitoAppClientSecret() {
        return cognitoAppClientSecret;
    }
}
