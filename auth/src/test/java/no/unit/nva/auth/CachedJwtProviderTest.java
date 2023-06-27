package no.unit.nva.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CachedJwtProviderTest {

    public static final Instant TOKEN_EXPIRE_AT = Instant.parse("2006-12-03T10:15:30.00Z");
    CachedJwtProvider cachedJwtProvider;
    private Clock mockedClock;

    private DecodedJWT jwt1 = mock(DecodedJWT.class);
    private DecodedJWT jwt2 = mock(DecodedJWT.class);

    @BeforeEach
    void setup() {

        when(jwt1.getExpiresAt()).thenReturn(Date.from(TOKEN_EXPIRE_AT));
        when(jwt2.getExpiresAt()).thenReturn(Date.from(TOKEN_EXPIRE_AT));

        mockedClock = mock(Clock.class);

        var cognitoAuthenticator = mock(CognitoAuthenticator.class);
        when(cognitoAuthenticator.fetchBearerToken())
            .thenReturn(jwt1)
            .thenReturn(jwt2);
        cachedJwtProvider = new CachedJwtProvider(cognitoAuthenticator, mockedClock);
    }

    @Test
    void shouldGetSameTokenOnSequentialCallsWhenTokenIsNotExpired() {
        var dateBeforeTokenExpiration = TOKEN_EXPIRE_AT.minus(Duration.ofMinutes(10));
        when(mockedClock.instant()).thenReturn(dateBeforeTokenExpiration);

        var token1 = cachedJwtProvider.getValue();
        var token2 = cachedJwtProvider.getValue();

        assertEquals(token1, token2);
    }

    @Test
    void shouldGetNewTokenWhenTokenHasExpired() {
        var dateAfterTokenExpiration = TOKEN_EXPIRE_AT.plus(Duration.ofMinutes(10));
        when(mockedClock.instant()).thenReturn(dateAfterTokenExpiration);

        var token1 = cachedJwtProvider.getValue();
        var token2 = cachedJwtProvider.getValue();

        assertNotEquals(token1, token2);
    }

    @Test
    void shouldGetNewTokenWhenTokenIsAboutToExpire() {
        var dateBeforeTokenExpiration = TOKEN_EXPIRE_AT.minus(Duration.ofSeconds(2));
        when(mockedClock.instant()).thenReturn(dateBeforeTokenExpiration);

        var token1 = cachedJwtProvider.getValue();
        var token2 = cachedJwtProvider.getValue();

        assertNotEquals(token1, token2);
    }
}
