package com.antientitylag.visibility;

import com.antientitylag.config.AntiEntityLagConfig;
import com.antientitylag.profile.EntityProfiler;
import com.antientitylag.update.DerivedCache;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Adaptive frustum-visibility reuse built on top of vanilla culling.
 *
 * <p>Vanilla already frustum-culls; this cache avoids <em>re-testing</em>
 * entities whose verdict cannot have changed (stationary box, stationary
 * camera, fresh verdict) and gives the render gate a single authoritative
 * answer per entity per frame. It never invents visibility: without a valid
 * frustum snapshot the verdict is {@code UNKNOWN} and everything renders.
 * It never touches updates — only render submission is ever skipped, and only
 * for fresh {@code OUTSIDE} verdicts with clean boxes.
 *
 * <p>Bounded to 4096 entries (access-order eviction). World changes and
 * disconnects invalidate everything. Per-frame fresh-test budget comes from
 * config; overflow entities reuse their last verdict or render normally.
 */
public final class VisibilityCache {
    private static final VisibilityCache INSTANCE = new VisibilityCache();

    public static VisibilityCache get() {
        return INSTANCE;
    }

    static final double CAMERA_EPSILON_SQ = 1.0e-6;
    static final int MAX_ENTRIES = 4096;

    private final DerivedCache<Integer, Entry> entries = new DerivedCache<>(MAX_ENTRIES);
    private Frustum frustum;
    private Vec3d cameraPos = Vec3d.ZERO;
    private int frame;
    private int testsThisFrame;
    private volatile boolean active;

    private VisibilityCache() {
    }

    public void beginFrame(Frustum frustum, Vec3d camera) {
        this.frustum = frustum;
        this.cameraPos = camera == null ? Vec3d.ZERO : camera;
        this.frame++;
        this.testsThisFrame = 0;
        this.active = frustum != null;
    }

    public boolean isActive() {
        return active;
    }

    public int cachedEntries() {
        return entries.size();
    }

    /**
     * Single authoritative skip decision. One cache lookup per call; all
     * verdict/test/reuse accounting happens here.
     */
    public boolean shouldSkip(
            int entityId,
            Box box,
            String rendererClassName,
            boolean rendererTrusted,
            AntiEntityLagConfig cfg) {
        EntityProfiler prof = EntityProfiler.get();
        Entry entry = entries.get(entityId);
        boolean boundsDirty = entry == null || !entry.sameBox(box) || rendererChanged(entry, rendererClassName);
        double cameraMovedSq = entry == null ? Double.POSITIVE_INFINITY : entry.cameraMovedSq(cameraPos);
        int age = entry == null ? Integer.MAX_VALUE : frame - entry.frame;

        VisibilityPolicy.Verdict verdict;
        if (entry != null
                && entry.verdict != VisibilityPolicy.Verdict.UNKNOWN
                && VisibilityPolicy.shouldReuse(
                        age, cameraMovedSq, boundsDirty,
                        cfg.visibilityMaxReuseFrames, CAMERA_EPSILON_SQ)) {
            prof.noteVisibilityHit();
            verdict = entry.verdict;
        } else {
            prof.noteVisibilityMiss();
            if (!active || testsThisFrame >= cfg.visibilityTestsPerFrame) {
                verdict = entry == null ? VisibilityPolicy.Verdict.UNKNOWN : entry.verdict;
            } else {
                testsThisFrame++;
                prof.noteVisibilityTest();
                verdict = testFrustum(box);
                entries.put(entityId, new Entry(verdict, frame, box, cameraPos, rendererClassName));
                entry = entries.get(entityId);
            }
        }

        boolean fresh = entry != null && (frame - entry.frame) <= cfg.visibilityMaxReuseFrames;
        boolean dirty = entry == null || !entry.sameBox(box);
        return VisibilityPolicy.shouldSkipRender(verdict, fresh, dirty, cfg.compatStrict, rendererTrusted);
    }

    public void onEntityRemoved(int entityId) {
        entries.invalidate(entityId);
    }

    public void onWorldChanged() {
        entries.invalidateAll();
        frustum = null;
        active = false;
        testsThisFrame = 0;
    }

    private VisibilityPolicy.Verdict testFrustum(Box box) {
        try {
            if (frustum == null || box == null) {
                return VisibilityPolicy.Verdict.UNKNOWN;
            }
            return frustum.isVisible(box) ? VisibilityPolicy.Verdict.INSIDE : VisibilityPolicy.Verdict.OUTSIDE;
        } catch (Throwable t) {
            return VisibilityPolicy.Verdict.UNKNOWN;
        }
    }

    private static boolean rendererChanged(Entry entry, String rendererClassName) {
        if (entry.rendererClass == null) {
            return rendererClassName != null;
        }
        return !entry.rendererClass.equals(rendererClassName);
    }

    private static final class Entry {
        final VisibilityPolicy.Verdict verdict;
        final int frame;
        final double minX;
        final double minY;
        final double minZ;
        final double maxX;
        final double maxY;
        final double maxZ;
        final double camX;
        final double camY;
        final double camZ;
        final String rendererClass;

        Entry(
                VisibilityPolicy.Verdict verdict,
                int frame,
                Box box,
                Vec3d camera,
                String rendererClass) {
            this.verdict = verdict;
            this.frame = frame;
            this.minX = box.minX;
            this.minY = box.minY;
            this.minZ = box.minZ;
            this.maxX = box.maxX;
            this.maxY = box.maxY;
            this.maxZ = box.maxZ;
            this.camX = camera.x;
            this.camY = camera.y;
            this.camZ = camera.z;
            this.rendererClass = rendererClass;
        }

        boolean sameBox(Box box) {
            return minX == box.minX
                    && minY == box.minY
                    && minZ == box.minZ
                    && maxX == box.maxX
                    && maxY == box.maxY
                    && maxZ == box.maxZ;
        }

        double cameraMovedSq(Vec3d camera) {
            double dx = camX - camera.x;
            double dy = camY - camera.y;
            double dz = camZ - camera.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
