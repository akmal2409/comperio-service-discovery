package com.akmal.comperio.http.exception;

public class HttpServerShutdownException extends RuntimeException {

  public HttpServerShutdownException(String message) {
    super(message);
  }

  public HttpServerShutdownException(String message, Throwable cause) {
    super(message, cause);
  }

  public HttpServerShutdownException(Throwable cause) {
    super(cause);
  }

  protected HttpServerShutdownException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
