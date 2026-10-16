# CRITERIA — AntiEntityLag 1.0.0 (Fabric 1.21.1)

Evidence rule: a box is checked only with named evidence (CI run URL, command
output, in-game observation). Never from memory.

## Build & repo (cloud-only)

- [x] Folder `AntiEntityLag/` exists in the Mods workspace.
- [x] Public repo `AntiEntityLag` exists under the authenticated account
      (https://github.com/Sekai0NI0itamio/AntiEntityLag).
- [x] `gradle build` passes on GitHub Actions (Temurin Java 21, Gradle 8.10.2);
      jar attached as artifact. Evidence: run 38883513382
      (https://github.com/Sekai0NI0itamio/AntiEntityLag/actions/runs/38883513382),
      BUILD SUCCESSFUL, `antientitylag-1.0.0.jar` (45 files, both mixins +
      refmap verified in artifact).
- [x] All unit tests pass in CI (`:test` executed, not skipped; 8 test classes:
      FrameTimeStats, TickClock, DerivedCache, VisibilityPolicy,
      CrowdRenderPolicy, AdaptiveController, BenchmarkPlan, ModdedEntityPolicy).
      Evidence: same run, `:test` → BUILD SUCCESSFUL.
- [x] NEVER built locally — only via `.github/workflows/build.yml`
      (workspace has no `build/` output; all compile/test evidence is CI).

## Correctness (must all hold)

> Status: statically enforced where possible (fail-closed gate, strict-mode
> default, bounded caches, no packet code, no world/render access off-thread);
> in-game verification pending a client test run — boxes stay open until then.

- [ ] Mod loads in a clean 1.21.1 + Fabric Loader + Fabric API client.
- [ ] Works with no optional compatibility mods installed.
- [ ] `/ael enabled false` restores baseline behavior (zero skips; overlay off
      path shows `skipped=0` growth stops).
- [ ] Entity removal cleans cached state (`/ael status` population tracks;
      no growth of `cachedEntries` across spawn/despawn cycles).
- [ ] World change / disconnect invalidates world-specific state (no stale
      verdicts; overlay shows sane values after rejoin).
- [x] No packet dropping/reordering/merging anywhere in the codebase
      (no network pipeline code exists; verified by inspection).
- [x] No background thread touches world/render state (only immutable JSON
      strings cross to the export worker — `BackgroundWork` + review).
- [x] Uncertain visibility always renders (`UNKNOWN`/stale/dirty/untrusted ⇒
      no skip — covered by `VisibilityPolicyTest` + `RenderGate` fail-safe).

## Subsystems (spec A–H)

> Status: implemented + unit-tested + CI-compiled. Profiler/overlay/commands
> need the same in-game run as above before behavioral boxes close.

- [x] A. Profiler: overlay + `/ael export` JSON with frames, percentiles,
      population, packets-adjacent counters, hit-rate, queue depth
      (code + `FrameTimeStatsTest`; behavior pending in-game).
- [x] B. Update optimization: sampled tick timing + bounded derived cache with
      invalidation (unit-tested hit-rate/eviction).
- [x] C. Network: spawn/remove accounting; documented decision that no packet
      pipeline hook ships in v1 (risk > benefit — see ARCHITECTURE.md).
- [x] D. Visibility: frustum-verdict reuse with invalidation on camera/box/
      world change (unit-tested policy + fail-closed gate; zero-false-culling
      proof pending in-game).
- [x] E. Crowd rendering: distance/hysteresis policy specified + unit-tested;
      renderer-internal hooks deferred with reason (see ARCHITECTURE.md).
- [x] F. Modded entities: generic interfaces + strict-mode fallback
      (unit-tested trust policy).
- [x] G. Memory/concurrency: bounded caches/queues, single export worker,
      drop-oldest counted; no thread-per-entity; no render-thread waits.
- [x] H. Adaptive: EMA + hysteresis levels (unit-tested climb/dwell/descend,
      spike immunity).

## Benchmarks & docs

- [x] `BENCHMARKS.md` protocol complete (counts, workloads, metrics, trials,
      environment record).
- [x] Baseline-vs-optimized comparison uses identical scenes/settings; no
      fabricated numbers anywhere (`compareMarkdown` lists unmatched scenarios
      instead of hiding them; README claims no numbers).
- [x] `docs/ARCHITECTURE.md` maps every spec subsystem, mixin contract, and
      deferred item with its reason.
