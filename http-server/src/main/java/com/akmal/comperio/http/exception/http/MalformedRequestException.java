package com.akmal.comperio.http.exception.http;

import com.akmal.comperio.http.HttpStatus;

public class MalformedRequestException extends AbstractHttpRequestException {

  public MalformedRequestException(HttpStatus status, String message, Throwable cause) {
    super(status, message, cause);
  }

  public MalformedRequestException(HttpStatus status, String message) {
    super(status, message);
  }
}
