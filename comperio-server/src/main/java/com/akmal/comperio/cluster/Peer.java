package com.akmal.comperio.cluster;

import java.util.UUID;

/**
 * The class represent the cluster peer and contains the information to connect to the remote host.
 */
public record Peer(
    UUID id,
    String host,
    int gossipGrpcPort,
    int httpPort,
    Peer.PeerStatus status,
    boolean isSeedNode,
    long lastUpdated,
    long generationTimestamp,
    long heartbeats
) {

  public static enum PeerStatus {
    DOWN(0), UP(1), JOINING(2), SICK(3);
    private int code;

    PeerStatus(int code) {
      this.code = code;
    }
  }

}
