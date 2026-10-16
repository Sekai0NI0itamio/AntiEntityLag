package com.antientitylag;

import com.antientitylag.update.DerivedCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DerivedCacheTest {
    @Test
    void hitRateAccounting() {
        DerivedCache<String, Integer> cache = new DerivedCache<>(4);
        cache.put("a", 1);
        assertEquals(1, cache.get("a"));
        assertNull(cache.get("missing"));
        assertEquals(0.5, cache.hitRate(), 1e-9);
        assertEquals(1, cache.hits());
        assertEquals(1, cache.misses());
    }

    @Test
    void boundedEviction() {
        DerivedCache<Integer, Integer> cache = new DerivedCache<>(2);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        assertEquals(2, cache.size());
        assertNull(cache.get(1));
    }

    @Test
    void invalidation() {
        DerivedCache<String, Integer> cache = new DerivedCache<>(4);
        cache.put("a", 1);
        assertTrue(cache.invalidate("a"));
        assertFalse(cache.invalidate("a"));
        assertNull(cache.get("a"));
        cache.put("b", 2);
        cache.invalidateAll();
        assertEquals(0, cache.size());
        assertNull(cache.get("b"));
    }
}
