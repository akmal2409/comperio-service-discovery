package com.akmal.comperio.http.registry;

import com.akmal.comperio.http.HttpRouteHandler;
import com.akmal.comperio.http.exception.HttpServerLaunchException;
import com.akmal.comperio.http.exception.HttpServerShutdownException;
import com.akmal.comperio.http.registry.dto.service.ClientRegistrationService;
import com.akmal.comperio.http.registry.handlers.ApplicationInstanceHandlers;
import com.akmal.comperio.http.router.HttpMethod;
import com.akmal.comperio.http.router.Route;
import com.akmal.comperio.http.router.Router;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RegistryHttpServer {

  private final String host;
  private final int port;
  private final HttpHandler rootHandler;

  private final AtomicReference<Undertow> server;
  private final AtomicBoolean started;
  private final AtomicBoolean stopped;

  private final ClientRegistrationService clientRegistrationService;

  RegistryHttpServer(String host, int port, Router<HttpRouteHandler> router,
      ClientRegistrationService clientRegistrationService) {
    this.host = host;
    this.port = port;
    this.clientRegistrationService = clientRegistrationService;
    this.registerRoutes(router);
    this.rootHandler = new RootHttpHandler(router);
    this.server = new AtomicReference<>(configureUndertow());
    this.started = new AtomicBoolean(false);
    this.stopped = new AtomicBoolean(false);
  }

  private void registerRoutes(Router<HttpRouteHandler> router) {
    final var appInstanceHandler = new ApplicationInstanceHandlers(clientRegistrationService);
    router.register(Route.of(HttpMethod.POST, "/v1/applications/{appId}/instances/{instanceId}", appInstanceHandler::handleRegistration));
    router.register(Route.of(HttpMethod.POST, "/v1/applications/{appId}/instances/{instanceId}/heartbeat", appInstanceHandler::handleHeartBeat));
    router.register(Route.of(HttpMethod.DELETE, "/v1/applications/{appId}/instances/{instanceId}", appInstanceHandler::handleDeregistration));
    router.register(Route.of(HttpMethod.GET, "/v1/applications/{appId}/instances", appInstanceHandler::handleQueryApplication));
    router.register(Route.of(HttpMethod.GET, "/v1/applications/{appId}/instances/{instanceId}", appInstanceHandler::handleQueryInstance));
  }

  public void start() {
    if (!this.started.compareAndSet(false, true)) throw new HttpServerLaunchException("Server is already running");
    this.server.get().start();
  }

  public void stop() {
    if (!this.stopped.compareAndSet(false, true)) throw new HttpServerShutdownException("Server shutdown has already been requested");
    this.server.get().stop();
  }

  /**
   * Returns configured but not started instance of Undertow based on the values of the calss
   *
   * @return {@link Undertow}
   */
  private Undertow configureUndertow() {
    return Undertow.builder()
               .addHttpListener(this.port, this.host, this.rootHandler)
               .build();
  }

  public static RegistryHttpServer bindTo(String host, int port, ClientRegistrationService clientRegistrationService) {
    return new RegistryHttpServer(host, port, Router.defaultRouter(), clientRegistrationService);
  }
}
