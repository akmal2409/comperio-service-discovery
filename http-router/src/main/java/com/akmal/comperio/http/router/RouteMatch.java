package com.akmal.comperio.http.router;

import com.akmal.comperio.http.util.Tuple;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record RouteMatch<H>(
    Route<H> route,
    Map<String, String> variables
) {

  /**
   * Creates an instance of RouteMatch and zips the variables together with
   * arguments in a map.
   * Precondition is that variables must be an ordered list with order matching
   * the variable declaration.
   *
   * @param route matched route.
   * @param variables ordered values of variables.
   */
  public static <H> RouteMatch<H> withVariables(Route<H> route, List<String> variables) {
    final var variableMap = IntStream.range(0, Math.min(route.getVariables().length, variables.size()))
                                .mapToObj(i -> new Tuple<>(route.getVariables()[i], variables.get(i)))
                                .collect(Collectors.toMap(Tuple::first, Tuple::second));

    return new RouteMatch<>(route, variableMap);
  }
}
