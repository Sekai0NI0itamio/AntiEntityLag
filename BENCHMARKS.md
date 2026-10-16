# Benchmark protocol — AntiEntityLag

## Scenario table (`BenchmarkPlan.standardPlan()`, 24 scenarios)

Counts: **0 / 100 / 250 / 500 / 1000 / 2000**.

- At every count: `STATIONARY`, `CROWD_MOVE`, `MIXED`.
- At 500 entities: `ROTATION`, `EQUIPMENT_CHURN`, `CAMERA_SWEEP`, `MODDED`,
  `HEAVY_LAYERS`, plus sustained `SOAK` (allocation/GC drift).

Workload definitions:

| Workload | Meaning |
|---|---|
| `STATIONARY` | Entities idle; measures baseline submission cost. |
| `CROWD_MOVE` | All entities moving simultaneously. |
| `ROTATION` | Frequent yaw/pitch/animation changes. |
| `EQUIPMENT_CHURN` | Frequent armor/metadata changes. |
| `CAMERA_SWEEP` | Rapid camera movement through the crowd. |
| `MIXED` | Players + mobs + projectiles + items + armor stands. |
| `MODDED` | Custom entities/renderers (strict-mode fallback path). |
| `HEAVY_LAYERS` | Armor + held items + capes + name tags on everyone. |
| `SOAK` | 10+ minute sustained run; watches memory/GC. |

## Metrics per trial

Average FPS · median frame time · **P95 / P99** frame time · entity update
duration (sampled) · render duration where measurable · GC pauses + allocation
rate where available · memory · mod overhead (skips, hit-rate, queue depth) ·
visual/gameplay regression checklist.

## Method

1. Fix environment: MC version, Java version, CPU/GPU, render distance,
   graphics settings, mod list, entity count, duration. Record all of them.
2. `/ael export` with the mod disabled → baseline trial (≥3 trials).
3. Enable, same scene/settings → optimized trials (≥3).
4. Compare with `BenchmarkPlan.compareMarkdown` (matched scenarios only;
   unmatched scenarios are listed, never silently dropped).
5. Claim nothing without the table. Simulated CI workloads validate the
   *machinery* (percentiles, compare math); FPS claims require in-game runs.

## Current results

No measured in-game results yet — this section fills in after the first
instrumented runs. Simulated/CI numbers are never presented as game
performance.
