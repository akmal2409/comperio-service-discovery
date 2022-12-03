package com.akmal.http.exception.internal;

public class HttpInitialisationFailure extends RuntimeException {

  public HttpInitialisationFailure(String message) {
    super(message);
  }

  public HttpInitialisationFailure(String message, Throwable cause) {
    super(message, cause);
  }

  public HttpInitialisationFailure(Throwable cause) {
    super(cause);
  }

  protected HttpInitialisationFailure(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
