package com.akmal.http.server;

import com.akmal.http.HttpStatus;
import com.akmal.http.MediaType;
import com.akmal.http.exception.http.AbstractHttpRequestException;
import com.akmal.http.exception.internal.HttpInitialisationFailure;
import com.akmal.http.parser.request.HttpRequestParser;
import com.akmal.http.parser.request.HttpV1RequestParser;
import com.akmal.http.router.Route;
import com.akmal.http.router.Router;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer implements Runnable, AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

  /**
   * Default implementation of the exception handler that returns a simple HTML page with exception
   * message, stacktrace and http status code.
   */
  private static final ExceptionHandler DEFAULT_EXCEPTION_HANDLER = (ex, response) -> {
    if (ex instanceof AbstractHttpRequestException httpEx) {
      response.setStatus(httpEx.getStatus());
    } else {
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    response.setContentType(MediaType.TEXT_HTML);
    final var errorPage = getHtmlErrorResponse(response.getStatus(), String.join("<br/>",
        Arrays.stream(ex.getStackTrace()).sequential().map(StackTraceElement::toString).toList()));

    try (final var out = new PrintWriter(response.getOutputStream())) {
      out.write(errorPage);
      out.flush();
    }
  };

  private final ExecutorService executorService;
  private final Router router;
  private final Collection<Route> routes;
  private final int port;
  private final InetAddress bindAddress;

  private final AtomicReference<Thread> serverThread;

  private final ThreadFactory serverThreadFactory;

  private ServerSocket serverSocket;

  private boolean shutdownRequested;

  private final HttpRequestParser httpRequestParser = new HttpV1RequestParser();


  HttpServer(Builder builder) {
    this.executorService = builder.executorService;
    this.router = builder.router;
    this.routes = builder.routes;
    this.port = builder.port;
    try {
      this.bindAddress = InetAddress.getByName(builder.bindAddress);
    } catch (UnknownHostException e) {
      throw new HttpInitialisationFailure(
          "Failed to bind server to an interface " + builder.bindAddress, e);
    }
    this.serverThread = new AtomicReference<>(null);
    this.serverThreadFactory = builder.serverThreadFactory;
  }

  public void start() {
    final var thread = this.serverThreadFactory.newThread(this);

    if (!serverThread.compareAndSet(null, thread)) {
      throw new IllegalStateException("Server instance is already started");
    }

    serverThread.get().start();
  }

  public synchronized void shutdown() {
    if (this.serverThread.get() == null || shutdownRequested) {
      throw new IllegalStateException("Server is not running");
    }
    this.shutdownRequested = true;

    this.closeSocket();
    this.serverThread.get().interrupt();
    this.executorService.shutdown();
    log.info("Graceful shutdown was requested. Shutting down");
  }

  public synchronized void shutdownNow() {
    if (this.serverThread.get() == null || shutdownRequested) {
      throw new IllegalStateException("Server is not running");
    }
    this.shutdownRequested = true;

    this.closeSocket();
    this.serverThread.get().interrupt();
    this.executorService.shutdownNow();

    log.info(
        "Immediate shutdown requested. Forcefully shutting down executor and close the socket.");
  }


  public boolean isShutdown() {
    return this.executorService.isShutdown();
  }

  ;

  public boolean isTerminated() {
    return this.executorService.isTerminated();
  }

  ;

  private void closeSocket() {
    if (this.serverSocket != null) {
      try {
        serverSocket.close();
      } catch (IOException consumed) {
      }
    }
  }

  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    if (this.serverThread.get() == null || this.executorService.isTerminated()) {
      throw new IllegalStateException("Server is not running");
    }
    return this.executorService.awaitTermination(timeout, unit);
  }

  @Override
  public void run() {
    log.info("Starting HTTP server at port {}. Registered routes: {}", port, this.routes.size());

    try (final var socket = (this.serverSocket = new ServerSocket(this.port, Integer.MAX_VALUE,
        this.bindAddress))) {
      while (!Thread.currentThread().isInterrupted()) {
        final var clientSocket = socket.accept();
        this.executorService.execute(
            new HttpSocketHandler(clientSocket, this.router, this.httpRequestParser,
                DEFAULT_EXCEPTION_HANDLER));
      }
    } catch (IOException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void close() throws Exception {
    serverSocket.close();
    this.serverThread.get().interrupt();
    this.executorService.shutdown();
    log.info("Graceful shutdown was requested. Shutting down");
  }

  public static Builder bindToPort(int port) {
    return new Builder(port);
  }

  public static class Builder {

    private ExecutorService executorService;
    private Router router;
    private Collection<Route> routes;
    private final int port;
    private String bindAddress;
    private final ThreadFactory serverThreadFactory = runnable -> Thread.ofVirtual()
                                                                      .unstarted(runnable);

    private Builder(int port) {
      this.port = port;
      this.executorService = Executors.newVirtualThreadPerTaskExecutor();
      this.router = Router.defaultRouter();
      this.routes = new LinkedList<>();
      this.bindAddress = "0.0.0.0";
    }

    public Builder addRoute(Route route) {
      this.routes.add(route);
      return this;
    }

    public Builder bindTo(String bindAddress) {
      this.bindAddress = bindAddress;
      return this;
    }

    public Builder withExecutor(ExecutorService executorService) {
      this.executorService = executorService;
      return this;
    }

    public Builder withRoutes(Collection<Route> routes) {
      this.routes.addAll(routes);
      return this;
    }

    public Builder withRouter(Router router) {
      this.router = router;
      return this;
    }

    public HttpServer build() {
      for (Route route : this.routes) {
        this.router.register(route);
      }

      return new HttpServer(this);
    }
  }

  private static String getHtmlErrorResponse(HttpStatus status, @Nullable String message) {
    return String.format("""
          <!DOCTYPE html>
            <html>
            
            <head>
              <meta charset="utf-8">
              <title>%s</title>
              <meta name="viewport" content="width=device-width, initial-scale=1">
            </head>
            
            <body>
              <h1>%s - %d</h1>
              <h4>Please contact the maintainer/administrator if the problem persists</h4>
              <p>%s</p>
            </body>
          </html>
        """, "Error " + status.value(), status.getReasonPhrase(), status.value(), message);
  }
}
