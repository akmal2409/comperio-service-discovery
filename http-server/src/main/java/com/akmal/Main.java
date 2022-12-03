package com.akmal;

import com.akmal.http.HttpStatus;
import com.akmal.http.MediaType;
import com.akmal.http.router.HttpMethod;
import com.akmal.http.router.Route;
import com.akmal.http.router.Router;
import com.akmal.http.server.HttpServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {

    HttpServer http = HttpServer.bindToPort(8080)
                          .addRoute(Route.of(HttpMethod.GET, "/hello-world/{username}",
                              (request, response) -> {
                                log.info(request.toString());
                                response.setContentType(MediaType.APPLICATION_JSON);
                                response.setStatus(HttpStatus.OK);

                                try (final var out = new PrintWriter(response.getOutputStream())) {
                                  out.println(String.format(
                                      "{\"name\": \"%s\", \"surname\": \"%s\", \"age\": \"%s\"}",
                                      request.variables().get("username"),
                                      request.queryParams().get("surname"),
                                      request.queryParams().get("age")));
                                  out.flush();
                                }
                              }))
                          .addRoute(Route.of(HttpMethod.POST, "/hello-world/{username}",
                              (request, response) -> {
                                log.info(request.toString());
                                Dto dto = null;


                                if (request.headers().get("Content-Length") != null) {
                                  int bodyLength = Integer.parseInt(request.headers().get("Content-Length"));
                                  byte[] bytes = new byte[bodyLength];


                                  try (BufferedInputStream bufferedInputStream = new BufferedInputStream(request.inputStream())) {
                                    System.out.println(bufferedInputStream.read());
                                    System.out.println(bufferedInputStream.read());
                                    System.out.println(bufferedInputStream.read());
                                    dto = new ObjectMapper().readValue(bytes, Dto.class);
                                  } catch (IOException e) {
                                    e.printStackTrace();
                                  }

                                  System.out.println("READ " + Arrays.toString(bytes));

                                }

                                try (PrintWriter printWriter = new PrintWriter(response.getOutputStream())) {
                                  printWriter.println(new ObjectMapper().writeValueAsString(dto));
                                  printWriter.flush();
                                } catch (JsonProcessingException e) {
                                  throw new RuntimeException(e);
                                }
                              }))
                          .withRouter(Router.defaultRouter())
                          .build();
    Runtime.getRuntime().addShutdownHook(new Thread(http::shutdownNow));

    try (http) {
      http.start();

      Thread.sleep(Long.MAX_VALUE);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
