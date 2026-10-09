package no.unit.nva.clients;

import java.net.URI;

/**
 * Thrown when a request to the identity service fails for any reason other than the requested
 * resource not existing: an error or unexpected status code (4xx other than 404, or 5xx), a network
 * error or timeout, a failure to obtain a backend access token, a response body that cannot be
 * parsed, or a request to a host other than the configured API host.
 */
public class IdentityServiceRequestFailedException extends IdentityServiceException {

  public IdentityServiceRequestFailedException(URI requestUri, Throwable cause) {
    super("Request to identity service failed: ", requestUri, cause);
  }
}
