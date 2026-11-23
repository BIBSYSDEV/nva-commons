package no.unit.nva.identifiers;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;

public sealed interface UriIdentifier<T>
    permits PersonIdentifier, OrganizationIdentifier, CustomerIdentifier {

  T value();

  UnixPath path();

  default URI toId(String host) {
    return UriWrapper.fromHost(host).addChild(path()).addChild(value().toString()).getUri();
  }

  static Optional<String> identifierFrom(URI uri, UnixPath expectedPath) {
    var parentPath = expectedPath.addRoot();
    return Optional.ofNullable(uri)
        .filter(UriIdentifier::hasHttpsScheme)
        .filter(UriIdentifier::hasHost)
        .filter(UriIdentifier::hasPathOnly)
        .filter(UriIdentifier::hasUnencodedPath)
        .map(URI::getPath)
        .map(UnixPath::of)
        .filter(path -> isCanonical(path, uri))
        .filter(path -> path.getParent().filter(parentPath::equals).isPresent())
        .map(UnixPath::getLastPathElement);
  }

  private static boolean isCanonical(UnixPath path, URI uri) {
    return path.addRoot().toString().equals(uri.getPath());
  }

  private static boolean hasHttpsScheme(URI uri) {
    return UriWrapper.HTTPS.equalsIgnoreCase(uri.getScheme());
  }

  private static boolean hasHost(URI uri) {
    return Objects.nonNull(uri.getHost());
  }

  private static boolean hasPathOnly(URI uri) {
    return Objects.isNull(uri.getRawQuery()) && Objects.isNull(uri.getRawFragment());
  }

  private static boolean hasUnencodedPath(URI uri) {
    return Objects.equals(uri.getRawPath(), uri.getPath());
  }
}
