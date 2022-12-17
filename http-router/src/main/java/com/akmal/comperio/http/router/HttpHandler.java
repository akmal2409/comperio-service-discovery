package com.akmal.comperio.http.router;

public interface HttpHandler<T> {

  void handleRequest(T exchange);
}
