package com.akmal.http.exception;

public class HttpServerLaunchException extends RuntimeException {

  public HttpServerLaunchException(String message) {
    super(message);
  }

  public HttpServerLaunchException(String message, Throwable cause) {
    super(message, cause);
  }

  public HttpServerLaunchException(Throwable cause) {
    super(cause);
  }

  protected HttpServerLaunchException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
