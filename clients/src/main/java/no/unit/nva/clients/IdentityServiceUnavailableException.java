package no.unit.nva.clients;

import java.net.URI;

/**
 * Thrown when a request to the identity service fails for any reason other than the requested
 * resource not existing, such as an unexpected status code, a network error, a failure to obtain a
 * backend access token, or a response body that cannot be parsed.
 */
public class IdentityServiceUnavailableException extends RuntimeException {

  public IdentityServiceUnavailableException(URI requestUri, Throwable cause) {
    super("Request to identity service failed: " + requestUri, cause);
  }
}
