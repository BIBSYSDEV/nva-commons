package no.unit.nva.clients;

import java.net.URI;

/**
 * Base class for the failures reported by {@link IdentityServiceClient}.
 *
 * <p>The message contains the request URI without user info, port, query or fragment, so that
 * credentials or query parameters in a caller-supplied URI do not end up in logs. {@link
 * #getRequestUri()} returns the full URI.
 */
public abstract class IdentityServiceException extends RuntimeException {

  private final URI requestUri;

  protected IdentityServiceException(String messagePrefix, URI requestUri, Throwable cause) {
    super(messagePrefix + withoutUserInfoOrQuery(requestUri), cause);
    this.requestUri = requestUri;
  }

  /**
   * Returns the URI of the identity service request that failed.
   *
   * @return the full request URI
   */
  public URI getRequestUri() {
    return requestUri;
  }

  private static String withoutUserInfoOrQuery(URI uri) {
    return uri.getScheme() + "://" + uri.getHost() + uri.getRawPath();
  }
}
