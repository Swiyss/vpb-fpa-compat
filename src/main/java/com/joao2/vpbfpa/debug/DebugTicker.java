package com.joao2.vpbfpa.debug;

import com.joao2.vpbfpa.VpbFpaCompatClient;
import com.joao2.vpbfpa.config.CompatConfig;
import com.joao2.vpbfpa.detect.DetectionResult;
import com.joao2.vpbfpa.detect.EnvironmentProbe;
import com.joao2.vpbfpa.detect.VpbWeaponDetector;
import net.minecraft.client.MinecraftClient;

public final class DebugTicker {
    private final CompatConfig config;
    private final EnvironmentProbe environmentProbe;
    private final VpbWeaponDetector weaponDetector;

    private DetectionResult lastResult = DetectionResult.inactive();
    private String lastLoggedSummary = "";
    private int ticksSinceIntervalLog = 0;

    public DebugTicker(CompatConfig config, EnvironmentProbe environmentProbe, VpbWeaponDetector weaponDetector) {
        this.config = config;
        this.environmentProbe = environmentProbe;
        this.weaponDetector = weaponDetector;
    }

    public void tick(MinecraftClient client) {
        if (client.player == null) {
            lastResult = DetectionResult.inactive();
            return;
        }

        DetectionResult result = weaponDetector.detect(client.player, config);
        lastResult = result;

        if (!config.debugLogging) {
            return;
        }

        ticksSinceIntervalLog++;
        String summary = summary();
        boolean changed = !summary.equals(lastLoggedSummary);
        boolean intervalElapsed = ticksSinceIntervalLog >= config.normalizedLogIntervalTicks();

        if (changed || (result.active() && intervalElapsed)) {
            VpbFpaCompatClient.LOGGER.info(summary);
            lastLoggedSummary = summary;
            ticksSinceIntervalLog = 0;
        }
    }

    public DetectionResult lastResult() {
        return lastResult;
    }

    public String summary() {
        return "VPB-FPA Compat: "
                + lastResult.summary()
                + ", mode=" + config.armMode().id()
                + ", pointblank=" + environmentProbe.pointBlankStatus()
                + ", gunitem_class=" + (environmentProbe.gunItemClassFound() ? "found" : "not found")
                + ", fresh=" + environmentProbe.freshStatus()
                + ", emf=" + environmentProbe.emfStatus()
                + ", etf=" + environmentProbe.etfStatus();
    }
}
