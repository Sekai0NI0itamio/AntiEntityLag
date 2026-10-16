package com.antientitylag.update;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small bounded access-order cache with explicit invalidation and hit-rate
 * accounting. Used for derived per-entity values that are expensive to
 * recompute and cheap to key.
 *
 * <p>Bounded by construction (eldest entry evicted past capacity), so it can
 * never grow without limit during long sessions. All methods are synchronized;
 * the mod only touches caches from the client thread, so contention is nil and
 * correctness is trivial.
 */
public final class DerivedCache<K, V> {
    private final LinkedHashMap<K, V> map;
    private final int capacity;
    private long hits;
    private long misses;

    public DerivedCache(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > DerivedCache.this.capacity;
            }
        };
    }

    public synchronized V get(K key) {
        V value = map.get(key);
        if (value != null) {
            hits++;
        } else {
            misses++;
        }
        return value;
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    public synchronized boolean invalidate(K key) {
        return map.remove(key) != null;
    }

    public synchronized void invalidateAll() {
        map.clear();
    }

    public synchronized int size() {
        return map.size();
    }

    public int capacity() {
        return capacity;
    }

    public synchronized double hitRate() {
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / (double) total;
    }

    public synchronized long hits() {
        return hits;
    }

    public synchronized long misses() {
        return misses;
    }

    public synchronized void resetStats() {
        hits = 0;
        misses = 0;
    }
}
