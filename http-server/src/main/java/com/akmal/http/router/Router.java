package com.akmal.http.router;

import java.util.Optional;

public interface Router {

  /**
   * Adds route to the registry.
   * @param route definition.
   * @return same instance for chaining.
   */
  Router register(Route route);

  /**
   * Resolves the route definition from the given URL.
   * Returns empty optional if none matched.
   *
   * @param path excluding domain
   * @return
   */
  Optional<RouteMatch> match(String path);
}
