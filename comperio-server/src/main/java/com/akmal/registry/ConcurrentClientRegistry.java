package com.akmal.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link ClientRegistry} interface that supports concurrent access
 * be delegating concurrency to {@link ConcurrentMap} implementations.
 * The only custom lock-free synchronization that has been implemented is the CAS removal of the entry.
 */
@ThreadSafe
class ConcurrentClientRegistry implements ClientRegistry {
  private static final Logger log = LoggerFactory.getLogger(ConcurrentClientRegistry.class);
  private final ConcurrentMap<String, ConcurrentMap<String, ClientRegistration>> registry;

  public ConcurrentClientRegistry() {
    this.registry = new ConcurrentHashMap<>();
  }

  @Override
  public void register(@NotNull String application,@NotNull ClientRegistration registration) {
    registry.computeIfAbsent(application, key -> new ConcurrentHashMap<>())
        .put(registration.instanceId(), registration);

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

      ConcurrentMap<String, ClientRegistration> newMap = new ConcurrentHashMap<>(curMap);

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
    final var instanceMap = registry.get(application);

    if (instanceMap == null) return Collections.emptyList();

    return Collections.unmodifiableCollection(instanceMap.values());
  }

  @Override
  public Optional<ClientRegistration> findOneByApplicationAndInstanceId(@NotNull String application,
      @NotNull String instanceId) {
    final var instanceMap = registry.get(application);

    if (instanceMap == null) return Optional.empty();

    return Optional.ofNullable(instanceMap.get(instanceId));
  }
}
