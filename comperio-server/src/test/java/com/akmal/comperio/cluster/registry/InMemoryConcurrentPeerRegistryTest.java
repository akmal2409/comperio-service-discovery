package com.akmal.comperio.cluster.registry;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.akmal.comperio.cluster.Peer;
import com.akmal.comperio.cluster.Peer.PeerStatus;
import com.akmal.comperio.shared.clock.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InMemoryConcurrentPeerRegistryTest {

  long expireAfterSeconds = 10;
  long startTime = 1000000000L;
  TestClock clock;

  InMemoryConcurrentPeerRegistry expiringRegistry;
  InMemoryConcurrentPeerRegistry nonExpiringRegistry;

  @BeforeEach
  void setup() {
    this.clock = new TestClock(startTime);
    expiringRegistry = new InMemoryConcurrentPeerRegistry(expireAfterSeconds, 3, clock);
    nonExpiringRegistry = new InMemoryConcurrentPeerRegistry(Long.MAX_VALUE, 3, clock);
  }

  @Test
  @DisplayName("Tests addition of peer to the registry")
  void shouldAddPeerToTheRegistry() {
    final var peer = new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.DOWN, false, startTime, startTime, 10);
    nonExpiringRegistry.add(peer);

    assertThat(nonExpiringRegistry.findAll().size()).isEqualTo(1);
    assertThat(nonExpiringRegistry.findAll().iterator().next()).isEqualTo(peer);
  }

  @Test
  @DisplayName("Tests whether the registry evicts the expired entries after the timeout when findAll() is invoked")
  void shouldEvictExpiredEntriesOnFindAll() {
    expiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.DOWN, false, startTime, startTime, 10));
    expiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.UP, false, startTime, startTime, 10));
    long expiresAt = startTime + SECONDS.toMillis(expireAfterSeconds);

    this.clock.currentTimeMs = expiresAt;

    Collection<Peer> peers = expiringRegistry.findAll();

    assertThat(peers.size()).isEqualTo(1);
    assertThat(peers.iterator().next()).extracting(Peer::status).isEqualTo(PeerStatus.UP);
  }

  @Test
  @DisplayName("Tests whether the registry evicts the expired entries after the timeout when add() is invoked")
  void shouldEvictExpiredEntriesOnAdd() {
    expiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.DOWN, false, startTime, startTime, 10));
    expiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.UP, false, startTime, startTime, 10));
    long expiresAt = startTime + SECONDS.toMillis(expireAfterSeconds);

    this.clock.currentTimeMs = expiresAt;

    expiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.UP, false, expiresAt + 10, expiresAt + 10, 10));

    Collection<Peer> peers = expiringRegistry.registry.values();

    assertThat(peers.size()).isEqualTo(2);
    assertThat(peers.stream().map(Peer::status).filter(PeerStatus.UP::equals).toList()).asList().hasSize(2);
  }

  @Test
  @DisplayName("Tests whether the registry evicts the expired entries after the timeout when addAll() is invoked")
  void shouldEvictExpiredEntriesOnAddAll() {
    expiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.DOWN, false, startTime, startTime, 10));
    expiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.UP, false, startTime, startTime, 10));
    long expiresAt = startTime + SECONDS.toMillis(expireAfterSeconds);

    this.clock.currentTimeMs = expiresAt;

    expiringRegistry.addAll(Collections.emptyList());
    Collection<Peer> peers = expiringRegistry.registry.values();

    assertThat(peers.size()).isEqualTo(1);
    assertThat(peers.iterator().next()).extracting(Peer::status).isEqualTo(PeerStatus.UP);
  }


  @Test
  @DisplayName("Should return n distinct elements when getNRandom is called")
  void shouldReturnDistinctNPeers() {
    nonExpiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.DOWN, false, startTime, startTime, 10));
    nonExpiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.UP, false, startTime, startTime, 10));
    nonExpiringRegistry.add(new Peer(UUID.randomUUID(), "localhost", 8000, 8080, PeerStatus.UP, false, startTime, startTime, 10));

    Set<Peer> peers = new HashSet<>(nonExpiringRegistry.getNRandom(2));

    assertThat(peers.size()).isEqualTo(2);
  }



  private class TestClock implements Clock {
    private long currentTimeMs;

    private TestClock(long currentTimeMs) {
      this.currentTimeMs = currentTimeMs;
    }

    @Override
    public long currentTimeMillis() {
      return this.currentTimeMs;
    }

    @Override
    public long currentTimeNanos() {
      return MILLISECONDS.toNanos(this.currentTimeMs);
    }
  }
}
