package com.antientitylag.visibility;

/**
 * Pure decision logic for the adaptive visibility cache.
 *
 * <p>Rules, in order of strictness:
 * <ol>
 *   <li>A dirty bounding box always forces a retest.</li>
 *   <li>A verdict older than {@code maxReuseFrames} is never reused.</li>
 *   <li>Camera motion beyond epsilon invalidates cached verdicts.</li>
 *   <li>Rendering is skipped only for a fresh {@code OUTSIDE} verdict with a
 *   clean box. When in doubt ({@code UNKNOWN}, stale, dirty, or an untrusted
 *   modded renderer under strict compatibility) the entity renders normally.</li>
 * </ol>
 */
public final class VisibilityPolicy {
    public enum Verdict {
        UNKNOWN,
        INSIDE,
        OUTSIDE
    }

    private VisibilityPolicy() {
    }

    public static boolean shouldReuse(
            int framesSinceTest,
            double cameraMovedSq,
            boolean boundsDirty,
            int maxReuseFrames,
            double cameraEpsilonSq) {
        if (boundsDirty) {
            return false;
        }
        if (framesSinceTest < 0 || framesSinceTest > maxReuseFrames) {
            return false;
        }
        return cameraMovedSq <= cameraEpsilonSq;
    }

    public static boolean shouldSkipRender(
            Verdict verdict,
            boolean fresh,
            boolean boundsDirty,
            boolean compatStrict,
            boolean rendererTrusted) {
        if (verdict != Verdict.OUTSIDE) {
            return false;
        }
        if (!fresh || boundsDirty) {
            return false;
        }
        if (compatStrict && !rendererTrusted) {
            return false;
        }
        return true;
    }
}
