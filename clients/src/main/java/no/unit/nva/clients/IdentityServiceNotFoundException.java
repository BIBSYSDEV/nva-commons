package no.unit.nva.clients;

import java.net.URI;

/** Thrown when the identity service responds that the requested resource does not exist. */
public class IdentityServiceNotFoundException extends IdentityServiceException {

  public IdentityServiceNotFoundException(URI requestUri, Throwable cause) {
    super("Identity service found nothing at: " + requestUri, cause);
  }
}
