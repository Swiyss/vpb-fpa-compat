package com.joao2.vpbfpa.trace;

import com.joao2.vpbfpa.VpbFpaCompatClient;
import com.joao2.vpbfpa.config.CompatConfig;
import com.joao2.vpbfpa.detect.DetectedHand;
import com.joao2.vpbfpa.detect.DetectionResult;
import com.joao2.vpbfpa.detect.VpbWeaponDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PoseTraceLogger {
    private static final ThreadLocal<TraceContext> CURRENT_CONTEXT = new ThreadLocal<>();
    private static final Map<String, Integer> LAST_STAGE_TICK = new HashMap<>();
    private static final Map<String, Integer> LAST_HOOK_TICK = new HashMap<>();
    private static final Map<String, Integer> LAST_HELD_ITEM_TICK = new HashMap<>();

    private PoseTraceLogger() {
    }

    public static void trace(String stage, PlayerEntityModel model, PlayerEntityRenderState state) {
        TraceContext context = createContext(state);
        if (context == null) {
            CURRENT_CONTEXT.remove();
            return;
        }

        CURRENT_CONTEXT.set(context);
        traceWithContext(stage, model, context);
    }

    public static void trace(String stage, PlayerEntityModel model, AbstractClientPlayerEntity player) {
        TraceContext context = createContext(player);
        if (context == null) {
            return;
        }

        CURRENT_CONTEXT.set(context);
        traceWithContext(stage, model, context);
    }

    public static void traceCurrent(String stage, PlayerEntityModel model) {
        TraceContext context = CURRENT_CONTEXT.get();
        if (context == null) {
            return;
        }

        traceWithContext(stage, model, context);
    }

    public static void hookReached(String stage, boolean playerModel) {
        CompatConfig config = VpbFpaCompatClient.config();
        MinecraftClient client = MinecraftClient.getInstance();
        if (config == null || !config.poseTracing || !config.poseTraceLogging || client.player == null) {
            return;
        }

        int tick = client.player.age;
        int lastTick = LAST_HOOK_TICK.getOrDefault(stage, Integer.MIN_VALUE);
        if (lastTick != Integer.MIN_VALUE && tick - lastTick < config.normalizedPoseTraceIntervalTicks()) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA Hook] hook={} reached context={} modelClass={}",
                stage,
                CURRENT_CONTEXT.get() != null,
                playerModel ? "player" : "non_player"
        );
        LAST_HOOK_TICK.put(stage, tick);
    }

    public static void heldItemTrace(String stage, ArmedEntityRenderState state, ItemRenderState itemState, ItemStack stack, Arm arm) {
        CompatConfig config = VpbFpaCompatClient.config();
        VpbWeaponDetector detector = VpbFpaCompatClient.weaponDetector();
        MinecraftClient client = MinecraftClient.getInstance();
        if (config == null || detector == null || !config.poseTracing || !config.poseTraceLogging || client.player == null || state == null || stack == null) {
            return;
        }

        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }

        DetectionResult detection = detector.detect(playerState, config);
        boolean local = playerState.id == client.player.getId();
        if (config.poseTraceLocalPlayerOnly && !local) {
            return;
        }
        if (!shouldTraceDetection(config, detection)) {
            return;
        }

        int tick = client.player.age;
        String key = stage + ":" + arm;
        int lastTick = LAST_HELD_ITEM_TICK.getOrDefault(key, Integer.MIN_VALUE);
        if (lastTick != Integer.MIN_VALUE && tick - lastTick < config.normalizedPoseTraceIntervalTicks()) {
            return;
        }

        DetectedHand hand = arm == state.mainArm ? DetectedHand.MAIN : DetectedHand.OFFHAND;
        Identifier itemId = stack.isEmpty() ? null : Registries.ITEM.getId(stack.getItem());
        Object armPose = arm == Arm.RIGHT ? state.rightArmPose : state.leftArmPose;
        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA HeldItemTrace] stage={} tick={} player={} item={} detectionItem={} active={} hand={} arm={} armPose={} itemStateEmpty={} mode=unknown",
                stage,
                tick,
                local ? "local" : "remote",
                itemId == null ? "none" : itemId.toString(),
                detection.active() ? detection.itemId() : "none",
                detection.active(),
                hand.id(),
                arm == null ? "unknown" : arm.name().toLowerCase(Locale.ROOT),
                armPose == null ? "null" : armPose.toString().toLowerCase(Locale.ROOT),
                itemState == null || itemState.isEmpty()
        );
        LAST_HELD_ITEM_TICK.put(key, tick);
    }

    private static void traceWithContext(String stage, PlayerEntityModel model, TraceContext context) {
        CompatConfig config = VpbFpaCompatClient.config();
        if (config == null || !config.poseTracing || !config.poseTraceLogging || model == null) {
            return;
        }

        int interval = config.normalizedPoseTraceIntervalTicks();
        int lastTick = LAST_STAGE_TICK.getOrDefault(stage, Integer.MIN_VALUE);
        if (lastTick != Integer.MIN_VALUE && context.tick - lastTick < interval) {
            return;
        }

        LAST_STAGE_TICK.put(stage, context.tick);
        VpbFpaCompatClient.LOGGER.info(format(stage, model, context));
    }

    private static TraceContext createContext(PlayerEntityRenderState state) {
        CompatConfig config = VpbFpaCompatClient.config();
        VpbWeaponDetector detector = VpbFpaCompatClient.weaponDetector();
        MinecraftClient client = MinecraftClient.getInstance();
        if (config == null || detector == null || client.player == null || state == null) {
            return null;
        }

        DetectionResult detection = detector.detect(state, config);
        boolean local = state.id == client.player.getId();
        return createContext(config, detection, local, state.id, nameFromText(state.playerName), "unknown");
    }

    private static TraceContext createContext(AbstractClientPlayerEntity player) {
        CompatConfig config = VpbFpaCompatClient.config();
        VpbWeaponDetector detector = VpbFpaCompatClient.weaponDetector();
        MinecraftClient client = MinecraftClient.getInstance();
        if (config == null || detector == null || client.player == null || player == null) {
            return null;
        }

        DetectionResult detection = detector.detect(player, config);
        boolean local = player == client.player || player.getUuid().equals(client.player.getUuid());
        UUID uuid = player.getUuid();
        return createContext(config, detection, local, player.getId(), player.getName().getString(), uuid.toString());
    }

    private static TraceContext createContext(CompatConfig config, DetectionResult detection, boolean local, int entityId, String name, String uuid) {
        if (!config.poseTracing || !config.poseTraceLogging) {
            return null;
        }

        if (config.poseTraceLocalPlayerOnly && !local) {
            return null;
        }

        if (!shouldTraceDetection(config, detection)) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int tick = client.player == null ? 0 : client.player.age;
        Perspective perspective = client.options.getPerspective();

        return new TraceContext(
                local,
                entityId,
                name == null || name.isBlank() ? "unknown" : sanitize(name),
                uuid == null || uuid.isBlank() ? "unknown" : uuid,
                detection,
                perspective == null ? "unknown" : perspective.name().toLowerCase(Locale.ROOT),
                tick
        );
    }

    private static boolean shouldTraceDetection(CompatConfig config, DetectionResult detection) {
        if (config.poseTraceIncludeInactive) {
            return true;
        }

        if (!config.poseTraceVpbOnly) {
            return true;
        }

        return detection.active();
    }

    private static String format(String stage, PlayerEntityModel model, TraceContext context) {
        DetectionResult detection = context.detection;
        DetectedHand hand = detection.hand();

        return "[VPB-FPA Trace] stage=" + stage
                + " tick=" + context.tick
                + " player=" + (context.local ? "local" : "remote")
                + " entityId=" + context.entityId
                + " name=" + context.name
                + " uuid=" + context.uuid
                + " perspective=" + context.perspective
                + " item=" + (detection.active() ? detection.itemId() : "none")
                + " active=" + detection.active()
                + " hand=" + hand.id()
                + " method=" + detection.method().id()
                + " rightArm=" + part(model.rightArm)
                + " leftArm=" + part(model.leftArm)
                + " rightSleeve=" + part(model.rightSleeve)
                + " leftSleeve=" + part(model.leftSleeve)
                + " body=" + part(model.body)
                + " head=" + part(model.head)
                + " rightLeg=" + part(model.rightLeg)
                + " leftLeg=" + part(model.leftLeg);
    }

    private static String part(ModelPart part) {
        if (part == null) {
            return "(missing)";
        }

        return String.format(Locale.ROOT, "(%.3f,%.3f,%.3f)", part.pitch, part.yaw, part.roll);
    }

    private static String nameFromText(Text text) {
        return text == null ? "unknown" : text.getString();
    }

    private static String sanitize(String value) {
        return value.replaceAll("\\s+", "_");
    }

    private record TraceContext(
            boolean local,
            int entityId,
            String name,
            String uuid,
            DetectionResult detection,
            String perspective,
            int tick
    ) {
    }
}
