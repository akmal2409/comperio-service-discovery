package com.akmal.http.registry.handlers;

import com.akmal.http.HttpStatus;
import com.akmal.http.MediaType;
import com.akmal.http.ObjectMapperHolder;
import com.akmal.http.RequestVariables;
import com.akmal.http.registry.dto.service.ClientRegistrationService;
import com.akmal.http.registry.dto.v1.ApiError;
import com.akmal.http.registry.dto.v1.ClientRegistrationDto;
import com.akmal.http.registry.dto.v1.ClientRegistrationRequestDto;
import com.akmal.http.registry.exception.ClientRegistrationFailureException;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * Class wraps set of handlers for routes to interact with the client registry.
 */
public class ApplicationInstanceHandlers {

  private static final String APP_ID_VARIABLE = "appId";
  private static final String INSTANCE_ID_VARIABLE = "instanceId";
  private final ClientRegistrationService clientRegistrationService;

  public ApplicationInstanceHandlers(ClientRegistrationService clientRegistrationService) {
    this.clientRegistrationService = clientRegistrationService;
  }

  public void handleRegistration(HttpServerExchange exchange, RequestVariables variables) {
    String appId = variables.asString(APP_ID_VARIABLE);
    String instanceId = variables.asString(INSTANCE_ID_VARIABLE);

    exchange.getRequestReceiver().receiveFullBytes((exchange1, bytes) -> {
      try {
        final var registrationRequest = ObjectMapperHolder.getInstance().readValue(bytes,
            ClientRegistrationRequestDto.class);

        final var registration = this.clientRegistrationService.registerInstance(appId, instanceId, registrationRequest);

        exchange.setStatusCode(HttpStatus.OK.value());
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), MediaType.APPLICATION_JSON);
        exchange.getResponseSender().send(ObjectMapperHolder.getInstance().writeValueAsString(registration));
      } catch (IOException e) {
        throw new ClientRegistrationFailureException("Failed to process client registration data", e);
      }
    });
  }

  public void handleHeartBeat(HttpServerExchange exchange, RequestVariables variables)
      throws JsonProcessingException {
    String appId = variables.asString(APP_ID_VARIABLE);
    String instanceId = variables.asString(INSTANCE_ID_VARIABLE);

    if (this.clientRegistrationService.renewByApplicationIdAndInstanceId(appId, instanceId)) {
      exchange.setStatusCode(HttpStatus.NO_CONTENT.value());
    } else {
      exchange.setStatusCode(HttpStatus.BAD_REQUEST.value());
      exchange.getResponseHeaders().add(new HttpString("Content-Type"), MediaType.APPLICATION_JSON);
      final var apiException = new ApiError("Failed to process heartbeat, client might not have been registered",
          Instant.now(), "CR-00001");
      exchange.getResponseSender().send(ObjectMapperHolder.getInstance().writeValueAsString(apiException));
    }
  }

  public void handleQueryApplication(HttpServerExchange exchange, RequestVariables variables) throws Exception {

    Collection<ClientRegistrationDto> registrations =  this.clientRegistrationService
                                                           .findInstancesByApplicationId(variables.asString(
                                                               APP_ID_VARIABLE));

    exchange.setStatusCode(HttpStatus.OK.value());
    exchange.getResponseHeaders().add(new HttpString("Content-Type"), MediaType.APPLICATION_JSON);
    exchange.getResponseSender().send(ObjectMapperHolder.getInstance().writeValueAsString(registrations));
  }

  public void handleDeregistration(HttpServerExchange exchange, RequestVariables requestVariables) {
    String appId = requestVariables.asString(APP_ID_VARIABLE);
    String instanceId = requestVariables.asString(INSTANCE_ID_VARIABLE);

    this.clientRegistrationService.deregisterInstance(appId, instanceId);

    exchange.setStatusCode(HttpStatus.NO_CONTENT.value());
  }

  public void handleQueryInstance(HttpServerExchange exchange, RequestVariables requestVariables)
      throws JsonProcessingException {
    Optional<ClientRegistrationDto> clientOptional = this.clientRegistrationService
                                                         .findByApplicationIdAndInstanceId(requestVariables.asString(
                                                             APP_ID_VARIABLE), requestVariables.asString(
                                                             INSTANCE_ID_VARIABLE));

    exchange.getResponseHeaders().add(new HttpString("Content-Type"), MediaType.APPLICATION_JSON);

    if (clientOptional.isEmpty()) {
      exchange.setStatusCode(HttpStatus.NOT_FOUND.value());
    } else {
      exchange.setStatusCode(HttpStatus.OK.value());
      exchange.getResponseSender().send(ObjectMapperHolder.getInstance().writeValueAsString(clientOptional.get()));
    }
  }
}
