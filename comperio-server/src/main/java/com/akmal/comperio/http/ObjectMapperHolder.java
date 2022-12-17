package com.akmal.comperio.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;

public class ObjectMapperHolder {

  private static final class Holder {
    private static final ObjectMapper INSTANCE = new ObjectMapper();

    static {
      INSTANCE.registerModule(new JSR310Module());
    }
  }


  public static ObjectMapper getInstance() {
    return Holder.INSTANCE;
  }
}
