package com.akmal.comperio.http;

import com.akmal.comperio.http.server.HttpRequest;
import com.akmal.comperio.http.server.HttpResponse;

@FunctionalInterface
public interface HttpHandler {

  void handle(HttpRequest request, HttpResponse response);
}
