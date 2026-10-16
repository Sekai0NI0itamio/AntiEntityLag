package com.antientitylag.mixin;

import com.antientitylag.profile.TickClock;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WHY: entity tick cost is the first number any optimization must know, and it
 * cannot be measured from Fabric events. Sampling 1-in-64 ticks keeps
 * steady-state overhead to a counter increment plus a predictable branch.
 *
 * <p>TARGET: {@code net.minecraft.entity.Entity#tick()V} — present and stable
 * across the entire 1.21.x series. No behavior is changed; both injections
 * only timestamp.
 *
 * <p>FAIL-SAFE: injections are measurement-only and never cancel, mutate, or
 * reorder anything. If the target ever moved, the mixins config
 * ({@code defaultRequire=1}) fails the game load loudly rather than running
 * half-instrumented — see ARCHITECTURE.md for the per-mixin contract.
 */
@Mixin(Entity.class)
public class EntityTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void antientitylag$tickStart(CallbackInfo ci) {
        TickClock.begin();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void antientitylag$tickEnd(CallbackInfo ci) {
        TickClock.end();
    }
}
