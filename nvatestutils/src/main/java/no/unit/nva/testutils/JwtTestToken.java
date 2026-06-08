package no.unit.nva.testutils;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Provides random JWTs for testing purposes. It has DELIBERATELY limited functionality to avoid
 * misuse.
 */
public final class JwtTestToken {

  private JwtTestToken() {
    // NO-OP
  }

  /**
   * Creates a random valid, current JWT.
   *
   * @return String representation of the JWT.
   */
  public static String randomToken() {
    var issuedAt = Instant.now();
    var expiresAt = issuedAt.plus(1, ChronoUnit.HOURS);
    return randomToken(issuedAt, expiresAt);
  }

  /**
   * Creates a random valid, expired JWT.
   *
   * @return String representation of the expired JWT.
   */
  public static String randomExpiredToken() {
    var issuedAt = Instant.now().minus(1, ChronoUnit.HOURS);
    var expiresAt = issuedAt.plus(1, ChronoUnit.MINUTES);
    return randomToken(issuedAt, expiresAt);
  }

  private static String randomToken(Instant issuedAt, Instant expiresAt) {
    return newToken(
        randomString(), randomString(), issuedAt, expiresAt, Algorithm.HMAC256(randomString()));
  }

  private static String newToken(
      String issuer, String subject, Instant issuedAt, Instant expiresAt, Algorithm algorithm) {
    return JWT.create()
        .withIssuer(issuer)
        .withSubject(subject)
        .withIssuedAt(issuedAt)
        .withExpiresAt(expiresAt)
        .sign(algorithm);
  }
}
