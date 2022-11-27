package com.akmal.http;

@FunctionalInterface
public interface HttpHandler {

  void handle(HttpRequest request, HttpResponse response);
}
