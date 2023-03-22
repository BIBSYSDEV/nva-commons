package no.unit.nva.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class BackendClientCredentialsTest {

    @Test
    public void gettersShouldReturnCorrectValues() throws JsonProcessingException {
        var credentials = new BackendClientCredentials("id", "secret");

        assertThat(credentials.getId(), is(equalTo("id")));
        assertThat(credentials.getSecret(), is(equalTo("secret")));
    }

    @Test
    public void toStringShouldCorrectlyRepresentObjectAsJson() throws JsonProcessingException {
        var originalCredentials = new BackendClientCredentials("id", "secret");
        var str = originalCredentials.toString();
        var parsedCredentials = JsonUtils.dtoObjectMapper.readValue(str,
                                                                    BackendClientCredentials.class);

        assertThat(parsedCredentials.getId(), is(equalTo(originalCredentials.getId())));
        assertThat(parsedCredentials.getSecret(), is(equalTo(originalCredentials.getSecret())));
    }

}