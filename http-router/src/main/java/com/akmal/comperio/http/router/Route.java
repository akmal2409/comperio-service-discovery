package com.akmal.comperio.http.router;

import com.akmal.comperio.http.util.Tuple;
import java.util.ArrayList;
import org.jetbrains.annotations.VisibleForTesting;


public class Route<H> {
  static final char VARIABLE_PLACEHOLDER = '$';
  private final HttpMethod method;
  private final String path;
  private final H handler;
  private final String[] variables;
  private final String pathWithWildcards;


  Route(HttpMethod method, String path, H handler) {
    this.method = method;

    boolean nonAscii = path.codePoints().anyMatch(c -> c > 127);
    if (nonAscii) throw new IllegalArgumentException("Only ASCII characters are allowed");

    this.path = path.charAt(path.length() - 1) != '/' ? path + '/' : path;
    this.handler = handler;
    final var replacedPathAndVars = this.parseVariables(this.path);
    this.variables = replacedPathAndVars.second();
    this.pathWithWildcards = replacedPathAndVars.first();
  }

  public static <H> Route<H> of(HttpMethod method, String path, H handler) {
    return new Route<>(method, path, handler);
  }

  /**
   * Method parses route, replaces the url variables with wildcards.
   * Precondition path must end with a slash.
   */
  @VisibleForTesting
  private Tuple<String, String[]> parseVariables(String path) {
    final var variables = new ArrayList<String>(3);
    final var sb = new StringBuilder(path.length());

    int i = 0;

    while (i < path.length()) {
      if (path.charAt(i) == '}') throw new IllegalArgumentException("Corrupted path variable. Path given: " + path);
      else if (path.charAt(i) == '{') {
        // start of a template variable
        final var varBuilder = new StringBuilder(3);
        i++;
        if (path.charAt(i) == '}') throw new IllegalArgumentException("Path variable must not be empty. Path given: " + path);

        while (i < path.length() - 1) {
          if (i == path.length() - 2 && path.charAt(i) != '}') throw new IllegalArgumentException("Corrupted path variable. Path given: " + path);
          else if (path.charAt(i) == '}') break;
          varBuilder.append(path.charAt(i++));
        }
        variables.add(varBuilder.toString());
        sb.append(VARIABLE_PLACEHOLDER);
      } else {
        sb.append(path.charAt(i));
      }
      i++;
    }

    return new Tuple<>(sb.toString(), variables.toArray(new String[0]));
  }

  public HttpMethod getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public H getHandler() {
    return handler;
  }

  /**
   * Returns ordered collection of variables that were present in
   * the route definition.
   */
  public String[] getVariables() {
    return variables;
  }


  String getPathWithWildcards() {
    return pathWithWildcards;
  }
}
