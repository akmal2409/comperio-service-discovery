package com.akmal.cluster.registry;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.akmal.cluster.Peer;
import com.akmal.cluster.Peer.PeerStatus;
import com.akmal.shared.clock.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.VisibleForTesting;

class InMemoryConcurrentPeerRegistry implements PeerRegistry {

  @VisibleForTesting
  protected final ConcurrentMap<UUID, Peer> registry;
  private final long timeoutAfterDownNanos; // if set to Long.MAX_VALUE the entries will never be evicted, otherwise evicted after having number of ms since being down, to reduce memory footprint
  private final long seedPeerWeight; // weight is used to favour the peer during the selection of N-random items.
  private final Clock clock;

  InMemoryConcurrentPeerRegistry(long timeoutAfterDownSeconds, long seedPeerWeight, Clock clock) {
    this.timeoutAfterDownNanos = SECONDS.toNanos(timeoutAfterDownSeconds);
    this.seedPeerWeight = seedPeerWeight;
    this.clock = clock;
    this.registry = new ConcurrentHashMap<>();
  }

  @Override
  public Collection<Peer> findAll() {
    this.evictExpiredEntries();
    return Collections.unmodifiableCollection(this.registry.values());
  }

  /**
   * The method uses the following algorithm to select randomly n-items given that seed nodes have higher probability.
   * First of all we need to initialise the array with entries (Peer, cumulative weight up until and including that peer entry) as well as compute the cumulative sum till the end.
   * Then we generate a random number in range of (0, totalCumulativeWeightSum) and perform binary search looking for a window of values whose sum is greater than the random number.
   * That allows us to give larger windows to the elements with higher probability (we just assign a higher weight to them).
   *
   * Returns less than n if and only if there are not enough of peers.
   * @param n number of random peers to return.
   * @return
   */
  @Override
  public Collection<Peer> getNRandom(int n) {
    this.evictExpiredEntries();
    // filter out peers and keep the relevant ones only
    List<Peer> peers = new ArrayList<>();

    for (Peer peer: this.registry.values()) {
      if (PeerStatus.UP.equals(peer.status())) {
        peers.add(peer);
      }
    }

    if (peers.size() <= n) return peers;

    int[] peerWeights = new int[peers.size()];
    int cumulativeWeightSum = 0;

    for (int i = 0; i < peers.size(); i++) {
      cumulativeWeightSum += (peers.get(i).isSeedNode() ? seedPeerWeight : 1);
      peerWeights[i] = cumulativeWeightSum;
    }

    Set<Peer> randomPeers = new HashSet<>();


    while (n > 0) {
      int randomWeight = ThreadLocalRandom.current().nextInt(1, cumulativeWeightSum + 1);

      int lo = 0;
      int hi = peerWeights.length - 1;
      int mid;

      while (lo < hi) {
        mid = lo + ((hi - lo) >>> 1);

        if (peerWeights[mid] < randomWeight) lo = mid + 1;
        else hi = mid;
      }

      // now both lo and hi are pointing to the element that has cumulative weight greater than or equal to the randomWeight.
      // now we just need to see if we have already picked this peer, if so we repeat the cycle
      if (!randomPeers.contains(peers.get(lo))) {
        randomPeers.add(peers.get(lo));
        n--;
      }
    }


    return randomPeers;
  }

  @Override
  public void add(Peer peer) {
    this.evictExpiredEntries();
    this.registry.put(peer.id(), peer);
  }

  @Override
  public void addAll(Iterable<Peer> peers) {
    this.evictExpiredEntries();

    for (Peer peer: peers) this.registry.put(peer.id(), peer);
  }

  private void evictExpiredEntries() {
    if (this.timeoutAfterDownNanos == Long.MAX_VALUE) return;

    List<Peer> peersToRemove = new LinkedList<>();

    for (Entry<UUID, Peer> peerEntry: this.registry.entrySet()) {
      if (PeerStatus.DOWN.equals(peerEntry.getValue().status())
              && (this.clock.currentTimeNanos() - MILLISECONDS.toNanos(peerEntry.getValue().lastUpdated())) >= this.timeoutAfterDownNanos) peersToRemove.add(peerEntry.getValue());
    }

    for (Peer peer: peersToRemove) {
      this.registry.remove(peer.id(), peer);
    }
  }
}
