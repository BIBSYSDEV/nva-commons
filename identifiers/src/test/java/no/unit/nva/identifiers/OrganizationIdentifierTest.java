package no.unit.nva.identifiers;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrganizationIdentifierTest {

  private static final String HOST = randomUri().getHost();
  private static final String CRISTIN = "cristin";
  private static final String PERSON = "person";
  private static final String ORGANIZATION = "organization";
  private static final String ENCODED_ORGANIZATION_ID = "1972.0%2F0.0";
  private static final int RANDOM_IDENTIFIER_BOUND = 1_000_000;

  @Test
  void shouldCreateOrganizationIdentifierFromCristinOrganizationUri() {
    var identifier = randomOrganizationIdentifier();

    var organizationIdentifier =
        OrganizationIdentifier.fromUri(cristinUri(ORGANIZATION, identifier));

    assertEquals(identifier, organizationIdentifier.value());
  }

  @Test
  void shouldThrowExceptionWhenOrganizationUriHasQueryParameters() {
    var uri = cristinUriWithRawPath(randomOrganizationIdentifier() + "?depth=top");

    assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationUriHasFragment() {
    var uri = cristinUriWithRawPath(randomOrganizationIdentifier() + "#affiliations");

    assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationUriIdentifierIsEncoded() {
    var uri = cristinUriWithRawPath(ENCODED_ORGANIZATION_ID);

    assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationUriHasUnexpectedResource() {
    var uri = cristinUri(PERSON, randomString());

    assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationUriIsNotCristinUri() {
    var uri = UriWrapper.fromHost(HOST).addChild(ORGANIZATION).addChild(randomString()).getUri();

    assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationUriHasNoIdentifier() {
    var uri = UriWrapper.fromHost(HOST).addChild(CRISTIN).addChild(ORGANIZATION).getUri();

    assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationIdentifierValueIsMissing() {
    assertThrows(IllegalArgumentException.class, () -> new OrganizationIdentifier(null));
    assertThrows(IllegalArgumentException.class, () -> new OrganizationIdentifier(" "));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "1972.0/0.0",
        "abc",
        "1972.0.0",
        "1972.0.0.0.0",
        "1972.0.0.a",
        "1972",
        "12345678901234567890.0.0.0"
      })
  void shouldThrowExceptionWhenValueIsNotCristinOrganizationNumber(String value) {
    assertThrows(IllegalArgumentException.class, () -> new OrganizationIdentifier(value));
  }

  @Test
  void shouldCreateOrganizationUriFromIdentifier() {
    var identifier = randomOrganizationIdentifier();

    var uri = new OrganizationIdentifier(identifier).toId(HOST);

    assertEquals(cristinUriWithRawPath(identifier), uri);
  }

  private String randomOrganizationIdentifier() {
    return "%d.%d.%d.%d"
        .formatted(
            randomInteger(RANDOM_IDENTIFIER_BOUND),
            randomInteger(RANDOM_IDENTIFIER_BOUND),
            randomInteger(RANDOM_IDENTIFIER_BOUND),
            randomInteger(RANDOM_IDENTIFIER_BOUND));
  }

  private URI cristinUriWithRawPath(String identifier) {
    return URI.create(
        "https://%s/cristin/%s/%s"
            .formatted(HOST, OrganizationIdentifierTest.ORGANIZATION, identifier));
  }

  @Test
  void shouldSerializeAsBareValue() throws JsonProcessingException {
    var identifier = new OrganizationIdentifier(randomOrganizationIdentifier());

    var json = dtoObjectMapper.writeValueAsString(identifier);

    assertEquals("\"" + identifier.value() + "\"", json);
  }

  @Test
  void shouldDeserializeFromBareValue() throws JsonProcessingException {
    var identifier = new OrganizationIdentifier(randomOrganizationIdentifier());

    var json = dtoObjectMapper.writeValueAsString(identifier);

    assertEquals(identifier, dtoObjectMapper.readValue(json, OrganizationIdentifier.class));
  }

  @Test
  void shouldNotIncludeProvidedUriInExceptionMessage() {
    var token = randomString();
    var uri =
        URI.create(
            "https://%s/cristin/organization/%s?access_token=%s"
                .formatted(HOST, randomOrganizationIdentifier(), token));

    var exception =
        assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));

    assertThat(exception.getMessage(), not(containsString(token)));
  }

  private URI cristinUri(String resource, String identifier) {
    return UriWrapper.fromHost(HOST)
        .addChild(CRISTIN)
        .addChild(resource)
        .addChild(identifier)
        .getUri();
  }
}
