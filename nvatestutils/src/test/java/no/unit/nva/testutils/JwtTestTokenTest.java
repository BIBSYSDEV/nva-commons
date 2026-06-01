package no.unit.nva.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import com.auth0.jwt.JWT;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JwtTestTokenTest {

    @Test
    void shouldCreateRandomJwt() {
        var actual = JwtTestToken.randomToken();
        assertThat(JWT.decode(actual).getExpiresAtAsInstant().isAfter(Instant.now()), is(true));
    }

    @Test
    void shouldCreateRandomExpiredJwt() {
        var actual = JwtTestToken.randomExpiredToken();
        assertThat(JWT.decode(actual).getExpiresAtAsInstant().isBefore(Instant.now()), is(true));
    }
}