package com.akmal.http.router;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RouteTest {
  class NoopRequestHandler {
    void handle() {
      // empty
    }
  }

  // parser test
  @Test
  @DisplayName("Should parse multiple variables in order and replace them with $")
  void shouldParseMultipleVariablesSuccessfully() {
    final var path = "/users/{userId}/follows/{followeeId}";
    final var expectedReplacedPath = "/users/$/follows/$/";
    final var expectedVariables = new String[]{"userId", "followeeId"};
    final var route = Route.of(HttpMethod.GET, path, new NoopRequestHandler());

    assertThat(route.getPathWithWildcards())
        .isEqualTo(expectedReplacedPath);
    assertThat(route.getVariables())
        .isEqualTo(expectedVariables);
  }

  @Test
  void shouldParseWithoutVariables() {
    final var path = "/hello/";

    final var route = Route.of(HttpMethod.GET, path, new NoopRequestHandler());

    assertThat(route.getPathWithWildcards()).isEqualTo(path);
    assertThat(route.getVariables()).hasSize(0);
  }

  @Test
  void shouldThrowExceptionWhenVariableIsNotEnding() {
    final var path = "/start/{corruptedVar/";

    AssertionsForClassTypes.assertThatThrownBy(() -> {
      Route.of(HttpMethod.GET, path, new NoopRequestHandler());
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowExceptionWhenVariableIsEndingTwice() {
    final var path = "/start/{corruptedVar}}/";
    AssertionsForClassTypes.assertThatThrownBy(() -> {
      Route.of(HttpMethod.GET, path, new NoopRequestHandler());
    }).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowExceptionWhenNoVariableStartButVariableEnd() {
    final var path = "/start/corruptedVar}/";
    AssertionsForClassTypes.assertThatThrownBy(() -> {
      Route.of(HttpMethod.GET, path, new NoopRequestHandler());
    }).isInstanceOf(IllegalArgumentException.class);
  }
}
