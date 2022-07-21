package no.unit.nva.auth;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CognitoCredentialsTest {
    
    @Test
    void shouldBeAbleToReadTheLatestCredentials() {
        final var clientId = new AtomicReference<>(randomString());
        final var clientSecret = new AtomicReference<>(randomString());
        var credentials = new CognitoCredentials(clientId::get, clientSecret::get, randomUri());
        clientId.set(randomString());
        clientSecret.set(randomString());
        assertThat(credentials.getCognitoAppClientId(), is(equalTo(clientId.get())));
        assertThat(credentials.getCognitoAppClientSecret(), is(equalTo(clientSecret.get())));
    }
}