package com.antientitylag;

import com.antientitylag.adaptive.AdaptiveController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveControllerTest {
    @Test
    void calmFramesStayAtFullQuality() {
        AdaptiveController controller = new AdaptiveController();
        for (int i = 0; i < 500; i++) {
            controller.onFrame(16.6);
        }
        assertEquals(0, controller.level());
    }

    @Test
    void sustainedPressureClimbsAndRecoveryDescends() {
        AdaptiveController controller = new AdaptiveController();
        for (int i = 0; i < 500; i++) {
            controller.onFrame(45.0);
        }
        assertEquals(AdaptiveController.MAX_LEVEL, controller.level());

        for (int i = 0; i < 2000; i++) {
            controller.onFrame(10.0);
        }
        assertEquals(0, controller.level());
    }

    @Test
    void singleSpikeDoesNotJumpLevels() {
        AdaptiveController controller = new AdaptiveController();
        for (int i = 0; i < 60; i++) {
            controller.onFrame(16.6);
        }
        controller.onFrame(120.0);
        for (int i = 0; i < 60; i++) {
            controller.onFrame(16.6);
        }
        assertEquals(0, controller.level());
    }

    @Test
    void emaTracksInput() {
        AdaptiveController controller = new AdaptiveController();
        for (int i = 0; i < 300; i++) {
            controller.onFrame(20.0);
        }
        assertTrue(Math.abs(controller.emaMs() - 20.0) < 0.5);
    }
}
