package com.akmal.http;

import java.util.Map;

/**
 * Wrapper around a vanilla Map and serves the purpose of casting
 * the values to the respective types.
 */
public class RequestVariables {

  private final Map<String, String> variables;


  public RequestVariables(Map<String, String> variables) {
    this.variables = variables;
  }

  public String asString(String key) {
    return this.variables.get(key);
  }

  public Integer asInteger(String key) {
    return Integer.valueOf(this.variables.get(key));
  }

  public Double asDouble(String key) {
    return Double.valueOf(this.variables.get(key));
  }

  public Boolean asBoolean(String key) {
    return Boolean.valueOf(this.variables.get(key));
  }

  public Float asFloat(String key) {
    return Float.valueOf(this.variables.get(key));
  }
}
