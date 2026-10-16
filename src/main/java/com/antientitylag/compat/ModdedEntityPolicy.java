package com.antientitylag.compat;

import java.util.Collections;
import java.util.Set;

/**
 * Decides whether an entity renderer's bounding box can be trusted for
 * visibility verdicts.
 *
 * <p>Vanilla renderers live under {@code net.minecraft.client.render.entity}
 * and respect {@code Entity#getBoundingBox()}. Anything else is a modded
 * renderer that may draw outside the entity box (beams, auras, multipart
 * contraptions). Under strict compatibility such renderers are never culled;
 * under lenient mode everything not on the explicit blocklist is trusted.
 * The blocklist is intentionally empty in v1 and is the documented extension
 * point for per-mod adapters.
 */
public final class ModdedEntityPolicy {
    private static final String VANILLA_RENDERER_PKG = "net.minecraft.client.render.entity.";

    static final Set<String> KNOWN_BAD_RENDERERS = Collections.emptySet();

    private ModdedEntityPolicy() {
    }

    public static boolean isVanillaRenderer(String rendererClassName) {
        return rendererClassName != null && rendererClassName.startsWith(VANILLA_RENDERER_PKG);
    }

    public static boolean trustBounds(String rendererClassName, boolean strict) {
        if (strict) {
            return isVanillaRenderer(rendererClassName);
        }
        return rendererClassName != null && !KNOWN_BAD_RENDERERS.contains(rendererClassName);
    }
}
