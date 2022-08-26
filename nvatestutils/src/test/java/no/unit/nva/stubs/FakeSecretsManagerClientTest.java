package no.unit.nva.stubs;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.Test;

public class FakeSecretsManagerClientTest {

    @Test
    void shouldBeUsableWithSecretsReader() {
        var secretName = randomString();
        var secretKey1 = randomString();
        var secretValue1 = randomString();
        var secretKey2 = randomString();
        var secretValue2 = randomString();
        var secretsClient =
            new FakeSecretsManagerClient()
                .putSecret(secretName, secretKey1, secretValue1)
                .putSecret(secretName, secretKey2, secretValue2);
        var secretsReader = new SecretsReader(secretsClient);
        assertThat(secretsReader.fetchSecret(secretName, secretKey1), is(equalTo(secretValue1)));
        assertThat(secretsReader.fetchSecret(secretName, secretKey2), is(equalTo(secretValue2)));
    }
}
