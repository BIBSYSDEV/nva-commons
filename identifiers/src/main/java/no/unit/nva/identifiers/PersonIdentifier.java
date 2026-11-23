package no.unit.nva.identifiers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.net.URI;
import java.util.regex.Pattern;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UnixPath;

public record PersonIdentifier(@JsonValue String value) implements UriIdentifier<String> {

  private static final UnixPath PERSON_PATH = UnixPath.of("cristin", "person");
  private static final Pattern CRISTIN_SEQUENCE_NUMBER = Pattern.compile("0|[1-9][0-9]{0,18}");
  private static final String INVALID_PERSON_IDENTIFIER_EXCEPTION =
      "Provided uri is not a person identifier: ";
  private static final String INVALID_PERSON_IDENTIFIER_VALUE_EXCEPTION =
      "Person identifier must be a Cristin sequence number!";
  private static final String MISSING_PERSON_IDENTIFIER_EXCEPTION =
      "Person identifier cannot be blank!";

  public PersonIdentifier {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(MISSING_PERSON_IDENTIFIER_EXCEPTION);
    }
    if (isNotCristinSequenceNumber(value)) {
      throw new IllegalArgumentException(INVALID_PERSON_IDENTIFIER_VALUE_EXCEPTION);
    }
  }

  @JsonCreator
  public static PersonIdentifier fromValue(String value) {
    return new PersonIdentifier(value);
  }

  public static PersonIdentifier fromUri(URI uri) {
    return UriIdentifier.identifierFrom(uri, PERSON_PATH)
        .map(PersonIdentifier::new)
        .orElseThrow(() -> new IllegalArgumentException(INVALID_PERSON_IDENTIFIER_EXCEPTION + uri));
  }

  private static boolean isNotCristinSequenceNumber(String value) {
    return !CRISTIN_SEQUENCE_NUMBER.matcher(value).matches();
  }

  @Override
  public UnixPath path() {
    return PERSON_PATH;
  }
}
