package com.akmal.comperio.http.registry.dto.service;

import com.akmal.comperio.http.registry.dto.v1.ClientRegistrationDto;
import com.akmal.comperio.http.registry.dto.v1.ClientRegistrationRequestDto;
import com.akmal.comperio.http.registry.exception.ClientRegistrationFailureException;
import com.akmal.comperio.registry.ClientRegistration;
import com.akmal.comperio.registry.ClientRegistry;
import com.akmal.comperio.registry.ClientStatus;
import com.akmal.comperio.shared.clock.Clock;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * Service class that decouples the handlers and other external interface's operations behind unified interface
 * for service registry management.
 */
public class ClientRegistrationService {

  private final ClientRegistry clientRegistry;
  private final Clock clock;

  public ClientRegistrationService(ClientRegistry clientRegistry, Clock clock) {
    this.clientRegistry = clientRegistry;
    this.clock = clock;
  }

  public ClientRegistrationDto registerInstance(String appId, String instanceId, ClientRegistrationRequestDto registrationDto) {
    if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(instanceId)) throw new ClientRegistrationFailureException("Failed to register client. Application id and instance id are required");
    if (StringUtils.isEmpty(registrationDto.ipAddress())) throw new ClientRegistrationFailureException("Failed to register client. Ip address is required");

    try {
      final var registration = new ClientRegistration(appId, instanceId,
          registrationDto.host(), registrationDto.port(), InetAddress.getByName(registrationDto.ipAddress()), clock.currentTimeMillis(), clock.currentTimeMillis(),
          1, ClientStatus.COLD);

      this.clientRegistry.register(appId, registration);

      return ClientRegistrationDto.fromClientRegistration(registration);
    } catch (UnknownHostException ex) {
      throw new ClientRegistrationFailureException("Failed to register client. Unknown ip address");
    }
  }

  public void deregisterInstance(String appId, String instanceId) {
    this.clientRegistry.deregister(appId, instanceId);
  }

  public Collection<ClientRegistrationDto> findInstancesByApplicationId(String appId) {
    return this.clientRegistry.findAllByApplication(appId)
               .stream()
               .map(ClientRegistrationDto::fromClientRegistration)
               .toList();
  }

  public Optional<ClientRegistrationDto> findByApplicationIdAndInstanceId(String appId, String instanceId) {
    return this.clientRegistry.findOneByApplicationAndInstanceId(appId, instanceId)
               .map(ClientRegistrationDto::fromClientRegistration);
  }

  public boolean renewByApplicationIdAndInstanceId(String appId, String instanceId) {
    return this.clientRegistry.renewInstance(appId, instanceId);
  }
}
