package com.antientitylag.profile;

/**
 * Central lightweight diagnostic aggregator. All record methods are plain
 * counter/array stores designed to cost single-digit nanoseconds; expensive
 * work (percentiles, JSON) happens only in {@link #snapshot()} and
 * {@link #exportJson()}, which run on overlay refresh or explicit export.
 */
public final class EntityProfiler {
    private static final EntityProfiler INSTANCE = new EntityProfiler();

    public static EntityProfiler get() {
        return INSTANCE;
    }

    private final FrameTimeStats frames = new FrameTimeStats(600);
    private long frameStartNanos;
    private boolean frameOpen;
    private double lastFrameMs;

    private int loadedEntities;
    private int playerCount;
    private long spawns;
    private long despawns;
    private long renderCalls;
    private long skippedRenders;
    private long visHits;
    private long visMisses;
    private long visTests;
    private long bgQueueDepth;
    private long droppedBg;
    private double lastMeanEntityTickNanos = -1.0;

    private EntityProfiler() {
    }

    public void beginFrame() {
        frameStartNanos = System.nanoTime();
        frameOpen = true;
    }

    public void endFrame() {
        if (!frameOpen) {
            return;
        }
        frameOpen = false;
        double frameMs = (System.nanoTime() - frameStartNanos) / 1_000_000.0;
        lastFrameMs = frameMs;
        frames.record(System.nanoTime() - frameStartNanos);
        lastMeanEntityTickNanos = TickClock.drainMeanNanos();
    }

    /** Most recent render-frame duration in milliseconds (0 before first frame). */
    public double lastFrameMs() {
        return lastFrameMs;
    }

    public void setPopulation(int entities, int players) {
        this.loadedEntities = entities;
        this.playerCount = players;
    }

    public void noteSpawn() {
        spawns++;
    }

    public void noteDespawn() {
        despawns++;
    }

    public void recordRenderCall() {
        renderCalls++;
    }

    public void noteSkippedRender() {
        skippedRenders++;
    }

    public void noteVisibilityHit() {
        visHits++;
    }

    public void noteVisibilityMiss() {
        visMisses++;
    }

    public void noteVisibilityTest() {
        visTests++;
    }

    public void setBackgroundStats(long queueDepth, long dropped) {
        this.bgQueueDepth = queueDepth;
        this.droppedBg = dropped;
    }

    public Snapshot snapshot() {
        double hitRate = (visHits + visMisses) == 0
                ? 0.0
                : (double) visHits / (double) (visHits + visMisses);
        return new Snapshot(
                System.currentTimeMillis(),
                frames.count(),
                frames.averageMs(),
                frames.percentileMs(50.0),
                frames.percentileMs(95.0),
                frames.percentileMs(99.0),
                loadedEntities,
                playerCount,
                spawns,
                despawns,
                renderCalls,
                skippedRenders,
                visHits,
                visMisses,
                visTests,
                hitRate,
                lastMeanEntityTickNanos,
                bgQueueDepth,
                droppedBg);
    }

    public String exportJson() {
        return snapshot().toJson();
    }

    public void reset() {
        frames.reset();
        frameOpen = false;
        lastFrameMs = 0.0;
        loadedEntities = 0;
        playerCount = 0;
        spawns = 0;
        despawns = 0;
        renderCalls = 0;
        skippedRenders = 0;
        visHits = 0;
        visMisses = 0;
        visTests = 0;
        bgQueueDepth = 0;
        droppedBg = 0;
        lastMeanEntityTickNanos = -1.0;
    }

    /** Immutable point-in-time view; JSON is hand-built to avoid any dependency. */
    public record Snapshot(
            long timestampMillis,
            int frameSamples,
            double avgMs,
            double p50Ms,
            double p95Ms,
            double p99Ms,
            int entities,
            int players,
            long spawns,
            long despawns,
            long renderCalls,
            long skippedRenders,
            long visHits,
            long visMisses,
            long visTests,
            double visHitRate,
            double meanEntityTickNs,
            long bgQueueDepth,
            long droppedBg) {

        public String toJson() {
            StringBuilder sb = new StringBuilder(512);
            sb.append('{');
            field(sb, "timestampMillis", timestampMillis).append(',');
            field(sb, "frameSamples", frameSamples).append(',');
            field(sb, "avgMs", avgMs).append(',');
            field(sb, "p50Ms", p50Ms).append(',');
            field(sb, "p95Ms", p95Ms).append(',');
            field(sb, "p99Ms", p99Ms).append(',');
            field(sb, "entities", entities).append(',');
            field(sb, "players", players).append(',');
            field(sb, "spawns", spawns).append(',');
            field(sb, "despawns", despawns).append(',');
            field(sb, "renderCalls", renderCalls).append(',');
            field(sb, "skippedRenders", skippedRenders).append(',');
            field(sb, "visHits", visHits).append(',');
            field(sb, "visMisses", visMisses).append(',');
            field(sb, "visTests", visTests).append(',');
            field(sb, "visHitRate", visHitRate).append(',');
            field(sb, "meanEntityTickNs", meanEntityTickNs).append(',');
            field(sb, "bgQueueDepth", bgQueueDepth).append(',');
            field(sb, "droppedBg", droppedBg);
            sb.append('}');
            return sb.toString();
        }

        private static StringBuilder field(StringBuilder sb, String name, long v) {
            return sb.append('"').append(name).append("\":").append(v);
        }

        private static StringBuilder field(StringBuilder sb, String name, double v) {
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                return sb.append('"').append(name).append("\":0");
            }
            return sb.append('"').append(name).append("\":").append(v);
        }
    }
}
