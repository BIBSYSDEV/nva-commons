package no.unit.commons.apigateway.authentication;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * A simple Lambda Authorizer. The class can be used by extending it and adding in the default constructor the principal
 * id value.
 */
public class DefaultRequestAuthorizer extends RequestAuthorizer {

    private static final Environment ENVIRONMENT = new Environment();
    public static final String API_KEY_SECRET_NAME = ENVIRONMENT.readEnv("API_KEY_SECRET_NAME");
    public static final String API_KEY_SECRET_KEY = ENVIRONMENT.readEnv("API_KEY_SECRET_KEY");
    private final SecretsReader secretsReader;
    private final String principalIdentifier;

    /**
     * The constructor that should be inherited by the default constructor of the custom Lambda Authorizer.
     *
     * @param principalId A string describing the service tha is using the Authorizer.
     */
    @JacocoGenerated
    public DefaultRequestAuthorizer(String principalId) {
        this(SecretsReader.defaultSecretsManagerClient(), principalId);
    }

    public DefaultRequestAuthorizer(SecretsManagerClient secretsClient,
                                    String principalId) {
        super();
        this.secretsReader = new SecretsReader(secretsClient);
        this.principalIdentifier = principalId;
    }

    @Override
    protected String principalId() {
        return principalIdentifier;
    }

    @Override
    protected String fetchSecret() {
        return secretsReader.fetchSecret(API_KEY_SECRET_NAME, API_KEY_SECRET_KEY);
    }
}
