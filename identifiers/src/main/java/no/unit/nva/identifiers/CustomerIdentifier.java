package no.unit.nva.identifiers;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import nva.commons.core.paths.UnixPath;

public record CustomerIdentifier(@JsonValue UUID value) implements UriIdentifier<UUID> {

  private static final UnixPath CUSTOMER_PATH = UnixPath.of("customer");
  private static final String INVALID_CUSTOMER_IDENTIFIER_EXCEPTION =
      "Provided uri is not a customer identifier";
  private static final String INVALID_CUSTOMER_IDENTIFIER_VALUE_EXCEPTION =
      "Customer identifier must be a canonical uuid!";
  private static final String MISSING_CUSTOMER_IDENTIFIER_EXCEPTION =
      "Customer identifier cannot be null!";

  public CustomerIdentifier {
    if (Objects.isNull(value)) {
      throw new IllegalArgumentException(MISSING_CUSTOMER_IDENTIFIER_EXCEPTION);
    }
  }

  @JsonCreator
  public static CustomerIdentifier fromValue(String value) {
    return new CustomerIdentifier(canonicalUuid(value));
  }

  public static CustomerIdentifier fromUri(URI uri) {
    return UriIdentifier.identifierFrom(uri, CUSTOMER_PATH)
        .map(CustomerIdentifier::fromValue)
        .orElseThrow(() -> new IllegalArgumentException(INVALID_CUSTOMER_IDENTIFIER_EXCEPTION));
  }

  @Override
  public UnixPath path() {
    return CUSTOMER_PATH;
  }

  private static UUID canonicalUuid(String value) {
    return attempt(() -> UUID.fromString(value))
        .toOptional()
        .filter(uuid -> uuid.toString().equals(value))
        .orElseThrow(
            () -> new IllegalArgumentException(INVALID_CUSTOMER_IDENTIFIER_VALUE_EXCEPTION));
  }
}
