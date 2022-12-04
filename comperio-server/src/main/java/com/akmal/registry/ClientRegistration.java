package com.akmal.registry;

import java.net.InetAddress;
import java.time.Instant;
import lombok.With;
import net.jcip.annotations.Immutable;

@Immutable
@With
public record ClientRegistration(
    String application,
    String instanceId,
    String host,
    int port,
    InetAddress ipAddress,
    long registrationTimestamp,
    long lastRenewalTimestamp,
    long renewalsSinceRegistration,
    ClientStatus status
) {

}
