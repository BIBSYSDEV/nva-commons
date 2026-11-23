package no.unit.nva.identifiers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.regex.Pattern;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UnixPath;

public record OrganizationIdentifier(@JsonValue String value) implements UriIdentifier<String> {

  private static final UnixPath ORGANIZATION_PATH = UnixPath.of("cristin", "organization");
  private static final Pattern CRISTIN_ORGANIZATION_NUMBER =
      Pattern.compile("(0|[1-9][0-9]{0,18})(\\.(0|[1-9][0-9]{0,18})){3}");
  private static final String INVALID_ORGANIZATION_IDENTIFIER_EXCEPTION =
      "Provided uri is not an organization identifier: ";
  private static final String INVALID_ORGANIZATION_IDENTIFIER_VALUE_EXCEPTION =
      "Organization identifier must be four dot separated numbers!";
  private static final String MISSING_ORGANIZATION_IDENTIFIER_EXCEPTION =
      "Organization identifier cannot be blank!";

  public OrganizationIdentifier {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(MISSING_ORGANIZATION_IDENTIFIER_EXCEPTION);
    }
    if (!isCristinOrganizationNumber(value)) {
      throw new IllegalArgumentException(INVALID_ORGANIZATION_IDENTIFIER_VALUE_EXCEPTION);
    }
  }

  @JsonCreator
  public static OrganizationIdentifier fromValue(String value) {
    return new OrganizationIdentifier(value);
  }

  public static OrganizationIdentifier fromUri(URI uri) {
    return UriIdentifier.identifierFrom(uri, ORGANIZATION_PATH)
        .map(OrganizationIdentifier::new)
        .orElseThrow(
            () -> new IllegalArgumentException(INVALID_ORGANIZATION_IDENTIFIER_EXCEPTION + uri));
  }

  private static boolean isCristinOrganizationNumber(String value) {
    return CRISTIN_ORGANIZATION_NUMBER.matcher(value).matches();
  }

  @Override
  public UnixPath path() {
    return ORGANIZATION_PATH;
  }
}
