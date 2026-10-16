package com.antientitylag.bench;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repeatable benchmark protocol.
 *
 * <p>Counts sweep 0/100/250/500/1000/2000. The three representative workloads
 * run at every count; the remaining workloads run at 500 entities where their
 * cost is most visible; SOAK is a sustained 500-entity run that exposes
 * allocation/GC drift. Simulated workloads stand in for crowded scenes in CI;
 * in-game runs against a real server use the same scenario table so results
 * stay comparable. No benchmark numbers are reported without naming the
 * machine, settings, and trial count (see BENCHMARKS.md).
 */
public final class BenchmarkPlan {
    public enum Workload {
        STATIONARY,
        CROWD_MOVE,
        ROTATION,
        EQUIPMENT_CHURN,
        CAMERA_SWEEP,
        MIXED,
        MODDED,
        HEAVY_LAYERS,
        SOAK
    }

    public record Scenario(int entityCount, Workload workload) {
    }

    public record TrialResult(
            Scenario scenario,
            int frames,
            double avgMs,
            double p50Ms,
            double p95Ms,
            double p99Ms,
            double meanTickNs,
            String notes) {
    }

    private static final int[] SWEEP_COUNTS = {0, 100, 250, 500, 1000, 2000};
    private static final Workload[] SWEEP_WORKLOADS = {
            Workload.STATIONARY, Workload.CROWD_MOVE, Workload.MIXED};

    private BenchmarkPlan() {
    }

    public static List<Scenario> standardPlan() {
        List<Scenario> plan = new ArrayList<>();
        for (int count : SWEEP_COUNTS) {
            for (Workload workload : SWEEP_WORKLOADS) {
                plan.add(new Scenario(count, workload));
            }
        }
        for (Workload workload : Workload.values()) {
            boolean inSweep = false;
            for (Workload swept : SWEEP_WORKLOADS) {
                if (swept == workload) {
                    inSweep = true;
                    break;
                }
            }
            if (!inSweep) {
                plan.add(new Scenario(500, workload));
            }
        }
        return plan;
    }

    private static String key(Scenario scenario) {
        return scenario.entityCount() + "/" + scenario.workload();
    }

    /**
     * Renders a Markdown comparison table. Positive {@code fpsDeltaPct} means
     * the optimized run is faster. Only scenarios present in both lists are
     * compared; the rest are reported as unmatched so gaps are visible.
     */
    public static String compareMarkdown(
            String baselineLabel,
            String optimizedLabel,
            List<TrialResult> baseline,
            List<TrialResult> optimized) {
        Map<String, TrialResult> baseByKey = new LinkedHashMap<>();
        for (TrialResult result : baseline) {
            baseByKey.put(key(result.scenario()), result);
        }
        Map<String, TrialResult> optByKey = new LinkedHashMap<>();
        for (TrialResult result : optimized) {
            optByKey.put(key(result.scenario()), result);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("| Entities | Workload | ").append(baselineLabel).append(" avg/p95 (ms) | ")
                .append(optimizedLabel).append(" avg/p95 (ms) | Δ avg | Δ p95 |\n");
        sb.append("|---|---|---|---|---|---|---|\n");

        List<String> unmatched = new ArrayList<>();
        for (Map.Entry<String, TrialResult> entry : optByKey.entrySet()) {
            TrialResult opt = entry.getValue();
            TrialResult base = baseByKey.get(entry.getKey());
            if (base == null) {
                unmatched.add(entry.getKey() + " (optimized only)");
                continue;
            }
            double avgDelta = pctChange(base.avgMs(), opt.avgMs());
            double p95Delta = pctChange(base.p95Ms(), opt.p95Ms());
            sb.append('|').append(opt.scenario().entityCount())
                    .append('|').append(opt.scenario().workload())
                    .append('|').append(fmt(base.avgMs())).append(" / ").append(fmt(base.p95Ms()))
                    .append('|').append(fmt(opt.avgMs())).append(" / ").append(fmt(opt.p95Ms()))
                    .append('|').append(fmtSigned(avgDelta)).append("%")
                    .append('|').append(fmtSigned(p95Delta)).append("%|\n");
        }
        for (Map.Entry<String, TrialResult> entry : baseByKey.entrySet()) {
            if (!optByKey.containsKey(entry.getKey())) {
                unmatched.add(entry.getKey() + " (baseline only)");
            }
        }
        if (!unmatched.isEmpty()) {
            sb.append("\nUnmatched scenarios (no comparison possible):\n");
            for (String u : unmatched) {
                sb.append("- ").append(u).append('\n');
            }
        }
        if (baseline.isEmpty() && optimized.isEmpty()) {
            sb.append("\n_no trials recorded_\n");
        }
        return sb.toString();
    }

    private static double pctChange(double base, double opt) {
        if (base <= 0.0) {
            return 0.0;
        }
        return (base - opt) / base * 100.0;
    }

    private static String fmt(double v) {
        return String.format("%.2f", v);
    }

    private static String fmtSigned(double v) {
        return (v >= 0 ? "+" : "") + String.format("%.1f", v);
    }
}
