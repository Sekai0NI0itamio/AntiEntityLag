package com.antientitylag.command;

import com.antientitylag.AntiEntityLagMod;
import com.antientitylag.bench.BenchmarkPlan;
import com.antientitylag.client.BackgroundWork;
import com.antientitylag.client.RenderGate;
import com.antientitylag.config.AntiEntityLagConfig;
import com.antientitylag.profile.EntityProfiler;
import com.antientitylag.profile.TickClock;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client commands: {@code /ael status|overlay|enabled|export|bench}.
 * All client-side; no server installation or permission needed.
 */
public final class AelCommands {
    private AelCommands() {
    }

    public static void register(ClientCommandDispatcher dispatcher) {
        dispatcher.register(ClientCommandManager.literal("ael")
                .then(ClientCommandManager.literal("status")
                        .executes(ctx -> status(ctx.getSource())))
                .then(ClientCommandManager.literal("overlay")
                        .executes(ctx -> toggleOverlay(ctx.getSource())))
                .then(ClientCommandManager.literal("enabled")
                        .then(ClientCommandManager.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setEnabled(
                                        ctx.getSource(), BoolArgumentType.getBool(ctx, "value")))))
                .then(ClientCommandManager.literal("export")
                        .executes(ctx -> export(ctx.getSource())))
                .then(ClientCommandManager.literal("bench")
                        .executes(ctx -> bench(ctx.getSource()))));
    }

    private static int status(ClientCommandSource source) {
        AntiEntityLagConfig cfg = AntiEntityLagConfig.get();
        EntityProfiler.Snapshot snapshot = EntityProfiler.get().snapshot();
        send(source, "master=" + cfg.masterEnabled
                + " visibility=" + cfg.visibilityEnabled
                + " strict=" + cfg.compatStrict);
        send(source, "entities=" + snapshot.entities()
                + " players=" + snapshot.players()
                + " renderCalls=" + snapshot.renderCalls()
                + " skipped=" + snapshot.skippedRenders());
        send(source, String.format("avg=%.2fms p50=%.2f p95=%.2f p99=%.2f",
                snapshot.avgMs(), snapshot.p50Ms(), snapshot.p95Ms(), snapshot.p99Ms()));
        send(source, String.format("visHit=%.1f%% visTests=%d adaptive=%d gateArmed=%b",
                snapshot.visHitRate() * 100.0,
                snapshot.visTests(),
                AntiEntityLagMod.ADAPTIVE.level(),
                RenderGate.isArmed()));
        return 1;
    }

    private static int toggleOverlay(ClientCommandSource source) {
        AntiEntityLagConfig cfg = AntiEntityLagConfig.get();
        cfg.diagnosticsOverlay = !cfg.diagnosticsOverlay;
        AntiEntityLagConfig.save();
        send(source, "diagnostics overlay " + (cfg.diagnosticsOverlay ? "ON" : "OFF"));
        return 1;
    }

    private static int setEnabled(ClientCommandSource source, boolean value) {
        AntiEntityLagConfig cfg = AntiEntityLagConfig.get();
        cfg.masterEnabled = value;
        TickClock.setArmed(value);
        AntiEntityLagConfig.save();
        send(source, "AntiEntityLag " + (value ? "ENABLED" : "DISABLED (baseline behavior)"));
        return 1;
    }

    private static int export(ClientCommandSource source) {
        String json = EntityProfiler.get().exportJson();
        Path path = FabricLoader.getInstance().getConfigDir()
                .resolve("antientitylag-profile-" + System.currentTimeMillis() + ".json");
        boolean queued = BackgroundWork.submitOrRunInline(() -> {
            try {
                Files.writeString(path, json);
            } catch (Exception ignored) {
                // Export is best-effort diagnostics; never disturb gameplay.
            }
        });
        send(source, "profile " + (queued ? "export queued" : "exported inline")
                + ": " + path.getFileName());
        return 1;
    }

    private static int bench(ClientCommandSource source) {
        int scenarios = BenchmarkPlan.standardPlan().size();
        send(source, "benchmark plan: " + scenarios + " scenarios (see BENCHMARKS.md).");
        send(source, "compare with: /ael export, toggle /ael enabled, /ael export again.");
        return 1;
    }

    private static void send(ClientCommandSource source, String message) {
        source.sendFeedback(Text.literal("[AntiEntityLag] " + message));
    }
}
