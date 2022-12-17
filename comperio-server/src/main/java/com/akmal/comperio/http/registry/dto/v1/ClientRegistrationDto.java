package com.akmal.comperio.http.registry.dto.v1;

import com.akmal.comperio.registry.ClientRegistration;

public record ClientRegistrationDto(
  String application,
  String instanceId,
  String host,
  int port,
  String ipAddress,
  long registrationTimestamp,
  long lastRenewalTimestamp,
  long renewalsSinceRegistration,
  String status
) {

  public static ClientRegistrationDto fromClientRegistration(ClientRegistration clientRegistration) {
    return new ClientRegistrationDto(clientRegistration.application(),
        clientRegistration.instanceId(), clientRegistration.host(),
        clientRegistration.port(),
        clientRegistration.ipAddress() != null ? clientRegistration.ipAddress().toString() : null,
        clientRegistration.registrationTimestamp(),
        clientRegistration.lastRenewalTimestamp(),
        clientRegistration.renewalsSinceRegistration(),
        clientRegistration.status().name());
  }
}
