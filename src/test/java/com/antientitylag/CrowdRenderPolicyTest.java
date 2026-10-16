package com.antientitylag;

import com.antientitylag.render.CrowdRenderPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrowdRenderPolicyTest {
    @Test
    void nameTagPolicy() {
        // Disabled: always render (vanilla fidelity preserved).
        assertTrue(CrowdRenderPolicy.shouldRenderNameTag(1_000_000.0, 2304.0, false, false));
        // Enabled: within distance renders, beyond culls, targets always render.
        assertTrue(CrowdRenderPolicy.shouldRenderNameTag(2304.0, 2304.0, false, true));
        assertFalse(CrowdRenderPolicy.shouldRenderNameTag(2305.0, 2304.0, false, true));
        assertTrue(CrowdRenderPolicy.shouldRenderNameTag(1_000_000.0, 2304.0, true, true));
    }

    @Test
    void shadowFalloff() {
        assertEquals(0.8f, CrowdRenderPolicy.shadowOpacity(100.0, 256.0, 2304.0, 0.8f, true), 1e-6f);
        assertEquals(0.0f, CrowdRenderPolicy.shadowOpacity(5000.0, 256.0, 2304.0, 0.8f, true), 1e-6f);
        float mid = CrowdRenderPolicy.shadowOpacity(1280.0, 256.0, 2304.0, 0.8f, true);
        assertTrue(mid > 0.0f && mid < 0.8f);
        // Disabled: base untouched at any distance.
        assertEquals(0.8f, CrowdRenderPolicy.shadowOpacity(5000.0, 256.0, 2304.0, 0.8f, false), 1e-6f);
    }
}
