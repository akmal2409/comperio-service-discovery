package com.akmal.http;

import java.util.Map;

public record HttpHeaders(
    Map<String, String> headers
) {

  public String get(String header) {
    return this.headers.get(header);
  }
}
