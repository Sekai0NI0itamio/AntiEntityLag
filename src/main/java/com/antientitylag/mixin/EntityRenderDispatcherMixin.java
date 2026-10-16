package com.antientitylag.mixin;

import com.antientitylag.client.RenderGate;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WHY: in crowded scenes the render dispatcher submits hundreds of entities
 * whose frustum verdict was already computed this frame. Consulting the
 * visibility cache here skips the entire per-entity render setup for entities
 * proven off-screen — while every uncertain case renders exactly as vanilla.
 *
 * <p>TARGET: {@code EntityRenderDispatcher#render(Entity, double, double,
 * double, float, float, MatrixStack, VertexConsumerProvider, int)}. This
 * descriptor is version-sensitive (render internals changed across 1.21.x),
 * hence {@code require=0}: if a future Minecraft changes the signature, this
 * injection quietly does not apply, the game still boots, the mod logs the
 * fallback, and behavior equals vanilla. A loud crash for an optional
 * render fast-path would violate the fail-safe principle.
 *
 * <p>FAIL-SAFE: {@link RenderGate} returns false on any doubt (no verdict,
 * stale verdict, dirty box, untrusted modded renderer under strict mode,
 * inactive cache, any throwable) and self-disarms on unexpected errors.
 * Ticks, interpolation, metadata, networking, and interaction are untouched —
 * only render <em>submission</em> is ever skipped, never updates.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void antientitylag$gateRender(
            Entity entity,
            double x,
            double y,
            double z,
            float rotationDegrees,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo ci) {
        if (RenderGate.onRender(entity)) {
            ci.cancel();
        }
    }
}
