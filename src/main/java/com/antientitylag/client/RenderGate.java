package com.antientitylag.client;

import com.antientitylag.compat.ModdedEntityPolicy;
import com.antientitylag.config.AntiEntityLagConfig;
import com.antientitylag.profile.EntityProfiler;
import com.antientitylag.visibility.VisibilityCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

/**
 * Render-thread gate consulted by {@code EntityRenderDispatcherMixin}.
 *
 * <p>Fail-safe by construction: any error, missing config state, inactive
 * cache, or uncertain verdict returns {@code false} (render normally) and, on
 * unexpected throwables, permanently disarms the gate for the session so
 * rendering can never be broken by this mod. Disabling the mod restores
 * baseline behavior immediately.
 */
public final class RenderGate {
    private static volatile boolean armed = true;

    private RenderGate() {
    }

    /**
     * Returns true when this entity's render submission should be skipped.
     * Only called from the render thread.
     */
    public static boolean onRender(Entity entity) {
        EntityProfiler.get().recordRenderCall();
        if (!armed) {
            return false;
        }
        try {
            AntiEntityLagConfig cfg = AntiEntityLagConfig.get();
            if (!cfg.masterEnabled || !cfg.visibilityEnabled) {
                return false;
            }
            if (entity == null) {
                return false;
            }
            String rendererClass = rendererClassName(entity);
            boolean trusted = ModdedEntityPolicy.trustBounds(rendererClass, cfg.compatStrict);
            boolean skip = VisibilityCache.get()
                    .shouldSkip(entity.getId(), entity.getBoundingBox(), rendererClass, trusted, cfg);
            if (skip) {
                EntityProfiler.get().noteSkippedRender();
            }
            return skip;
        } catch (Throwable t) {
            armed = false;
            return false;
        }
    }

    private static String rendererClassName(Entity entity) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.getEntityRenderDispatcher() == null) {
                return null;
            }
            Object renderer = client.getEntityRenderDispatcher().getRenderer(entity);
            return renderer == null ? null : renderer.getClass().getName();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isArmed() {
        return armed;
    }
}
