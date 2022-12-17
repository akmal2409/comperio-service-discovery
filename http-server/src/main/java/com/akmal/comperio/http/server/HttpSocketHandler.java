package com.akmal.comperio.http.server;

import com.akmal.comperio.http.HttpHandler;
import com.akmal.comperio.http.HttpProtocol;
import com.akmal.comperio.http.HttpStatus;
import com.akmal.comperio.http.exception.http.PathNotFoundException;
import com.akmal.comperio.http.parser.request.HttpRequestParser;
import com.akmal.comperio.http.parser.request.ParsedHttpRequestDetails;
import com.akmal.comperio.http.router.RouteMatch;
import com.akmal.comperio.http.router.Router;
import java.net.Socket;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpSocketHandler implements SocketHandler {

  private static final Logger log = LoggerFactory.getLogger(HttpSocketHandler.class);
  private static final int PARSING_TIMEOUT = 3000;
  private final Socket socket;
  private final Router<HttpHandler> router;

  private final HttpRequestParser requestParser;
  private final ExceptionHandler exceptionHandler;

  public HttpSocketHandler(Socket socket, Router<HttpHandler> router, HttpRequestParser requestParser,
      ExceptionHandler exceptionHandler) {
    this.socket = socket;
    this.router = router;
    this.requestParser = requestParser;
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * Handles lifecycle of a single client connection by parsing the http request details, validating them and matching the route.
   * Uses a second try catch block specifically to catch validation exceptions which have to be written back to client.
   * If you remove it and combine with a global try-with-resources, the socket by the time exception reaches catch will be closed.
   */
  @Override
  public void run() {

    try (socket;
          final var in = socket.getInputStream()) {
      socket.setSoTimeout(
          PARSING_TIMEOUT); // set timeout for reading the header, if it exceeds, someone might be just opening TCP connection without any input.

      try {
        ParsedHttpRequestDetails requestDetails = requestParser.parse(in);
        socket.setSoTimeout(0);

        final Optional<RouteMatch<HttpHandler>> routeMatchOpt = this.router.match(requestDetails.method(), requestDetails.path());

        if (routeMatchOpt.isEmpty()) {
          throw new PathNotFoundException(HttpStatus.NOT_FOUND, "Requested path is not found");
        }

        final var routeMatch = routeMatchOpt.get();

        final var request = new HttpRequest(requestDetails.method(), requestDetails.protocol(), requestDetails.path(),
            socket.getInetAddress(), requestDetails.headers(), requestDetails.queryParams(), socket.getInputStream(), routeMatch.variables());
        System.out.println("AV " + socket.getInputStream().available());
        routeMatch.route().getHandler().handle(request, HttpResponse.builder()
                                                            .httpProtocol(HttpProtocol.HTTP_V1_1)
                                                            .outputStream(socket.getOutputStream())
                                                            .status(HttpStatus.OK)
                                                            .build());
      } catch (RuntimeException ex) {
        this.exceptionHandler.handle(ex, HttpResponse.builder()
                                             .httpProtocol(HttpProtocol.HTTP_V1_1)
                                             .outputStream(socket.getOutputStream())
                                             .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                             .build());
      }

    } catch (Exception e) {
      log.error("Socket exception occurred when processing request", e);
    } finally {
      Thread.currentThread().interrupt();
    }
  }
}
