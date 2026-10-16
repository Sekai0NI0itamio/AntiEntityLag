package com.antientitylag.render;

/**
 * Pure policy for expensive optional rendering layers.
 *
 * <p>v1 verdict: name-tag and shadow suppression hooks are
 * <em>investigated but deferred</em> (see ARCHITECTURE.md) because the
 * cloud-only pipeline cannot runtime-verify renderer-internal injection
 * points, and an unverified visual hook is worse than no hook. This class
 * captures the agreed distance/hysteresis policy now so the future hook has a
 * tested contract to implement against — and so reviewers can see exactly what
 * behavior is (not yet) enabled.
 */
public final class CrowdRenderPolicy {
    private CrowdRenderPolicy() {
    }

    /**
     * Name tags render when the feature is off, the entity is within cull
     * distance, or it is the current interaction/camera target.
     */
    public static boolean shouldRenderNameTag(
            double distSq, double cullDistSq, boolean targeted, boolean enabled) {
        if (!enabled) {
            return true;
        }
        if (targeted) {
            return true;
        }
        return distSq <= cullDistSq;
    }

    /**
     * Shadow opacity with linear falloff between {@code nearSq} (full base
     * opacity) and {@code farSq} (fully suppressed). Returns {@code base}
     * unchanged when the feature is disabled.
     */
    public static float shadowOpacity(
            double distSq, double nearSq, double farSq, float base, boolean enabled) {
        if (!enabled) {
            return base;
        }
        if (distSq <= nearSq) {
            return base;
        }
        if (distSq >= farSq || farSq <= nearSq) {
            return 0.0f;
        }
        double t = (distSq - nearSq) / (farSq - nearSq);
        return (float) (base * (1.0 - t));
    }
}
