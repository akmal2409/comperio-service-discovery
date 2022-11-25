package com.akmal.shared.clock;

/**
 * Wrapper of the System time methods, solely for testing temporal methods without
 * waiting.
 */
public class SystemClock implements Clock {

  @Override
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  @Override
  public long currentTimeNanos() {
    return System.nanoTime();
  }
}
