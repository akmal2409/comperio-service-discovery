package com.akmal.registry;

import com.akmal.shared.clock.SystemClock;
import java.net.InetAddress;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.Result;
import org.openjdk.jcstress.annotations.State;

@JCStressTest
@State
@Outcome(id = "val, true", expect = Expect.FORBIDDEN)
public class ConcurrentClientRegistryNonExpiringConcurrencyReadRemovalTest {

  ConcurrentClientRegistry registry;

  private ClientRegistration readRegistration;
  private boolean deleted;

  @Result
  public static class StringResult {
    String r1;
    String r2;
  }


  private final String APP_ID = "app";
  private final String INSTANCE_ID = "instance_id";
  public ConcurrentClientRegistryNonExpiringConcurrencyReadRemovalTest() {
    try {
      this.registry = new ConcurrentClientRegistry(Long.MAX_VALUE, new SystemClock());
      this.registry.register(APP_ID, new ClientRegistration(APP_ID, INSTANCE_ID, "host.com", 8080, InetAddress.getLocalHost(),
          System.currentTimeMillis(), System.currentTimeMillis(), 2, ClientStatus.UP));
    } catch (Exception e) {
      throw new RuntimeException("Failed");
    }
  }

  @Actor
  public void reader() {
    this.readRegistration = this.registry.findOneByApplicationAndInstanceId(APP_ID, INSTANCE_ID).orElse(null);
  }

  @Actor
  public void deleter() {
    this.deleted = this.registry.deregister(APP_ID, INSTANCE_ID);
  }

  @Arbiter
  public void arbiter(StringResult result) {
    result.r1 = this.readRegistration == null ? "null" : "val";
    result.r2 = this.deleted ? "true" : "false";
  }
}
