package no.unit.nva.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import com.auth0.jwt.JWT;
import java.util.Date;
import org.junit.jupiter.api.Test;

class JwtTestTokenTest {

    @Test
    void shouldCreateRandomJwt() {
        var actual = JwtTestToken.randomToken();
        assertThat(JWT.decode(actual).getExpiresAt().after(new Date()), is(true));
    }

    @Test
    void shouldCreateRandomExpiredJwt() {
        var actual = JwtTestToken.randomExpiredToken();
        assertThat(JWT.decode(actual).getExpiresAt().before(new Date()), is(true));
    }
}