# AntiEntityLag — Zero Entity Lag (Fabric 1.21.1, client-side)

A serious performance-engineering project: a client-side Fabric mod that pushes
entity-related CPU, frame-time, and allocation overhead as close to the
hardware and engine limits as correctness allows.

**Target:** Minecraft Java 1.21.1 · Fabric Loader ≥ 0.16.14 · Java 21 ·
client-side only, no server installation.

> **Cloud-only builds.** This mod is NEVER built, compiled, or tested locally.
> Every build, test run, and check happens in GitHub Actions
> (`.github/workflows/build.yml`). See `CRITERIA.md`.

## What it does

| Subsystem | Behavior |
|---|---|
| Sampled tick profiler | Measures per-entity tick cost at 1-in-64 sampling (~1ns steady-state). No behavior change. |
| Adaptive visibility cache | Reuses fresh frustum verdicts for stationary entities; budgeted fresh tests per frame; uncertain ⇒ render normally. |
| Render gate | Skips render *submission* only for fresh `OUTSIDE` verdicts with clean boxes. Ticks, interpolation, metadata, networking untouched. |
| Modded-entity safety | Strict mode (default): only vanilla renderers are ever skipped. Anything else renders normally. |
| Adaptive controller | EMA frame-time with hysteresis, levels 0–3. No instant-FPS decisions. |
| Diagnostics | Optional overlay (`/ael overlay`), `/ael status`, JSON export (`/ael export`, off-thread write). |
| Benchmark protocol | 24-scenario plan (0–2000 entities × 9 workloads), baseline-vs-optimized Markdown compare. See `BENCHMARKS.md`. |

## Install

1. Install Fabric Loader (≥ 0.16.14) for Minecraft 1.21.1 + Fabric API.
2. Download the jar from GitHub Actions artifacts (or Releases for tagged versions).
3. Drop it in `mods/`. Client-side only.

## Commands (all client-side)

- `/ael status` — population, frame percentiles, visibility hit-rate, adaptive level, gate state.
- `/ael overlay` — toggle the diagnostic overlay.
- `/ael enabled <true|false>` — master switch. `false` restores baseline behavior.
- `/ael export` — write a profiler JSON snapshot to `config/` (off-thread).
- `/ael bench` — show the benchmark plan summary.

## Configuration (`config/antientitylag.json`)

| Key | Default | Meaning |
|---|---|---|
| `masterEnabled` | `true` | Kill switch. Off = vanilla behavior. |
| `visibilityEnabled` | `true` | Visibility-verdict reuse. Off = vanilla culling only. |
| `visibilityMaxReuseFrames` | `4` | Max verdict reuse for stationary entities. |
| `visibilityTestsPerFrame` | `512` | Fresh frustum-test budget per frame. |
| `compatStrict` | `true` | Only vanilla renderers are ever skipped. |
| `diagnosticsOverlay` | `false` | On-screen stats. |
| `adaptiveEnabled` | `true` | Hysteresis-based adaptive levels. |

## Compatibility

- Vanilla entities, players (skins/capes/armor/name tags), mobs, projectiles,
  armor stands, items: fully supported via generic entity/renderer interfaces.
- Modded entities: supported; unknown renderers fall back to normal rendering
  under strict mode (default).
- Works alongside Sodium and other rendering mods: the mod only *skips*
  already-off-screen submissions and never alters geometry, shaders, or world state.
- Minecraft support is claimed per tested version only: **1.21.1** (this release).
  Other 1.21.x releases require inspection + testing first.

## Performance claims policy

No numbers are published without machine, settings, trial count, and scenario
table. There are no benchmark results in this file until measured runs exist —
see `BENCHMARKS.md` for the protocol and how to reproduce.

## Architecture

See `docs/ARCHITECTURE.md` for the subsystem map (spec sections A–H), the mixin
contract table, the threading classification, and investigated-but-deferred
items with reasons.
