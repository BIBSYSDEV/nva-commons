package no.unit.nva.stubs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import org.junit.jupiter.api.Test;

class FakeStsClientTest {

    @Test
    public void assumeRoleReturnsNonEmptyCredentials() {
        FakeStsClient fakeStsClient = new FakeStsClient();
        Credentials credentials = fakeStsClient.assumeRole(new AssumeRoleRequest()).getCredentials();
        assertThat(credentials.getAccessKeyId(), is(equalTo(FakeStsClient.SAMPLE_ACCESS_KEY_ID)));
        assertThat(credentials.getSecretAccessKey(), is(equalTo(FakeStsClient.SAMPLE_ACCESS_KEY)));
        assertThat(credentials.getSessionToken(), is(equalTo(FakeStsClient.SAMPLE_SESSION_TOKEN)));
    }
}