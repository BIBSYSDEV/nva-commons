package no.unit.nva.stubs;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.Test;

public class FakeSecretsManagerClientTest {

    private final static String BAR_VALUE = "bar";

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

    @Test
    void shouldBeUsableWithSecretsReaderUsingPlainTextJson() {
        var plainTextSecretName = randomString();
        var plainTextSecretValue = "{\"bar\":\"" + BAR_VALUE + "\"}";

        var secretsClient =
            new FakeSecretsManagerClient().putPlainTextSecret(plainTextSecretName, plainTextSecretValue);
        var secretsReader = new SecretsReader(secretsClient);

        Foo foo = secretsReader.fetchPlainTextJsonSecret(plainTextSecretName, Foo.class);
        assertThat(foo.getBar(), is(equalTo(BAR_VALUE)));
    }

    private static final class Foo {

        private String bar;

        public String getBar() {
            return bar;
        }

        @JacocoGenerated
        public void setBar(String bar) {
            this.bar = bar;
        }
    }
}
