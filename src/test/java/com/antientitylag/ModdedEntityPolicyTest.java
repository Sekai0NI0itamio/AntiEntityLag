package com.antientitylag;

import com.antientitylag.compat.ModdedEntityPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModdedEntityPolicyTest {
    @Test
    void vanillaRenderersTrustedInStrictMode() {
        assertTrue(ModdedEntityPolicy.trustBounds(
                "net.minecraft.client.render.entity.PlayerEntityRenderer", true));
    }

    @Test
    void unknownRenderersDistrustedInStrictMode() {
        assertFalse(ModdedEntityPolicy.trustBounds("com.example.CoolDragonRenderer", true));
        assertFalse(ModdedEntityPolicy.trustBounds(null, true));
    }

    @Test
    void lenientModeTrustsAllKnownRenderers() {
        assertTrue(ModdedEntityPolicy.trustBounds("com.example.CoolDragonRenderer", false));
        assertFalse(ModdedEntityPolicy.trustBounds(null, false));
    }
}
