package com.akmal.comperio.http.router;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Map;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrieRouterTest {

  TrieRouter<NoopRequestHandler> router;
  NoopRequestHandler handler = new NoopRequestHandler();

  @BeforeEach
  void setup() {
    this.router = new TrieRouter<NoopRequestHandler>();
  }

  @Test
  @DisplayName("Should register a route given path")
  void shouldRegisterRouteGivenPath() {
    Route<NoopRequestHandler> expectedRoute = Route.of(HttpMethod.GET, "/users", handler);

    this.router.register(expectedRoute);

    RouteMatch actualMatch = this.router.match(HttpMethod.GET, expectedRoute.getPath()).orElse(null);

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
    Route<NoopRequestHandler> expectedRoute = Route.of(HttpMethod.GET, "/users/{userId}/cars/{carId}", handler);
    final var path = "/users/12323/cars/543d";
    final var expectedVariables = Map.of("userId", "12323", "carId", "543d");

    this.router.register(expectedRoute);

    RouteMatch actualMatch = this.router.match(HttpMethod.GET, path).orElse(null);

    assertThat(actualMatch)
        .isNotNull()
        .extracting(RouteMatch::variables)
        .usingRecursiveComparison()
        .isEqualTo(expectedVariables);

  }

  @Test
  @DisplayName("Should not parse non-ascii characters in a path")
  void shouldNotParseAndThrowExceptionNonAsciiInAPath() {
    Route<NoopRequestHandler> expectedRoute = Route.of(HttpMethod.GET, "/users/{userId}/cars/{carId}", handler);
    final var path = "/use" + (char)(128)  + "rs/12323/cars/543d";

    this.router.register(expectedRoute);


    AssertionsForClassTypes.assertThatNoException().isThrownBy(() -> this.router.match(HttpMethod.GET, path));
  }

  @Test
  @DisplayName("Should parse non-ascii characters in a template variable")
  void shouldParseNonAsciiInAVariable() {
    final var nonAsciiVariable = "日本人中國的";
    Route<NoopRequestHandler> expectedRoute = Route.of(HttpMethod.GET, "/users/{userId}", handler);
    final var expectedVars = Map.of("userId", nonAsciiVariable);
    final var path = "/users/" + nonAsciiVariable;

    this.router.register(expectedRoute);

    RouteMatch match = router.match(HttpMethod.GET, path).orElse(null);

    assertThat(match)
        .isNotNull()
        .extracting(RouteMatch::variables)
        .usingRecursiveComparison()
        .isEqualTo(expectedVars);
  }

  @Test
  @DisplayName("Should select more specific route and avoid ambiguity")
  void shouldSelectMoreSpecificRoute() {
    Route<NoopRequestHandler> expectedLessSpecificRoute = Route.of(HttpMethod.GET, "/users/{userId}/test", handler);
    Route<NoopRequestHandler> expectedSpecificRoute = Route.of(HttpMethod.GET, "/users/specific/test", handler);

    final var path = "/users/specific/test";

    this.router.register(expectedSpecificRoute);
    this.router.register(expectedLessSpecificRoute);

    RouteMatch match = router.match(HttpMethod.GET, path).orElse(null);

    assertThat(match)
        .isNotNull()
        .extracting(RouteMatch::route)
        .usingRecursiveComparison()
        .isEqualTo(expectedSpecificRoute);
  }

  @Test
  @DisplayName("Should select less specific route (the one with the variables) when only partial match obtained on a specific route (backtrack)")
  void shouldSelectLessSpecificRouteWhenPartialMatchWithSpecificEncountered() {
    Route<NoopRequestHandler> expectedLessSpecificRoute = Route.of(HttpMethod.GET, "/users/{userId}/test", handler);
    Route<NoopRequestHandler> expectedSpecificRoute = Route.of(HttpMethod.GET, "/users/specific/test", handler);

    final var path = "/users/specifi/test";

    this.router.register(expectedSpecificRoute);
    this.router.register(expectedLessSpecificRoute);

    RouteMatch match = router.match(HttpMethod.GET, path).orElse(null);

    assertThat(match)
        .isNotNull()
        .extracting(RouteMatch::route)
        .usingRecursiveComparison()
        .isEqualTo(expectedLessSpecificRoute);
  }

  @Test
  @DisplayName("Should register all different handlers for different verbs on the same route")
  void shouldRegisterAllDiffHandlerForVerbsOnSamePath() {
    String path = "/users";
    Route<NoopRequestHandler> expectedGetRoute = Route.of(HttpMethod.GET, path, handler);
    Route<NoopRequestHandler> expectedPatchRoute = Route.of(HttpMethod.PATCH, path, handler);
    Route<NoopRequestHandler> expectedHeadRoute = Route.of(HttpMethod.HEAD, path, handler);
    Route<NoopRequestHandler> expectedPutRoute = Route.of(HttpMethod.PUT, path, handler);
    Route<NoopRequestHandler> expectedOptionsRoute = Route.of(HttpMethod.OPTIONS, path, handler);
    Route<NoopRequestHandler> expectedPostRoute = Route.of(HttpMethod.POST, path, handler);

    this.router.register(expectedGetRoute);
    this.router.register(expectedPatchRoute);
    this.router.register(expectedHeadRoute);
    this.router.register(expectedPutRoute);
    this.router.register(expectedOptionsRoute);
    this.router.register(expectedPostRoute);

    RouteMatch actualGetMatch = router.match(HttpMethod.GET, path).orElse(null);
    RouteMatch actualPatchMatch = router.match(HttpMethod.PATCH, path).orElse(null);
    RouteMatch actualHeadMatch = router.match(HttpMethod.HEAD, path).orElse(null);
    RouteMatch actualPutMatch = router.match(HttpMethod.PUT, path).orElse(null);
    RouteMatch actualOptionsMatch = router.match(HttpMethod.OPTIONS, path).orElse(null);
    RouteMatch actualPostMatch = router.match(HttpMethod.POST, path).orElse(null);

    assertThat(actualGetMatch).extracting(RouteMatch::route).usingRecursiveComparison().isEqualTo(expectedGetRoute);
    assertThat(actualPatchMatch).extracting(RouteMatch::route).usingRecursiveComparison().isEqualTo(expectedPatchRoute);
    assertThat(actualHeadMatch).extracting(RouteMatch::route).usingRecursiveComparison().isEqualTo(expectedHeadRoute);
    assertThat(actualPutMatch).extracting(RouteMatch::route).usingRecursiveComparison().isEqualTo(expectedPutRoute);
    assertThat(actualOptionsMatch).extracting(RouteMatch::route).usingRecursiveComparison().isEqualTo(expectedOptionsRoute);
    assertThat(actualPostMatch).extracting(RouteMatch::route).usingRecursiveComparison().isEqualTo(expectedPostRoute);
  }

  class NoopRequestHandler {
    void handle() {
      // empty
    }
  }
}
