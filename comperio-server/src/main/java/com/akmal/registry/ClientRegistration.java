package com.akmal.registry;

import java.net.InetAddress;
import java.time.Instant;
import net.jcip.annotations.Immutable;

@Immutable
public record ClientRegistration(
    String application,
    String instanceId,
    String host,
    InetAddress ipAddress,
    long timestamp,
    ClientStatus status
) {

}
