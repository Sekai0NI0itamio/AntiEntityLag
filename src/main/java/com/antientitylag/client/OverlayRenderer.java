package com.antientitylag.client;

import com.antientitylag.AntiEntityLagMod;
import com.antientitylag.config.AntiEntityLagConfig;
import com.antientitylag.profile.EntityProfiler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/**
 * Optional on-screen diagnostics. Renders a handful of text lines from the
 * latest profiler snapshot; performs no measurement itself.
 */
public final class OverlayRenderer {
    private OverlayRenderer() {
    }

    public static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        AntiEntityLagConfig cfg = AntiEntityLagConfig.get();
        if (!cfg.masterEnabled || !cfg.diagnosticsOverlay) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }
        EntityProfiler.Snapshot snapshot = EntityProfiler.get().snapshot();
        int y = 4;
        y = line(context, client,
                "AEL entities=" + snapshot.entities() + " players=" + snapshot.players(), y);
        y = line(context, client,
                String.format("avg %.2fms p95 %.2fms p99 %.2fms",
                        snapshot.avgMs(), snapshot.p95Ms(), snapshot.p99Ms()),
                y);
        y = line(context, client,
                "skipped=" + snapshot.skippedRenders()
                        + " visHit=" + (int) (snapshot.visHitRate() * 100) + "%"
                        + " lvl=" + AntiEntityLagMod.ADAPTIVE.level(),
                y);
        if (snapshot.meanEntityTickNs() >= 0.0) {
            line(context, client,
                    String.format("entTick %.0fns", snapshot.meanEntityTickNs()), y);
        }
    }

    private static int line(DrawContext context, MinecraftClient client, String text, int y) {
        context.drawText(client.textRenderer, Text.literal(text), 4, y, 0xE8E8E8, true);
        return y + 9;
    }
}
