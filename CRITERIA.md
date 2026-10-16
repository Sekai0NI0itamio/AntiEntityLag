# CRITERIA — AntiEntityLag 1.0.0 (Fabric 1.21.1)

Evidence rule: a box is checked only with named evidence (CI run URL, command
output, in-game observation). Never from memory.

## Build & repo (cloud-only)

- [ ] Folder `AntiEntityLag/` exists in the Mods workspace.
- [ ] Public repo `AntiEntityLag` exists under the authenticated account.
- [ ] `gradle build` passes on GitHub Actions (Temurin Java 21, Gradle 8.10.2);
      jar attached as artifact. Evidence: Actions run URL.
- [ ] All unit tests pass in CI (`test` task, JUnit 5). Evidence: Actions log.
- [ ] NEVER built locally — only via `.github/workflows/build.yml`.

## Correctness (must all hold)

- [ ] Mod loads in a clean 1.21.1 + Fabric Loader + Fabric API client.
- [ ] Works with no optional compatibility mods installed.
- [ ] `/ael enabled false` restores baseline behavior (zero skips; overlay off
      path shows `skipped=0` growth stops).
- [ ] Entity removal cleans cached state (`/ael status` population tracks;
      no growth of `cachedEntries` across spawn/despawn cycles).
- [ ] World change / disconnect invalidates world-specific state (no stale
      verdicts; overlay shows sane values after rejoin).
- [ ] No packet dropping/reordering/merging anywhere in the codebase.
- [ ] No background thread touches world/render state (only immutable JSON
      strings cross to the export worker).
- [ ] Uncertain visibility always renders (`UNKNOWN`/stale/dirty/untrusted ⇒
      no skip — covered by `VisibilityPolicyTest` + `RenderGate` fail-safe).

## Subsystems (spec A–H)

- [ ] A. Profiler: overlay + `/ael export` JSON with frames, percentiles,
      population, packets-adjacent counters, hit-rate, queue depth.
- [ ] B. Update optimization: sampled tick timing + bounded derived cache with
      invalidation (unit-tested hit-rate/eviction).
- [ ] C. Network: spawn/remove accounting; documented decision that no packet
      pipeline hook ships in v1 (risk > benefit — see ARCHITECTURE.md).
- [ ] D. Visibility: frustum-verdict reuse with invalidation on camera/box/
      world change; zero false culling in supported tests.
- [ ] E. Crowd rendering: distance/hysteresis policy specified + unit-tested;
      renderer-internal hooks deferred with reason (see ARCHITECTURE.md).
- [ ] F. Modded entities: generic interfaces + strict-mode fallback
      (unit-tested trust policy).
- [ ] G. Memory/concurrency: bounded caches/queues, single export worker,
      drop-oldest counted; no thread-per-entity; no render-thread waits.
- [ ] H. Adaptive: EMA + hysteresis levels (unit-tested climb/dwell/descend,
      spike immunity).

## Benchmarks & docs

- [ ] `BENCHMARKS.md` protocol complete (counts, workloads, metrics, trials,
      environment record).
- [ ] Baseline-vs-optimized comparison uses identical scenes/settings; no
      fabricated numbers anywhere.
- [ ] `docs/ARCHITECTURE.md` maps every spec subsystem, mixin contract, and
      deferred item with its reason.
