package com.akmal.comperio.http;

public enum HttpProtocol {
  HTTP_V1("HTTP/1"), HTTP_V1_1("HTTP/1.1"), HTTP_V2("HTTP/2");

  private String name;

  HttpProtocol(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static HttpProtocol fromName(String name) {
    for (HttpProtocol protocol: values()) {
      if (protocol.name.equalsIgnoreCase(name)) return protocol;
    }

    return null;
  }
}
