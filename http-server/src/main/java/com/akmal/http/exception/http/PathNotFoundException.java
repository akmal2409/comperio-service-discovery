package com.akmal.http.exception.http;

import com.akmal.http.HttpStatus;

public class PathNotFoundException extends AbstractHttpRequestException {

  public PathNotFoundException(HttpStatus status, String message, Throwable cause) {
    super(status, message, cause);
  }

  public PathNotFoundException(HttpStatus status, String message) {
    super(status, message);
  }
}
