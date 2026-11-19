package no.unit.nva.identifiers;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class PersonIdentifierTest {

  private static final String HOST = randomUri().getHost();
  private static final String CRISTIN = "cristin";
  private static final String PERSON = "person";
  private static final String ENCODED_PERSON_ID = "123%2F456";
  private static final int RANDOM_IDENTIFIER_BOUND = 1_000_000_000;

  @Test
  void shouldCreatePersonIdentifierFromCristinPersonUri() {
    var identifier = randomPersonIdentifier();

    var personIdentifier = PersonIdentifier.fromUri(cristinUri(PERSON, identifier));

    assertEquals(identifier, personIdentifier.value());
  }

  @Test
  void shouldThrowExceptionWhenPersonUriHasQueryParameters() {
    var uri = cristinUriWithRawPath(randomPersonIdentifier() + "?depth=top");

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenPersonUriHasFragment() {
    var uri = cristinUriWithRawPath(randomPersonIdentifier() + "#affiliations");

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenPersonUriIdentifierIsEncoded() {
    var uri = cristinUriWithRawPath(ENCODED_PERSON_ID);

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenPersonUriHasEmptyPathSegment() {
    var uri = URI.create("https://%s/cristin//person/%s".formatted(HOST, randomPersonIdentifier()));

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenPersonUriHasTrailingSlash() {
    var uri = cristinUriWithRawPath(randomPersonIdentifier() + "/");

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenPersonUriHasUnexpectedResource() {
    var uri = cristinUri(randomString(), randomString());

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenPersonUriIsNotCristinUri() {
    var uri = UriWrapper.fromHost(HOST).addChild(PERSON).addChild(randomString()).getUri();

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenPersonUriHasNoIdentifier() {
    var uri = UriWrapper.fromHost(HOST).addChild(CRISTIN).addChild(PERSON).getUri();

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenUriIsNull() {
    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(null));
  }

  @Test
  void shouldThrowExceptionWhenPersonIdentifierValueIsMissing() {
    assertThrows(IllegalArgumentException.class, () -> new PersonIdentifier(null));
    assertThrows(IllegalArgumentException.class, () -> new PersonIdentifier(" "));
  }

  @Test
  void shouldThrowExceptionWhenPersonIdentifierValueContainsPathDelimiter() {
    assertThrows(IllegalArgumentException.class, () -> new PersonIdentifier("123/456"));
  }

  @Test
  void shouldThrowExceptionWhenPersonIdentifierValueRequiresEncoding() {
    assertThrows(IllegalArgumentException.class, () -> new PersonIdentifier("123 456"));
  }

  @Test
  void shouldThrowExceptionWhenPersonIdentifierValueIsDotSegment() {
    assertThrows(IllegalArgumentException.class, () -> new PersonIdentifier(".."));
  }

  @Test
  void shouldThrowExceptionWhenPersonIdentifierValueIsNotNumeric() {
    assertThrows(IllegalArgumentException.class, () -> new PersonIdentifier("abc"));
  }

  @Test
  void shouldThrowExceptionWhenPersonIdentifierValueHasLeadingZero() {
    assertThrows(IllegalArgumentException.class, () -> new PersonIdentifier("0123"));
  }

  @Test
  void shouldCreatePersonUriFromIdentifier() {
    var identifier = randomPersonIdentifier();

    var uri = new PersonIdentifier(identifier).toUri(HOST);

    assertEquals(cristinUriWithRawPath(identifier), uri);
  }

  @Test
  void shouldReturnEqualIdentifierWhenPersonUriIsParsedBack() {
    var identifier = new PersonIdentifier(randomPersonIdentifier());

    assertEquals(identifier, PersonIdentifier.fromUri(identifier.toUri(HOST)));
  }

  private String randomPersonIdentifier() {
    return String.valueOf(randomInteger(RANDOM_IDENTIFIER_BOUND));
  }

  private URI cristinUriWithRawPath(String identifier) {
    return URI.create(
        "https://%s/cristin/%s/%s".formatted(HOST, PersonIdentifierTest.PERSON, identifier));
  }

  private URI cristinUri(String resource, String identifier) {
    return UriWrapper.fromHost(HOST)
        .addChild(CRISTIN)
        .addChild(resource)
        .addChild(identifier)
        .getUri();
  }
}
