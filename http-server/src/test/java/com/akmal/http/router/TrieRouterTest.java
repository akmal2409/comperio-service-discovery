package com.akmal.http.router;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;

import com.akmal.http.HttpHandler;
import java.util.Map;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrieRouterTest {

  TrieRouter router;
  HttpHandler handler = (request, response) -> {};

  @BeforeEach
  void setup() {
    this.router = new TrieRouter();
  }

  @Test
  @DisplayName("Should register a route given path")
  void shouldRegisterRouteGivenPath() {
    Route expectedRoute = Route.of(HttpMethod.GET, "/users", handler);

    this.router.register(expectedRoute);

    RouteMatch actualMatch = this.router.match(expectedRoute.getPath()).orElse(null);

    assertThat(actualMatch)
        .isNotNull()
        .extracting(RouteMatch::route)
        .usingRecursiveComparison()
        .isEqualTo(expectedRoute);

    assertThat(actualMatch)
        .isNotNull()
        .extracting(RouteMatch::variables)
        .has(new Condition<>(map -> map.size() == 0, "Variables must be empty"));
  }

  @Test
  @DisplayName("Should return matched variable params")
  void shouldParseVariables() {
    Route expectedRoute = Route.of(HttpMethod.GET, "/users/{userId}/cars/{carId}", handler);
    final var path = "/users/12323/cars/543d";
    final var expectedVariables = Map.of("userId", "12323", "carId", "543d");

    this.router.register(expectedRoute);

    RouteMatch actualMatch = this.router.match(path).orElse(null);

    assertThat(actualMatch)
        .isNotNull()
        .extracting(RouteMatch::variables)
        .usingRecursiveComparison()
        .isEqualTo(expectedVariables);

  }

  @Test
  @DisplayName("Should not parse non-ascii characters in a path")
  void shouldNotParseAndThrowExceptionNonAsciiInAPath() {
    Route expectedRoute = Route.of(HttpMethod.GET, "/users/{userId}/cars/{carId}", handler);
    final var path = "/use" + (char)(128)  + "rs/12323/cars/543d";

    this.router.register(expectedRoute);


    assertThatNoException().isThrownBy(() -> this.router.match(path));
  }

  @Test
  @DisplayName("Should parse non-ascii characters in a template variable")
  void shouldParseNonAsciiInAVariable() {
    final var nonAsciiVariable = "日本人中國的";
    Route expectedRoute = Route.of(HttpMethod.GET, "/users/{userId}", handler);
    final var expectedVars = Map.of("userId", nonAsciiVariable);
    final var path = "/users/" + nonAsciiVariable;

    this.router.register(expectedRoute);

    RouteMatch match = router.match(path).orElse(null);

    assertThat(match)
        .isNotNull()
        .extracting(RouteMatch::variables)
        .usingRecursiveComparison()
        .isEqualTo(expectedVars);
  }

  @Test
  @DisplayName("Should select more specific route and avoid ambiguity")
  void shouldSelectMoreSpecificRoute() {
    Route expectedLessSpecificRoute = Route.of(HttpMethod.GET, "/users/{userId}/test", handler);
    Route expectedSpecificRoute = Route.of(HttpMethod.GET, "/users/specific/test", handler);

    final var path = "/users/specific/test";

    this.router.register(expectedSpecificRoute);
    this.router.register(expectedLessSpecificRoute);

    RouteMatch match = router.match(path).orElse(null);

    assertThat(match)
        .isNotNull()
        .extracting(RouteMatch::route)
        .usingRecursiveComparison()
        .isEqualTo(expectedSpecificRoute);
  }

  @Test
  @DisplayName("Should select less specific route (the one with the variables) when only partial match obtained on a specific route (backtrack)")
  void shouldSelectLessSpecificRouteWhenPartialMatchWithSpecificEncountered() {
    Route expectedLessSpecificRoute = Route.of(HttpMethod.GET, "/users/{userId}/test", handler);
    Route expectedSpecificRoute = Route.of(HttpMethod.GET, "/users/specific/test", handler);

    final var path = "/users/specifi/test";

    this.router.register(expectedSpecificRoute);
    this.router.register(expectedLessSpecificRoute);

    RouteMatch match = router.match(path).orElse(null);

    assertThat(match)
        .isNotNull()
        .extracting(RouteMatch::route)
        .usingRecursiveComparison()
        .isEqualTo(expectedLessSpecificRoute);
  }
}