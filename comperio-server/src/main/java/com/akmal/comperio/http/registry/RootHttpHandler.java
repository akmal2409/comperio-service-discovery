package com.akmal.comperio.http.registry;

import com.akmal.comperio.http.HttpRouteHandler;
import com.akmal.comperio.http.HttpStatus;
import com.akmal.comperio.http.RequestVariables;
import com.akmal.comperio.http.router.HttpMethod;
import com.akmal.comperio.http.router.RouteMatch;
import com.akmal.comperio.http.router.Router;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.util.Optional;

/**
 * Global HttpHandler that redirects to more specific routes by using {@link com.akmal.comperio.http.router.Router}
 */
public class RootHttpHandler implements HttpHandler {
  private final Router<HttpRouteHandler> router;

  public RootHttpHandler(Router<HttpRouteHandler> router) {
    this.router = router;
  }

  @Override
  public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
    final HttpMethod method = HttpMethod.fromString(httpServerExchange.getRequestMethod().toString());

    Optional<RouteMatch<HttpRouteHandler>> matchOptional = this.router.match(method, httpServerExchange.getRequestPath());

    if (matchOptional.isEmpty()) {
      sendStringResponse(httpServerExchange, HttpStatus.NOT_FOUND, "Requested resource not found");
      return;
    }

    final HttpRouteHandler handler = matchOptional.get().route().getHandler();

    handler.handleRequest(httpServerExchange, new RequestVariables(matchOptional.get().variables()));
  }

  private void sendStringResponse(HttpServerExchange exchange, HttpStatus status, String message) {
    exchange.setStatusCode(status.value());
    exchange.getResponseSender().send(message);
  }
}
