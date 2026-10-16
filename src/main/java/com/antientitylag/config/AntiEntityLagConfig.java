package com.antientitylag.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small configuration surface. Defaults preserve vanilla visuals and
 * correctness; every optimization beyond safe visibility caching is opt-in.
 */
public final class AntiEntityLagConfig {
    private static final Logger LOG = Logger.getLogger("AntiEntityLag");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AntiEntityLagConfig INSTANCE = new AntiEntityLagConfig();

    public static AntiEntityLagConfig get() {
        return INSTANCE;
    }

    /** Master kill switch. Off restores baseline behavior (mixins idle). */
    public volatile boolean masterEnabled = true;
    /** Adaptive frustum-visibility reuse. Off = vanilla culling only. */
    public volatile boolean visibilityEnabled = true;
    /** How many frames a visibility verdict may be reused (stationary entities). */
    public volatile int visibilityMaxReuseFrames = 4;
    /** Per-frame budget of fresh frustum tests; overflow reuses last verdict. */
    public volatile int visibilityTestsPerFrame = 512;
    /** Strict mode: only vanilla renderers are ever visibility-skipped. */
    public volatile boolean compatStrict = true;
    /** Optional diagnostic overlay (F3-style, top-left). */
    public volatile boolean diagnosticsOverlay = false;
    /** Adaptive quality controller (hysteresis, EMA-based, never instant FPS). */
    public volatile boolean adaptiveEnabled = true;

    private AntiEntityLagConfig() {
    }

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("antientitylag.json");
    }

    public static void load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            save();
            return;
        }
        try {
            String json = Files.readString(path);
            AntiEntityLagConfig loaded = GSON.fromJson(json, AntiEntityLagConfig.class);
            if (loaded != null) {
                INSTANCE.masterEnabled = loaded.masterEnabled;
                INSTANCE.visibilityEnabled = loaded.visibilityEnabled;
                INSTANCE.visibilityMaxReuseFrames = clamp(loaded.visibilityMaxReuseFrames, 0, 20);
                INSTANCE.visibilityTestsPerFrame = clamp(loaded.visibilityTestsPerFrame, 16, 8192);
                INSTANCE.compatStrict = loaded.compatStrict;
                INSTANCE.diagnosticsOverlay = loaded.diagnosticsOverlay;
                INSTANCE.adaptiveEnabled = loaded.adaptiveEnabled;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[AntiEntityLag] Bad config, keeping defaults: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "[AntiEntityLag] Could not save config: " + e.getMessage());
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.min(max, Math.max(min, v));
    }
}
