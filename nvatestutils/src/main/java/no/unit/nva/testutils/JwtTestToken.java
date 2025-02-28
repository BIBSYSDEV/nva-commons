package no.unit.nva.testutils;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Provides random JWTs for testing purposes. It has DELIBERATELY limited functionality to avoid misuse.
 */
public final class JwtTestToken {

    private JwtTestToken() {
        // NO-OP
    }

    /**
     * Creates a random valid, current JWT.
     *
     * @return String representation from the JWT.
     */
    public static String randomToken() {
        var issuedAt = new Date();
        var expiresAt = new Date(issuedAt.getTime() + TimeUnit.HOURS.toMillis(1));
        return randomToken(issuedAt, expiresAt);
    }

    /**
     * Creates a random valid, expired JWT.
     *
     * @return String representation from the expired JWT.
     */
    public static String randomExpiredToken() {
        var issuedAt = new Date(new Date().getTime() - TimeUnit.HOURS.toMillis(1));
        var expiresAt = new Date(issuedAt.getTime() + TimeUnit.MINUTES.toMillis(1));
        return randomToken(issuedAt, expiresAt);
    }

    private static String randomToken(Date issuedAt, Date expiresAt) {
        return newToken(randomString(),
                        randomString(),
                        issuedAt,
                        expiresAt,
                        Algorithm.HMAC256(randomString()));
    }

    private static String newToken(String issuer,
                                   String subject,
                                   Date issuedAt,
                                   Date expiresAt,
                                   Algorithm algorithm) {
        return JWT.create()
                   .withIssuer(issuer)
                   .withSubject(subject)
                   .withIssuedAt(issuedAt)
                   .withExpiresAt(expiresAt)
                   .sign(algorithm);
    }
}
