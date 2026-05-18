package com.joao2.vpbfpa;

import com.joao2.vpbfpa.config.CompatConfig;
import com.joao2.vpbfpa.debug.DebugOverlay;
import com.joao2.vpbfpa.debug.DebugTicker;
import com.joao2.vpbfpa.detect.EnvironmentProbe;
import com.joao2.vpbfpa.detect.VpbWeaponDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VpbFpaCompatClient implements ClientModInitializer {
    public static final String MOD_ID = "vpb_fpa_compat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static CompatConfig config;
    private static EnvironmentProbe environmentProbe;
    private static VpbWeaponDetector weaponDetector;
    private static DebugTicker debugTicker;

    @Override
    public void onInitializeClient() {
        config = CompatConfig.load(FabricLoader.getInstance().getConfigDir().resolve("vpb-fpa-compat.json"));
        environmentProbe = EnvironmentProbe.create();
        weaponDetector = new VpbWeaponDetector(environmentProbe);
        debugTicker = new DebugTicker(config, environmentProbe, weaponDetector);

        ClientTickEvents.END_CLIENT_TICK.register(debugTicker::tick);
        DebugOverlay.register(config, environmentProbe, debugTicker);

        LOGGER.info(
                "VPB-FPA Compat initialized. enabled={}, debugLogging={}, debugOverlay={}, detectionMode={}, armMode={}, armRestoreLayerMode={}, armRestoreStrategy={}, armRestoreSourceMode={}, armLockCustomArmMode={}, armLockSleeveSyncMode={}, armLockFreezeWhileHoldingGun={}, overlay=({}, {}, scale={}, background={}), poseTracing={}, poseTraceLogging={}, poseTraceIntervalTicks={}, pointblankMod={}, gunItemClass={}, emf={}, etf={}, nea={}",
                config.enabled,
                config.debugLogging,
                config.debugOverlay,
                config.detectionMode(),
                config.armMode(),
                config.armRestoreLayerMode(),
                config.armRestoreStrategy(),
                config.armRestoreSourceMode(),
                config.armLockCustomArmMode(),
                config.armLockSleeveSyncMode(),
                config.armLockFreezeWhileHoldingGun,
                config.normalizedOverlayX(),
                config.normalizedOverlayY(),
                config.normalizedOverlayScale(),
                config.overlayBackground,
                config.poseTracing,
                config.poseTraceLogging,
                config.normalizedPoseTraceIntervalTicks(),
                environmentProbe.pointBlankModLoaded(),
                environmentProbe.gunItemClassFound(),
                environmentProbe.emfModLoaded(),
                environmentProbe.etfModLoaded(),
                environmentProbe.notEnoughAnimationsModLoaded()
        );
    }

    public static CompatConfig config() {
        return config;
    }

    public static EnvironmentProbe environmentProbe() {
        return environmentProbe;
    }

    public static VpbWeaponDetector weaponDetector() {
        return weaponDetector;
    }
}
