package com.akmal.comperio;

import com.akmal.comperio.http.registry.RegistryHttpServer;
import com.akmal.comperio.http.registry.dto.service.ClientRegistrationService;
import com.akmal.comperio.registry.ClientRegistry;
import com.akmal.comperio.shared.clock.SystemClock;

public class Comperio {

  public static void main(String[] args) {
    ClientRegistry clientRegistry = ClientRegistry.withExpiry(30000, new SystemClock());
    ClientRegistrationService clientRegistrationService = new ClientRegistrationService(clientRegistry, new SystemClock());

    RegistryHttpServer registryHttpServer = RegistryHttpServer.bindTo("localhost", 8080, clientRegistrationService);

    registryHttpServer.start();
  }
}
