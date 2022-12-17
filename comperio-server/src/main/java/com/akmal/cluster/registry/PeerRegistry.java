package com.akmal.cluster.registry;

import com.akmal.cluster.Peer;
import com.akmal.shared.clock.Clock;
import com.akmal.shared.clock.SystemClock;
import java.util.Collection;

public interface PeerRegistry {
  long DEFAULT_TIMEOUT_AFTER_DOWN_SECONDS = 30;
  long DEFAULT_SEED_NODE_WEIGHT = 3;

  /**
   * Returns all known peers.
   * @return list of known to this node peers.
   */
  Collection<Peer> findAll();

  /**
   * Returns n number specified peers randomly, with seed nodes having the highest probability.
   *
   * @param n number of random peers to return.
   * @return list of randomly picked n peers with seed nodes having the higher probability of choice.
   */
  Collection<Peer> getNRandom(int n);

  /**
   * Adds peer to the registry.
   * @param peer instance.
   */
  void add(Peer peer);

  /**
   * Adds a collection of peers to the registry. Useful during catch up recovery or start up.
   * @param peers list of peers.
   */
  void addAll(Iterable<Peer> peers);


  /**
   * Returns an instance of the {@link InMemoryConcurrentPeerRegistry} with specified configuration params.
   *
   * @param timeoutAfterDownMs time in ms after which the node marked as 'DOWN' should be evicted.
   * @param seedPeerWeight during the n-random selection the weight that the peer should have. Default should be 3.
   * @param clock
   * @return
   */
  static PeerRegistry inMemory(long timeoutAfterDownMs, long seedPeerWeight, Clock clock) {
    return new InMemoryConcurrentPeerRegistry(timeoutAfterDownMs, seedPeerWeight, clock);
  }

  /**
   * Returns an instance of the {@link InMemoryConcurrentPeerRegistry} with default configuration parameters.
   * Will evict the entries marked as 'DOWN' after 30 seconds.
   * Will apply default weight to the seed nodes of 3.
   * @return
   */
  static PeerRegistry inMemory() {
    return new InMemoryConcurrentPeerRegistry(PeerRegistry.DEFAULT_TIMEOUT_AFTER_DOWN_SECONDS, PeerRegistry.DEFAULT_SEED_NODE_WEIGHT, new SystemClock());
  }
}
