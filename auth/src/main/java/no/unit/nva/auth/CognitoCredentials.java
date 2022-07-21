package no.unit.nva.auth;

import java.net.URI;
import java.util.function.Supplier;

public class CognitoCredentials {
    
    private final Supplier<String> cognitoAppClientId;
    private final Supplier<String> cognitoAppClientSecret;
    private final URI cognitoOAuthServerUri;
    
    public CognitoCredentials(Supplier<String> cognitoAppClientId,
                              Supplier<String> cognitoAppClientSecret,
                              URI cognitoOAuthServerUri) {
        this.cognitoAppClientId = cognitoAppClientId;
        this.cognitoAppClientSecret = cognitoAppClientSecret;
        this.cognitoOAuthServerUri = cognitoOAuthServerUri;
    }
    
    public URI getCognitoOAuthServerUri() {
        return cognitoOAuthServerUri;
    }

    public String getCognitoAppClientId() {
        return cognitoAppClientId.get();
    }

    public String getCognitoAppClientSecret() {
        return cognitoAppClientSecret.get();
    }
}
