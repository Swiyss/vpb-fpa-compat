package com.joao2.vpbfpa.debug;

import com.joao2.vpbfpa.VpbFpaCompatClient;
import com.joao2.vpbfpa.arm.ArmRestoreManager;
import com.joao2.vpbfpa.config.CompatConfig;
import com.joao2.vpbfpa.detect.DetectionResult;
import com.joao2.vpbfpa.detect.EnvironmentProbe;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class DebugOverlay {
    private static final Identifier ELEMENT_ID = Identifier.of(VpbFpaCompatClient.MOD_ID, "debug_overlay");
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int TITLE_COLOR = 0xFFFFFF55;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0xCC000000;
    private static long lastOverlayLogMs = 0L;

    private DebugOverlay() {
    }

    public static void register(CompatConfig config, EnvironmentProbe environmentProbe, DebugTicker debugTicker) {
        HudElementRegistry.addLast(ELEMENT_ID, (drawContext, tickCounter) -> render(drawContext, config, environmentProbe, debugTicker));
        VpbFpaCompatClient.LOGGER.info("VPB-FPA debug overlay registered with Fabric HudElementRegistry as {}", ELEMENT_ID);
    }

    private static void render(DrawContext drawContext, CompatConfig config, EnvironmentProbe environmentProbe, DebugTicker debugTicker) {
        if (!config.enabled || !config.debugOverlay) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }

        DetectionResult result = debugTicker.lastResult();
        String[] lines = {
                "VPB-FPA Compat",
                "state: " + (result.active() ? "active" : "inactive"),
                "weapon: " + (result.active() ? result.itemId() : "none"),
                "hand: " + result.hand().id(),
                "mode: " + config.armMode().id(),
                "arms: " + ArmRestoreManager.status(),
                "layers: " + config.armRestoreLayerMode().id(),
                "strategy: " + config.armRestoreStrategy().id(),
                "source: " + config.armRestoreSourceMode().id(),
                "sleeve sync: " + config.armLockSleeveSyncMode().id(),
                "pointblank: " + environmentProbe.pointBlankStatus(),
                "GunItem: " + (environmentProbe.gunItemClassFound() ? "found" : "missing"),
                "EMF: " + environmentProbe.emfStatus(),
                "ETF: " + environmentProbe.etfStatus(),
                "NEA: " + (environmentProbe.notEnoughAnimationsModLoaded() ? "detected" : "not detected")
        };
        logOverlayRender(config, result, lines);

        int width = calculateWidth(client, lines);
        int height = PADDING * 2 + lines.length * LINE_HEIGHT;
        int x = config.normalizedOverlayX();
        int y = config.normalizedOverlayY();
        float scale = config.normalizedOverlayScale();

        var matrices = drawContext.getMatrices();
        matrices.pushMatrix();
        try {
            matrices.translate(x, y);
            matrices.scale(scale);

            if (config.overlayBackground) {
                drawContext.fill(0, 0, width + PADDING * 2, height, BACKGROUND_COLOR);
            }

            int lineY = PADDING;
            for (int i = 0; i < lines.length; i++) {
                int color = i == 0 ? TITLE_COLOR : TEXT_COLOR;
                drawContext.drawTextWithShadow(client.textRenderer, lines[i], PADDING, lineY, color);
                lineY += LINE_HEIGHT;
            }
        } finally {
            matrices.popMatrix();
        }
    }

    private static int calculateWidth(MinecraftClient client, String[] lines) {
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, client.textRenderer.getWidth(line));
        }
        return width;
    }

    private static void logOverlayRender(CompatConfig config, DetectionResult result, String[] lines) {
        if (!config.debugLogging) {
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMs = Math.max(3000L, config.normalizedLogIntervalTicks() * 50L);
        if (now - lastOverlayLogMs < intervalMs) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA Compat] overlay render lines={} active={} weapon={}",
                lines.length,
                result.active(),
                result.active() ? result.itemId() : "none"
        );
        lastOverlayLogMs = now;
    }
}
