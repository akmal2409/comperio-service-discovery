package com.akmal.comperio.http.exception.http;

import com.akmal.comperio.http.HttpStatus;

public class UnsupportedMethodException extends AbstractHttpRequestException {

  public UnsupportedMethodException(HttpStatus status, String message, Throwable cause) {
    super(status, message, cause);
  }

  public UnsupportedMethodException(HttpStatus status, String message) {
    super(status, message);
  }
}
