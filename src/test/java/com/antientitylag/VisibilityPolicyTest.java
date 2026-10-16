package com.antientitylag;

import com.antientitylag.visibility.VisibilityPolicy;
import com.antientitylag.visibility.VisibilityPolicy.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisibilityPolicyTest {
    @Test
    void reusesFreshStationaryVerdict() {
        assertTrue(VisibilityPolicy.shouldReuse(0, 0.0, false, 4, 1e-6));
        assertTrue(VisibilityPolicy.shouldReuse(4, 0.0, false, 4, 1e-6));
    }

    @Test
    void rejectsStaleMovedOrDirty() {
        assertFalse(VisibilityPolicy.shouldReuse(5, 0.0, false, 4, 1e-6));
        assertFalse(VisibilityPolicy.shouldReuse(0, 1.0, false, 4, 1e-6));
        assertFalse(VisibilityPolicy.shouldReuse(0, 0.0, true, 4, 1e-6));
        assertFalse(VisibilityPolicy.shouldReuse(-1, 0.0, false, 4, 1e-6));
    }

    @Test
    void skipsOnlyFreshOutsideWithCleanBox() {
        assertTrue(VisibilityPolicy.shouldSkipRender(Verdict.OUTSIDE, true, false, true, true));
        assertTrue(VisibilityPolicy.shouldSkipRender(Verdict.OUTSIDE, true, false, false, false));
    }

    @Test
    void neverSkipsOnDoubt() {
        assertFalse(VisibilityPolicy.shouldSkipRender(Verdict.INSIDE, true, false, false, true));
        assertFalse(VisibilityPolicy.shouldSkipRender(Verdict.UNKNOWN, true, false, false, true));
        assertFalse(VisibilityPolicy.shouldSkipRender(Verdict.OUTSIDE, false, false, false, true));
        assertFalse(VisibilityPolicy.shouldSkipRender(Verdict.OUTSIDE, true, true, false, true));
        // Strict compatibility + untrusted (modded) renderer: render normally.
        assertFalse(VisibilityPolicy.shouldSkipRender(Verdict.OUTSIDE, true, false, true, false));
    }
}
