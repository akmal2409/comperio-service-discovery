package com.akmal.http;

import java.util.Map;

public record QueryParameters(
    Map<String, String> params
) {

  public String get(String key) {
    return params.get(key);
  }
}
