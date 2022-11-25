package com.akmal.registry;

import java.util.Collection;
import java.util.Optional;

/**
 * A common contract for a registry that stores {@link ClientRegistration} records.
 * Provides an API to register, deregister and query clients.
 */
public interface ClientRegistry {

  /**
   * Registers the client in the registry with a given application name.
   * If the application name was not associated with any of the instances, it creates a new group entry.
   * Else it appends to the existing application to instance mapping.
   *
   * @param application name (service group name)
   * @param registration {@link ClientRegistration} instance that holds remote client details
   */
  void register(String application, ClientRegistration registration);

  /**
   * Method deregisters the client and removes associated mapping if present.
   *
   * @param application name (service group name)
   * @param instanceId unique identifier of an instance.
   * @return operation status, whether the client was deregistered or not.
   */
  boolean deregister(String application, String instanceId);

  /**
   * Queries all the {@link ClientRegistration} instances associated with an application (if any).
   *
   * @param application name (service group)
   * @return {@link Collection<ClientRegistration>} either empty or with at least 1 client.
   */
  Collection<ClientRegistration> findAllByApplication(String application);

  /**
   * Queries {@link ClientRegistration} instance information by composite key (application, instanceId).
   *
   * @param application name (service group)
   * @param instanceId unique identifier of an instance.
   * @return {@link Optional<ClientRegistration>}
   */
  Optional<ClientRegistration> findOneByApplicationAndInstanceId(String application, String instanceId);

  /**
   * Constructs fresh instance of the registry.
   *
   * @return empty registry.
   */
  static ClientRegistry newInstance() {
    return new ConcurrentClientRegistry();
  }
}
