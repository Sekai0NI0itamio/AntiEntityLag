package com.antientitylag.client;

import com.antientitylag.AntiEntityLagMod;
import com.antientitylag.command.AelCommands;
import com.antientitylag.config.AntiEntityLagConfig;
import com.antientitylag.profile.EntityProfiler;
import com.antientitylag.visibility.VisibilityCache;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

/**
 * Wires every subsystem to Fabric events. All handlers are main-thread,
 * read-only with respect to world state, and allocation-light: the render
 * events only snapshot references (frustum, camera) that the game already
 * computed.
 */
public final class ClientHooks {
    private static int loadedCount;

    private ClientHooks() {
    }

    public static void register() {
        // Frame timing comes from the render loop (FPS-relevant), not ticks.
        WorldRenderEvents.START.register(context -> {
            if (AntiEntityLagConfig.get().masterEnabled) {
                EntityProfiler.get().beginFrame();
            }
        });
        WorldRenderEvents.END.register(context -> {
            if (AntiEntityLagConfig.get().masterEnabled) {
                EntityProfiler.get().endFrame();
            }
        });

        // Fresh frustum + camera snapshot before entities render.
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            AntiEntityLagConfig cfg = AntiEntityLagConfig.get();
            if (!cfg.masterEnabled || !cfg.visibilityEnabled) {
                VisibilityCache.get().beginFrame(null, null);
                return;
            }
            try {
                VisibilityCache.get().beginFrame(context.frustum(), context.camera().getPos());
            } catch (Throwable t) {
                // No valid snapshot: cache goes inactive, everything renders.
                VisibilityCache.get().beginFrame(null, null);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AntiEntityLagConfig cfg = AntiEntityLagConfig.get();
            if (!cfg.masterEnabled) {
                return;
            }
            if (client.world != null) {
                EntityProfiler.get().setPopulation(loadedCount, client.world.getPlayers().size());
            }
            if (cfg.adaptiveEnabled) {
                AntiEntityLagMod.ADAPTIVE.onFrame(EntityProfiler.get().lastFrameMs());
            }
            EntityProfiler.get()
                    .setBackgroundStats(BackgroundWork.queueDepth(), BackgroundWork.droppedCount());
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            VisibilityCache.get().onWorldChanged();
            EntityProfiler.get().reset();
            recount(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            VisibilityCache.get().onWorldChanged();
            EntityProfiler.get().reset();
            loadedCount = 0;
        });

        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            loadedCount++;
            EntityProfiler.get().noteSpawn();
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            loadedCount = Math.max(0, loadedCount - 1);
            VisibilityCache.get().onEntityRemoved(entity.getId());
            EntityProfiler.get().noteDespawn();
        });

        HudRenderCallback.EVENT.register(OverlayRenderer::onHudRender);
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> AelCommands.register(dispatcher));
    }

    private static void recount(MinecraftClient client) {
        try {
            if (client.world == null) {
                loadedCount = 0;
                return;
            }
            int count = 0;
            for (Entity ignored : client.world.getEntities()) {
                count++;
            }
            loadedCount = count;
        } catch (Throwable t) {
            loadedCount = 0;
        }
    }
}
