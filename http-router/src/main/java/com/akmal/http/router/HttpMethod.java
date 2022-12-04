package com.akmal.http.router;

public enum HttpMethod {
  GET, POST, PUT, OPTIONS, HEAD, PATCH, DELETE;


  public static HttpMethod fromString(String name) {
    for (HttpMethod method: values()) {
      if (method.toString().equalsIgnoreCase(name)) return method;
    }
    return null;
  }
}
