package com.antientitylag;

import com.antientitylag.bench.BenchmarkPlan;
import com.antientitylag.bench.BenchmarkPlan.Scenario;
import com.antientitylag.bench.BenchmarkPlan.TrialResult;
import com.antientitylag.bench.BenchmarkPlan.Workload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenchmarkPlanTest {
    @Test
    void planCoversRequiredCountsAndWorkloads() {
        List<Scenario> plan = BenchmarkPlan.standardPlan();
        for (int count : new int[]{0, 100, 250, 500, 1000, 2000}) {
            final int expected = count;
            assertTrue(plan.stream().anyMatch(s -> s.entityCount() == expected),
                    "missing count " + expected);
        }
        for (Workload workload : Workload.values()) {
            assertTrue(plan.stream().anyMatch(s -> s.workload() == workload),
                    "missing workload " + workload);
        }
    }

    @Test
    void comparisonShowsImprovementWithCorrectSign() {
        Scenario scenario = new Scenario(500, Workload.MIXED);
        List<TrialResult> baseline = List.of(
                new TrialResult(scenario, 600, 20.0, 19.0, 30.0, 40.0, 5000.0, ""));
        List<TrialResult> optimized = List.of(
                new TrialResult(scenario, 600, 16.0, 15.0, 24.0, 34.0, 4000.0, ""));
        String markdown = BenchmarkPlan.compareMarkdown("base", "opt", baseline, optimized);
        assertTrue(markdown.contains("500"));
        assertTrue(markdown.contains("MIXED"));
        assertTrue(markdown.contains("+20.0%"));
        assertTrue(markdown.contains("+20.0%"));
    }

    @Test
    void unmatchedScenariosAreReportedNotHidden() {
        Scenario onlyOpt = new Scenario(100, Workload.SOAK);
        String markdown = BenchmarkPlan.compareMarkdown("base", "opt", List.of(),
                List.of(new TrialResult(onlyOpt, 600, 16.0, 15.0, 20.0, 25.0, 1000.0, "")));
        assertTrue(markdown.contains("Unmatched"));
        assertEquals(1, BenchmarkPlan.standardPlan().stream()
                .filter(s -> s.workload() == Workload.SOAK).count());
    }
}
