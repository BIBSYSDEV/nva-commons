package no.unit.nva.clients;

import java.net.URI;

/** Base class for the failures reported by {@link IdentityServiceClient}. */
public abstract class IdentityServiceException extends RuntimeException {

  private final URI requestUri;

  protected IdentityServiceException(String messagePrefix, URI requestUri, Throwable cause) {
    super(messagePrefix + requestUri, cause);
    this.requestUri = requestUri;
  }

  /**
   * Returns the URI of the identity service request that failed.
   *
   * @return the request URI
   */
  public URI getRequestUri() {
    return requestUri;
  }
}
