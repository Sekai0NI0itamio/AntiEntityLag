package com.antientitylag;

import com.antientitylag.adaptive.AdaptiveController;
import com.antientitylag.client.ClientHooks;
import com.antientitylag.config.AntiEntityLagConfig;
import com.antientitylag.profile.TickClock;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entrypoint. Registers event hooks only; all behavior changes are
 * gated behind {@link AntiEntityLagConfig#masterEnabled} so disabling the mod
 * restores baseline behavior without uninstalling.
 */
public class AntiEntityLagMod implements ClientModInitializer {
    public static final String MOD_ID = "antientitylag";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static final AdaptiveController ADAPTIVE = new AdaptiveController();

    @Override
    public void onInitializeClient() {
        AntiEntityLagConfig.load();
        TickClock.setArmed(AntiEntityLagConfig.get().masterEnabled);
        ClientHooks.register();
        AntiEntityLagConfig cfg = AntiEntityLagConfig.get();
        LOG.info("[AntiEntityLag] initialized: master={} visibility={} (reuse={}, budget={}) "
                        + "strict={} adaptive={} overlay={}",
                cfg.masterEnabled,
                cfg.visibilityEnabled,
                cfg.visibilityMaxReuseFrames,
                cfg.visibilityTestsPerFrame,
                cfg.compatStrict,
                cfg.adaptiveEnabled,
                cfg.diagnosticsOverlay);
    }
}
