package com.akmal.comperio.http.registry.exception;


public class ClientRegistrationFailureException extends RuntimeException {

  public ClientRegistrationFailureException(String message) {
    super(message);
  }

  public ClientRegistrationFailureException(String message, Throwable cause) {
    super(message, cause);
  }

  public ClientRegistrationFailureException(Throwable cause) {
    super(cause);
  }

  protected ClientRegistrationFailureException(String message, Throwable cause,
      boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
