package com.akmal.http.server;

@FunctionalInterface
public interface ExceptionHandler {

  void handle(Throwable ex, HttpResponse response);
}
