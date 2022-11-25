package com.akmal.registry;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.akmal.shared.clock.Clock;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConcurrentClientRegistryTest {

  static class TestClock implements Clock {
    long time;

    @Override
    public long currentTimeMillis() {
      return this.time;
    }

    @Override
    public long currentTimeNanos() {
      return (long) (this.time * 1e+6);
    }
  }

  long timeout = 20L;
  TestClock clock = new TestClock();
  ConcurrentClientRegistry expiringRegistry;
  ConcurrentClientRegistry nonExpiringRegistry;

  @BeforeEach
  void setup() {
    expiringRegistry = new ConcurrentClientRegistry(timeout, clock);
    nonExpiringRegistry = new ConcurrentClientRegistry(Long.MAX_VALUE, new TestClock());
  }


  @Test
  @DisplayName("Should register client registration successfully from a single thread, no previous entry")
  void shouldRegisterClientEntrySuccessfullyNoPrevious() throws UnknownHostException {
    ClientRegistration expectedRegistration = new ClientRegistration("test", "test", "http://localhost",
        InetAddress.getLocalHost(), System.currentTimeMillis(), ClientStatus.UP);

    nonExpiringRegistry.register("test", expectedRegistration);

    ClientRegistration actualRegistration = nonExpiringRegistry.findOneByApplicationAndInstanceId("test", "test")
                                                .orElse(null);

    assertThat(actualRegistration)
        .isNotNull()
        .usingRecursiveComparison()
        .isEqualTo(expectedRegistration);
  }

  @Test
  @DisplayName("Should register client registration successfully from a single thread with previous entry present")
  void shouldRegisterClientEntrySuccessfullyPrevious() throws UnknownHostException {
    ClientRegistration expectedOldRegistration = new ClientRegistration("test", "test", "http://localhost",
        InetAddress.getLocalHost(), System.currentTimeMillis(), ClientStatus.UP);
    nonExpiringRegistry.register(expectedOldRegistration.application(), expectedOldRegistration);

    ClientRegistration expectedRegistration = new ClientRegistration("test", "test", "http://localhost",
        InetAddress.getLocalHost(), System.currentTimeMillis(), ClientStatus.UP);

    nonExpiringRegistry.register(expectedRegistration.application(), expectedRegistration);

    ClientRegistration actualRegistration = nonExpiringRegistry.findOneByApplicationAndInstanceId(expectedRegistration.application(), expectedRegistration.instanceId())
                                                .orElse(null);

    assertThat(actualRegistration)
        .isNotNull()
        .usingRecursiveComparison()
        .isEqualTo(expectedRegistration);
  }

  @Test
  @DisplayName("Should de-register client successfully when present, single thread")
  void shouldDeregisterClientWhenPresent() throws UnknownHostException {
    ClientRegistration expectedRegistration = new ClientRegistration("test", "test", "http://localhost",
        InetAddress.getLocalHost(), System.currentTimeMillis(), ClientStatus.UP);
    nonExpiringRegistry.register(expectedRegistration.application(), expectedRegistration);

    boolean status = nonExpiringRegistry.deregister(expectedRegistration.application(), expectedRegistration.instanceId());

    ClientRegistration actualRegistration = nonExpiringRegistry.findOneByApplicationAndInstanceId(expectedRegistration.application(), expectedRegistration.instanceId())
                                                .orElse(null);
    assertThat(actualRegistration)
        .isNull();

    assertThat(status).isTrue();
  }

  @Test
  @DisplayName("Should de-register client successfully when present and other client registration being present in the same client map, single thread")
  void shouldDeregisterClientWhenPresentWithOthersIntact() throws UnknownHostException {
    ClientRegistration otherRegistration = new ClientRegistration("test", "test2", "http://localhost",
        InetAddress.getLocalHost(), System.currentTimeMillis(), ClientStatus.UP);
    ClientRegistration expectedRegistration = new ClientRegistration("test", "test", "http://localhost",
        InetAddress.getLocalHost(), System.currentTimeMillis(), ClientStatus.UP);
    nonExpiringRegistry.register(expectedRegistration.application(), expectedRegistration);
    nonExpiringRegistry.register(otherRegistration.application(), otherRegistration);

    boolean status = nonExpiringRegistry.deregister(expectedRegistration.application(), expectedRegistration.instanceId());

    ClientRegistration actualRegistration = nonExpiringRegistry.findOneByApplicationAndInstanceId(expectedRegistration.application(), expectedRegistration.instanceId())
                                                .orElse(null);

    ClientRegistration actualOtherRegistration = nonExpiringRegistry.findOneByApplicationAndInstanceId(otherRegistration.application(), otherRegistration.instanceId())
                                                                       .orElse(null);
    assertThat(actualRegistration)
        .isNull();

    assertThat(status).isTrue();
    assertThat(actualOtherRegistration)
        .isNotNull()
        .isEqualTo(otherRegistration);
  }

  @Test
  @DisplayName("Should not de-register client when not present, but other client is present under the same application")
  void shouldNotDeregisterClientWhenNotPresent() throws UnknownHostException {
    ClientRegistration otherRegistration = new ClientRegistration("test", "testOther", "http://localhost",
        InetAddress.getLocalHost(), System.currentTimeMillis(), ClientStatus.UP);
    nonExpiringRegistry.register(otherRegistration.application(), otherRegistration);

    boolean status = nonExpiringRegistry.deregister(otherRegistration.application(), "not-present");

    ClientRegistration actualRegistration = nonExpiringRegistry.findOneByApplicationAndInstanceId(otherRegistration.application(), "not-present")
                                                .orElse(null);
    ClientRegistration actualOtherRegistration = nonExpiringRegistry.findOneByApplicationAndInstanceId(otherRegistration.application(), otherRegistration.instanceId())
                                                     .orElse(null);

    assertThat(actualRegistration)
        .isNull();

    assertThat(status).isFalse();

    assertThat(actualOtherRegistration)
        .isNotNull()
        .isEqualTo(otherRegistration);
  }

//  Testing for potential memory leaks
  @Test
  @DisplayName("Should remove empty client map when last client is deregistered")
  void shouldRemoveMapWhenLastClientDeregistered() throws UnknownHostException {
    ClientRegistration registration = new ClientRegistration("test", "test", "http://localhost",
        InetAddress.getLocalHost(), System.currentTimeMillis(), ClientStatus.UP);

    nonExpiringRegistry.register(registration.application(), registration);

    boolean removalStatus = nonExpiringRegistry.deregister(registration.application(), registration.instanceId());

    assertThat(removalStatus).isTrue();
    assertThat(nonExpiringRegistry.registry.get(registration.application())).isNull();
  }

  // Testing eviction
  @Test
  @DisplayName("Should evict expired entries from registry on read one")
  void shouldEvictExpiredEntriesFromRegistryOnRead() throws UnknownHostException {
    long entryTimestamp = System.currentTimeMillis();
    long timeOfExpiry = entryTimestamp + timeout;

    ClientRegistration evictedRegistration = new ClientRegistration("test", "test1", "http://localhost",
        InetAddress.getLocalHost(), entryTimestamp, ClientStatus.UP);
    ClientRegistration nonEvictedRegistration =  new ClientRegistration("test", "test2", "http://localhost",
        InetAddress.getLocalHost(), entryTimestamp + timeOfExpiry + 1, ClientStatus.UP);

    expiringRegistry.register(evictedRegistration.application(), evictedRegistration);
    expiringRegistry.register(nonEvictedRegistration.application(), nonEvictedRegistration);

    clock.time = timeOfExpiry;

    ClientRegistration actualEvictedRegistration = expiringRegistry.findOneByApplicationAndInstanceId(evictedRegistration.application(), evictedRegistration.instanceId())
                                                .orElse(null);
    ClientRegistration actualNonEvictedRegistration = expiringRegistry.findOneByApplicationAndInstanceId(nonEvictedRegistration.application(), nonEvictedRegistration.instanceId())
                                                       .orElse(null);

    assertThat(actualEvictedRegistration).isNull();
    assertThat(actualNonEvictedRegistration).isNotNull();
  }

  @Test
  @DisplayName("Should evict expired entries from registry on read all by application")
  void shouldEvictExpiredEntriesFromRegistryOnReadAllByApp() throws UnknownHostException {
    long entryTimestamp = System.currentTimeMillis();
    long timeOfExpiry = entryTimestamp + timeout;

    ClientRegistration evictedRegistration = new ClientRegistration("test", "test1", "http://localhost",
        InetAddress.getLocalHost(), entryTimestamp, ClientStatus.UP);
    ClientRegistration nonEvictedRegistration =  new ClientRegistration("test", "test2", "http://localhost",
        InetAddress.getLocalHost(), entryTimestamp + timeOfExpiry + 1, ClientStatus.UP);

    expiringRegistry.register(evictedRegistration.application(), evictedRegistration);
    expiringRegistry.register(nonEvictedRegistration.application(), nonEvictedRegistration);

    clock.time = timeOfExpiry;

    Collection<ClientRegistration> entries = expiringRegistry.findAllByApplication(evictedRegistration.application());


    assertThat(entries)
        .isNotNull()
        .usingRecursiveComparison()
        .isEqualTo(Collections.unmodifiableCollection(List.of(nonEvictedRegistration)));

  }

  @Test
  @DisplayName("Should evict expired entries from registry on delete")
  void shouldEvictExpiredEntriesFromRegistryOnDelete() throws UnknownHostException {
    long entryTimestamp = System.currentTimeMillis();
    long timeOfExpiry = entryTimestamp + timeout;

    ClientRegistration evictedRegistration = new ClientRegistration("test", "test1", "http://localhost",
        InetAddress.getLocalHost(), entryTimestamp, ClientStatus.UP);
    ClientRegistration nonEvictedRegistration =  new ClientRegistration("test", "test2", "http://localhost",
        InetAddress.getLocalHost(), entryTimestamp + timeOfExpiry + 1, ClientStatus.UP);

    expiringRegistry.register(evictedRegistration.application(), evictedRegistration);
    expiringRegistry.register(nonEvictedRegistration.application(), nonEvictedRegistration);

    clock.time = timeOfExpiry;

    expiringRegistry.deregister(nonEvictedRegistration.application(), nonEvictedRegistration.instanceId());

    ClientRegistration actualEvictedRegistration = expiringRegistry.findOneByApplicationAndInstanceId(evictedRegistration.application(), evictedRegistration.instanceId())
                                                       .orElse(null);


    assertThat(actualEvictedRegistration).isNull();
    assertThat(expiringRegistry.registry.get(evictedRegistration.application())).isNull();
  }

  @Test
  @DisplayName("Should evict expired entries from registry on write")
  void shouldEvictExpiredEntriesFromRegistryOnWrite() throws UnknownHostException {
    long entryTimestamp = System.currentTimeMillis();
    long timeOfExpiry = entryTimestamp + timeout;

    ClientRegistration evictedRegistration = new ClientRegistration("test", "test1", "http://localhost",
        InetAddress.getLocalHost(), entryTimestamp, ClientStatus.UP);
    ClientRegistration nonEvictedRegistration =  new ClientRegistration("test", "test2", "http://localhost",
        InetAddress.getLocalHost(), entryTimestamp + timeOfExpiry + 1, ClientStatus.UP);

    expiringRegistry.register(evictedRegistration.application(), evictedRegistration);

    clock.time = timeOfExpiry;

    expiringRegistry.register(nonEvictedRegistration.application(), nonEvictedRegistration);

    ClientRegistration actualEvictedRegistration = expiringRegistry.findOneByApplicationAndInstanceId(evictedRegistration.application(), evictedRegistration.instanceId())
                                                       .orElse(null);
    ClientRegistration actualNonEvictedRegistration = expiringRegistry.findOneByApplicationAndInstanceId(nonEvictedRegistration.application(), nonEvictedRegistration.instanceId())
                                                          .orElse(null);

    assertThat(actualEvictedRegistration).isNull();
    assertThat(actualNonEvictedRegistration).isNotNull();
  }
}
