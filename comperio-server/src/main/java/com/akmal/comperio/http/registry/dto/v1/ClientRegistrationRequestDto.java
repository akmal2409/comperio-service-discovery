package com.akmal.comperio.http.registry.dto.v1;

public record ClientRegistrationRequestDto(
    String host,
    int port,
    String ipAddress
) {

}
