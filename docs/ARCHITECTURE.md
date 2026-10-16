# Architecture — AntiEntityLag 1.0.0

Spec-subsystem map (§4 A–H), mixin contracts (§9), threading (§5), and every
investigated-but-deferred item with its reason.

## A. Profiler — `profile/`

`EntityProfiler` (counters + `FrameTimeStats` ring) fed by render-loop events
(`WorldRenderEvents.START/END`), entity load/unload events, `TickClock`
samples, and render-gate counters. Recording paths are allocation-free;
percentiles/JSON run only on overlay refresh or `/ael export`. Exported JSON
is built by hand (no dependency) and written off-thread.

## B. Update optimization — `update/` + `profile/TickClock`

`TickClock` samples 1-in-64 `Entity#tick` executions with a re-entrancy guard.
`DerivedCache` is the bounded, explicitly-invalidated cache primitive used for
derived per-entity data (visibility entries today; future derived render data
must benchmark lookup+invalidation+memory against recomputation before use).

## C. Network

Spawns/removals are counted via `ClientEntityEvents`. No Netty/packet hook
ships in v1: timing packets would require a pipeline mixin whose failure mode
is silent protocol corruption, and spawn/remove + tick coverage already
captures the client-side effect. If profiling shows packet *handling* (not
server tick/latency) dominating, the hook candidate is documented here for a
future version with in-game validation. Server TPS and latency are reported,
never "fixed".

## D. Visibility — `visibility/` + `client/RenderGate`

`VisibilityCache` reuses vanilla frustum verdicts; `RenderGate.onRender` is
the single consumer, called from the dispatcher mixin. Skip requires ALL of:
fresh `OUTSIDE`, clean box, same renderer class, active cache, and (strict
mode) vanilla renderer. Anything else renders normally. Budget
(`visibilityTestsPerFrame`) bounds fresh tests; overflow reuses or renders.

## E. Crowd rendering

Distance/hysteresis policy is specified and unit-tested in
`render/CrowdRenderPolicy` (name-tag cull distance with target exemption,
shadow linear falloff). Renderer-internal hooks (name-tag/shadow suppression)
are **deferred**: the cloud-only pipeline cannot runtime-verify those
injection points, and an unverified visual hook risks exactly the popping and
false-culling the spec forbids. Landing criteria: in-game verification +
mixin `require=1` + conflict tests with Sodium/shaders.

## F. Modded entities — `compat/ModdedEntityPolicy`

Generic `Entity`/renderer handling everywhere; no vanilla class list. Strict
mode trusts only `net.minecraft.client.render.entity.*`; the (currently empty)
blocklist is the extension point for per-mod adapters.

## G. Memory & concurrency — `client/BackgroundWork`

One daemon worker, queue cap 4, drop-oldest counted. Only immutable JSON
strings cross threads. All caches bounded with eviction. No thread-per-entity,
no render-thread waits, synchronous inline fallback for exports.

## H. Adaptive — `adaptive/AdaptiveController`

EMA frame time, levels 0–3, multi-frame confirmation both directions plus
post-change cooldown. Currently reports the level (overlay/status); automatic
quality scaling of optional work keys off it in the next iteration once the
per-level savings are measured.

## Mixin contracts (§9)

| Mixin | Target | Inject | Require | Why | Fallback |
|---|---|---|---|---|---|
| `EntityTickMixin` | `Entity#tick()V` HEAD+TAIL | measurement only | 1 (config `defaultRequire`) | Stable across 1.21.x; sampling needs the hottest method | Loud load failure rather than silent half-instrumentation |
| `EntityRenderDispatcherMixin` | `EntityRenderDispatcher#render(Entity,double×3,float×2,MatrixStack,VertexConsumerProvider,int)` HEAD | cancellable gate | **0** | Descriptor is version-sensitive | Injection absent ⇒ vanilla behavior; gate logs via `/ael status` (`gateArmed`) |

`RenderGate` additionally self-disarms on any unexpected throwable.

## Threading classification (§5)

1. Authoritative mutation (ticks, packets, world): main thread only, never moved.
2. Read-only inspection (frustum snapshot, boxes): main thread, zero-copy refs.
3. Derived calculation (verdict reuse math): main thread, budgeted.
4. Render submission (skip decision): main thread, fail-closed.
5. Optional cosmetic processing (file export): the single bounded worker.
   Async visibility computation was evaluated and rejected for v1: per-test
   cost (~100ns) is below snapshot+queue+validate cost. Revisit with numbers.

## Deferred with reason (not dropped)

- Renderer-internal name-tag/shadow hooks (needs in-game verification).
- Async visibility worker (needs measurement showing end-to-end win).
- Packet-timing instrumentation (risk > benefit, see §C).
- 1.21.x versions beyond 1.21.1 (require inspection + testing, never assumed).
