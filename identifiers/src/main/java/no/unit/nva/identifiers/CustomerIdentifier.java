package no.unit.nva.identifiers;

import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import nva.commons.core.paths.UnixPath;

public record CustomerIdentifier(UUID value) implements UriIdentifier<UUID> {

  private static final UnixPath CUSTOMER_PATH = UnixPath.of("customer");
  private static final String INVALID_CUSTOMER_IDENTIFIER_EXCEPTION =
      "Provided uri is not a customer identifier: ";
  private static final String MISSING_CUSTOMER_IDENTIFIER_EXCEPTION =
      "Customer identifier cannot be null!";

  public CustomerIdentifier {
    if (Objects.isNull(value)) {
      throw new IllegalArgumentException(MISSING_CUSTOMER_IDENTIFIER_EXCEPTION);
    }
  }

  public static CustomerIdentifier fromUri(URI uri) {
    return UriIdentifier.identifierFrom(uri, CUSTOMER_PATH)
        .map(identifier -> uuid(identifier, uri))
        .map(CustomerIdentifier::new)
        .orElseThrow(
            () -> new IllegalArgumentException(INVALID_CUSTOMER_IDENTIFIER_EXCEPTION + uri));
  }

  @Override
  public UnixPath path() {
    return CUSTOMER_PATH;
  }

  private static UUID uuid(String identifier, URI uri) {
    return attempt(() -> UUID.fromString(identifier))
        .toOptional()
        .filter(uuid -> uuid.toString().equals(identifier))
        .orElseThrow(
            () -> new IllegalArgumentException(INVALID_CUSTOMER_IDENTIFIER_EXCEPTION + uri));
  }
}
