package com.akmal.comperio.http;

import io.undertow.server.HttpServerExchange;

public interface HttpRouteHandler {
  void handleRequest(HttpServerExchange exchange, RequestVariables requestVariables) throws Exception;
}
