package com.akmal.http;

import com.akmal.http.server.HttpRequest;
import com.akmal.http.server.HttpResponse;

@FunctionalInterface
public interface HttpHandler {

  void handle(HttpRequest request, HttpResponse response);
}
