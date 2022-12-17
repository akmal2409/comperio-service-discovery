package com.akmal.comperio.http.exception.http;

import com.akmal.comperio.http.HttpStatus;

public abstract class AbstractHttpRequestException extends RuntimeException {

  private final HttpStatus status;


  protected AbstractHttpRequestException(HttpStatus status, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  protected AbstractHttpRequestException(HttpStatus status, String message) {
    this(status, message, null);
  }

  public HttpStatus getStatus() {
    return this.status;
  }
}
