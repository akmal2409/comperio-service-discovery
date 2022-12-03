package com.akmal.http.router;

public interface HttpHandler<T> {

  void handleRequest(T exchange);
}
