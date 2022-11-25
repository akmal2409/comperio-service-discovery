package com.akmal.registry;

import com.akmal.shared.clock.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link ClientRegistry} interface that supports concurrent access
 * be delegating concurrency to {@link ConcurrentMap} implementations.
 * The only custom lock-free synchronization that has been implemented is the CAS removal of the entry.
 * Additionally if the timeout is set to something lower than Long.MAX_VALUE the registry will
 * perform eviction of expired entries lazily on demand during read and removal.
 */
@ThreadSafe
@VisibleForTesting
class ConcurrentClientRegistry implements ClientRegistry {
  protected static final Logger log = LoggerFactory.getLogger(ConcurrentClientRegistry.class);
  @VisibleForTesting protected final ConcurrentMap<String, ConcurrentMap<String, ClientRegistration>> registry;
  private final long timeoutMs;
  private final Clock clock; // solely for testing time dependent methods without waiting

  public ConcurrentClientRegistry(long timeoutMs, Clock clock) {
    this.timeoutMs = timeoutMs;
    this.registry = new ConcurrentHashMap<>();
    this.clock = clock;
  }

  @Override
  public void register(@NotNull String application,@NotNull ClientRegistration registration) {
    registry.computeIfAbsent(application, key -> new ConcurrentHashMap<>())
        .put(registration.instanceId(), registration);

    this.evictExpiredEntries(application);

    log.debug("message=Registered client;application={};instance_id={};address={};host={};status={};timestamp={}", application,
        registration.instanceId(), registration.ipAddress() != null ? registration.ipAddress().getHostAddress() : null, registration.host(),
        registration.status(), registration.timestamp());
  }


  /**
   * Method deregisters the client by removing the entry in the registry.
   * Uses CAS (Compare and Set) algorithm to remove the entry.
   * If the application does not have any clients after the removal, we need to evict that entry
   * to avoid the memory leak.
   *
   * @param application name (service group name)
   * @param instanceId unique identifier of an instance.
   * @return Returns true if and only if the entry was present before.
   */
  @Override
  public boolean deregister(@NotNull String application, @NotNull String instanceId) {
    ConcurrentMap<String, ClientRegistration> curMap = registry.get(application);
    boolean removed = false;
    ClientRegistration oldRegistration = null;

    while (true) {
      if (curMap == null) break;

      final var newMap = cloneClientMapFilterExpired(curMap);

      if ((oldRegistration = newMap.remove(instanceId)) != null) {
        removed = true;
      }

      if (newMap.isEmpty()) {
        if (!registry.remove(application, curMap)) {
          curMap = registry.get(application);
          removed = false;
          oldRegistration = null;
          continue;
        }
      } else {
        if (!registry.replace(application, curMap, newMap)) {
          curMap = registry.get(application);
          removed = false;
          oldRegistration = null;
          continue;
        }
      }

      break;
    }

    if (oldRegistration != null) {
      log.debug("message=De-registered client;application={};instance_id={};address={};host={};status={};timestamp={}", application,
          oldRegistration.instanceId(), oldRegistration.ipAddress() != null ? oldRegistration.ipAddress().getHostAddress() : null, oldRegistration.host(),
          oldRegistration.status(), oldRegistration.timestamp());
    }

    return removed;
  }

  @Override
  public Collection<ClientRegistration> findAllByApplication(@NotNull String application) {
    this.evictExpiredEntries(application);

    final var instanceMap = registry.get(application);

    if (instanceMap == null) return Collections.emptyList();

    return Collections.unmodifiableCollection(instanceMap.values());
  }

  @Override
  public Optional<ClientRegistration> findOneByApplicationAndInstanceId(@NotNull String application,
      @NotNull String instanceId) {
    this.evictExpiredEntries(application);

    final var instanceMap = registry.get(application);

    if (instanceMap == null) return Optional.empty();

    return Optional.ofNullable(instanceMap.get(instanceId));
  }

  /**
   * Lazily evicts old entries for a specified key by using CAS algorithm.
   * Checks if currentTime - entryTime < timeoutMs in that case we leave the entry.
   * @param application
   */
  private void evictExpiredEntries(String application) {
    if (timeoutMs == Long.MAX_VALUE) return;

    var curMap = registry.get(application);

    while (true) {
      if (curMap == null) break;

      final var newMap = cloneClientMapFilterExpired(curMap);

      if (newMap.size() == curMap.size()) break;

      if (newMap.isEmpty()) {
        if (!registry.remove(application, curMap)) {
          curMap = registry.get(application);
          continue;
        }
      } else {
        if (!registry.replace(application, curMap, newMap)) {
          curMap = registry.get(application);
          continue;
        }
      }

      break;
    }
  }

  private ConcurrentMap<String, ClientRegistration> cloneClientMapFilterExpired(ConcurrentMap<String, ClientRegistration> map) {
    final var newMap = new ConcurrentHashMap<String, ClientRegistration>(map.size());
    long currentTime = clock.currentTimeMillis();

    for (Entry<String, ClientRegistration> entry: map.entrySet()) {
      if (timeoutMs == Long.MAX_VALUE || (currentTime - entry.getValue().timestamp()) < timeoutMs) {
        newMap.put(entry.getKey(), entry.getValue());
      }
    }

    return newMap;
  }
}
