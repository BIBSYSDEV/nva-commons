package no.unit.nva.identifiers;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.UUID;
import java.util.stream.Stream;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CustomerIdentifierTest {

  private static final String HOST = randomUri().getHost();
  private static final String CRISTIN = "cristin";
  private static final String CUSTOMER = "customer";

  @Test
  void shouldCreateCustomerIdentifierFromCustomerUri() {
    var identifier = UUID.randomUUID();

    var customerIdentifier = CustomerIdentifier.fromUri(customerUri(identifier.toString()));

    assertEquals(identifier, customerIdentifier.value());
  }

  @ParameterizedTest
  @MethodSource("identifiersThatAreNotCanonicalUuids")
  void shouldThrowExceptionWhenCustomerUriIdentifierIsNotCanonicalUuid(String identifier) {
    var uri = customerUri(identifier);

    assertThrows(IllegalArgumentException.class, () -> CustomerIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenCustomerUriHasQueryParameters() {
    var uri = customerUriWithRawPath(UUID.randomUUID() + "?depth=top");

    assertThrows(IllegalArgumentException.class, () -> CustomerIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenCustomerUriHasFragment() {
    var uri = customerUriWithRawPath(UUID.randomUUID() + "#affiliations");

    assertThrows(IllegalArgumentException.class, () -> CustomerIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenCustomerUriHasUnexpectedResource() {
    var uri =
        UriWrapper.fromHost(HOST)
            .addChild(CRISTIN)
            .addChild(CUSTOMER)
            .addChild(UUID.randomUUID().toString())
            .getUri();

    assertThrows(IllegalArgumentException.class, () -> CustomerIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenCustomerUriHasNoIdentifier() {
    var uri = UriWrapper.fromHost(HOST).addChild(CUSTOMER).getUri();

    assertThrows(IllegalArgumentException.class, () -> CustomerIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenCustomerIdentifierValueIsMissing() {
    assertThrows(IllegalArgumentException.class, () -> new CustomerIdentifier(null));
  }

  @Test
  void shouldCreateCustomerUriFromIdentifier() {
    var identifier = UUID.randomUUID();

    var uri = new CustomerIdentifier(identifier).toId(HOST);

    assertEquals(customerUriWithRawPath(identifier.toString()), uri);
  }

  private static Stream<String> identifiersThatAreNotCanonicalUuids() {
    return Stream.of(randomString(), "1-2-3-4-5");
  }

  private URI customerUriWithRawPath(String identifier) {
    return URI.create("https://%s/customer/%s".formatted(HOST, identifier));
  }

  @Test
  void shouldSerializeAsBareValue() throws JsonProcessingException {
    var identifier = new CustomerIdentifier(UUID.randomUUID());

    var json = dtoObjectMapper.writeValueAsString(identifier);

    assertEquals("\"" + identifier.value() + "\"", json);
  }

  @Test
  void shouldDeserializeFromBareValue() throws JsonProcessingException {
    var identifier = new CustomerIdentifier(UUID.randomUUID());

    var json = dtoObjectMapper.writeValueAsString(identifier);

    assertEquals(identifier, dtoObjectMapper.readValue(json, CustomerIdentifier.class));
  }

  @Test
  void shouldNotIncludeProvidedUriInExceptionMessage() {
    var token = randomString();
    var uri =
        URI.create(
            "https://%s/customer/%s?access_token=%s".formatted(HOST, UUID.randomUUID(), token));

    var exception =
        assertThrows(IllegalArgumentException.class, () -> CustomerIdentifier.fromUri(uri));

    assertThat(exception.getMessage(), not(containsString(token)));
  }

  private URI customerUri(String identifier) {
    return UriWrapper.fromHost(HOST).addChild(CUSTOMER).addChild(identifier).getUri();
  }
}
