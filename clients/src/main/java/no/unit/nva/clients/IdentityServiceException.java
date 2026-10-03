package no.unit.nva.clients;

/** Base class for the failures reported by {@link IdentityServiceClient}. */
public abstract class IdentityServiceException extends RuntimeException {

  protected IdentityServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
