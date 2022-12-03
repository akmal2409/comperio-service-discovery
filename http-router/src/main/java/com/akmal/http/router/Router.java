package com.akmal.http.router;

import java.util.Optional;

public interface Router<H> {

  /**
   * Adds route to the registry.
   * @param route definition.
   * @return same instance for chaining.
   */
  Router<H> register(Route<H> route);

  /**
   * Resolves the route definition from the given URL.
   * Returns empty optional if none matched.
   *
   * @param path excluding domain
   * @return
   */
  Optional<RouteMatch<H>> match(HttpMethod method, String path);

  static <H> Router<H> defaultRouter() {
    return new TrieRouter<>();
  }

}
