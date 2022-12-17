package com.akmal.comperio.http.exception.http;

import com.akmal.comperio.http.HttpStatus;

public class PathNotFoundException extends AbstractHttpRequestException {

  public PathNotFoundException(HttpStatus status, String message, Throwable cause) {
    super(status, message, cause);
  }

  public PathNotFoundException(HttpStatus status, String message) {
    super(status, message);
  }
}
