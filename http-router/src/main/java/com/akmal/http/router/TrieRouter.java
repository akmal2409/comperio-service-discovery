package com.akmal.http.router;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Basic implementation of the TrieRouter with some known limitations.
 * For example, if the path1 = /users/{userId}/cars and path2 = /users/specific/cars, then given /users/specifi/cars it will be able to resolve to the correct route
 * and consider 'specifi' as a variable. Therefore, it can withstand one overlap in paths.
 * However, if there are more overlapping routes, it will not be able to resolve them, like in the case above, if the user id is actually 'specific' it will resolve to the /users/specific/cars and
 * will not consider it a template variable. Example of when it will fail is following: path1 = /users/{userId}/cars/hello path2 = /users/specific/cars , given path = /users/specific/cars/hello
 * it will fail to match any of the routes.
 */
class TrieRouter<H> implements Router<H> {

  private static class TrieNode {
    protected final TrieNode[] children;

    private TrieNode() {
      this.children = new TrieNode[128];
    }
  }

  private static class TerminalTrieNode<H> extends TrieNode {
    private final Route<H>[] routes;

    @SuppressWarnings("unchecked")
    private TerminalTrieNode() {
      this.routes = (Route<H>[]) new Route[6];
    }

    private void addRouteForMethod(HttpMethod method, Route<H> route) {
      this.routes[method.ordinal()] = route;
    }

    private Route<H> routeForMethod(HttpMethod method) {
      return this.routes[method.ordinal()];
    }
  }

  private final TrieNode root;

  TrieRouter() {
    this.root = new TrieNode();
  }


  @SuppressWarnings("unchecked")
  @Override
  public Router<H> register(Route<H> route) {

    TrieNode cursor = this.root;
    char[] characters = route.getPathWithWildcards().toCharArray();

    for (int i = 0; i < characters.length - 1; i++) {
      if (cursor.children[characters[i]] == null) {
        cursor.children[characters[i]] = new TrieNode();
      }

      cursor = cursor.children[characters[i]];
    }

    if (cursor.children[characters[characters.length - 1]] == null) cursor.children[characters[characters.length - 1]] = new TerminalTrieNode();

    ((TerminalTrieNode<H>) cursor.children[characters[characters.length - 1]]).addRouteForMethod(route.getMethod(), route);
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<RouteMatch<H>> match(HttpMethod method, String path) {
    path = path.charAt(path.length() - 1) != '/' ? path + '/' : path; // path must always end with an ending slash.
    TrieNode cursor = this.root;

    final var variables =  new ArrayList<String>();

    char[] characters = path.toCharArray();

    int lastSlashIndex = -1;
    TrieNode lastWildcardNode = null;

    for (int i = 0; i < characters.length; i++) {
      if (characters[i] < 127 && cursor.children[characters[i]] != null) {
        cursor = cursor.children[characters[i]];
        if (characters[i] == '/') {
          lastSlashIndex = i;
          lastWildcardNode = cursor.children[Route.VARIABLE_PLACEHOLDER];
        }
      } else {
        if (i == 0) return Optional.empty();
        StringBuilder varBuilder = new StringBuilder(4);
        TrieNode nextNode = cursor.children[Route.VARIABLE_PLACEHOLDER];

        // we might have gotten partial match with a route, however, that was intended to be a variable, we need to assemble what we have skipped
        if (characters[i - 1] != '/') {
          if (lastSlashIndex == -1 || lastWildcardNode == null) return Optional.empty();
          varBuilder = new StringBuilder(path.substring(lastSlashIndex + 1, i)); // otherwise backtrack to the last wildcard node between /<content>/ and consider whatever we assembled as part of the variable
          nextNode = lastWildcardNode;
        } else if (cursor.children[Route.VARIABLE_PLACEHOLDER] == null) return Optional.empty();
        // it might be then a template variable, to check that we need to see if the current character is after a '/'
        // if it is, then we need to see if we have any placeholder for a variable at this part of path, if not, there is no match.



        // assemble the variable
        while (i < characters.length) { // because last char is always a slash
          if (characters[i] == '/') break;
          else if (i == characters.length - 1) throw new IllegalArgumentException("Corrupted path, it must always end with a '/'");
          varBuilder.append(characters[i++]);
        }
        i--;
        variables.add(varBuilder.toString());
        cursor = nextNode;
      }
    }

    if (cursor instanceof TerminalTrieNode terminal) {
      terminal = (TerminalTrieNode<H>) terminal;
      Route<H> route = terminal.routeForMethod(method);

      if (route != null) {
        return Optional.of(RouteMatch.withVariables(terminal.routeForMethod(method), variables));
      }
    }


    return Optional.empty();
  }
}
