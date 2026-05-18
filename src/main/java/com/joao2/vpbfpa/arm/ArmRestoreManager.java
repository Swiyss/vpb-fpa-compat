package com.joao2.vpbfpa.arm;

import com.joao2.vpbfpa.VpbFpaCompatClient;
import com.joao2.vpbfpa.config.ArmMode;
import com.joao2.vpbfpa.config.ArmLockSleeveSyncMode;
import com.joao2.vpbfpa.config.ArmRestoreLayerMode;
import com.joao2.vpbfpa.config.ArmRestoreSourceMode;
import com.joao2.vpbfpa.config.ArmRestoreStrategy;
import com.joao2.vpbfpa.config.CompatConfig;
import com.joao2.vpbfpa.detect.DetectionResult;
import com.joao2.vpbfpa.detect.VpbWeaponDetector;
import com.joao2.vpbfpa.trace.ModelPartDumper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ArmRestoreManager {
    private static final ThreadLocal<RenderMutationContext> ACTIVE_RENDER = new ThreadLocal<>();
    private static ArmSnapshot snapshot;
    private static String status = "off";
    private static int lastCaptureLogTick = Integer.MIN_VALUE;
    private static int lastRestoreLogTick = Integer.MIN_VALUE;
    private static int lastCleanupLogTick = Integer.MIN_VALUE;
    private static int lastSkipLogTick = Integer.MIN_VALUE;
    private static int lastLayerCompareLogTick = Integer.MIN_VALUE;
    private static int lastHeldItemSyncTick = Integer.MIN_VALUE;
    private static int lastAuthorityTraceLogTick = Integer.MIN_VALUE;
    private static int lastDeltaTraceLogTick = Integer.MIN_VALUE;
    private static int lastLockTraceLogTick = Integer.MIN_VALUE;
    private static int lastLockCalibrationLogTick = Integer.MIN_VALUE;
    private static int lastSleeveSyncLogTick = Integer.MIN_VALUE;
    private static final Map<String, Integer> LAST_SYNC_TRACE_TICK = new HashMap<>();
    private static final Map<String, Integer> LAST_TRANSFORM_COMPARE_TICK = new HashMap<>();
    private static final Map<String, Integer> LAST_PART_TRACE_TICK = new HashMap<>();
    private static final Set<String> LOGGED_ATTACHMENT_PARTS = new HashSet<>();
    private static LockPoseBridge lockPoseBridge;
    private static FreezePose freezePose;
    private static final ThreadLocal<HeldItemLockContext> HELD_ITEM_LOCK = new ThreadLocal<>();

    private ArmRestoreManager() {
    }

    public static void beginActualModelRenderCommand(PlayerEntityModel model, PlayerEntityRenderState state) {
        cleanupActive("begin_reentered");

        CompatConfig config = VpbFpaCompatClient.config();
        if (!usesScopedRenderMutation(config) || model == null || state == null) {
            ACTIVE_RENDER.remove();
            return;
        }

        ACTIVE_RENDER.set(new RenderMutationContext(model, state.id));
    }

    public static void afterSetAnglesTail(String stage, PlayerEntityModel model, PlayerEntityRenderState state) {
        CompatConfig config = VpbFpaCompatClient.config();
        if (config == null) {
            return;
        }

        ArmMode mode = config.armMode();
        if (mode == ArmMode.VISUAL_PROBE_AFTER_SET_ANGLES_TAIL) {
            MutationRequest request = mutationRequest(config, model, state);
            if (request != null) {
                applyAllArmLikeVisualProbe(config, request, stage);
            }
            return;
        }

        if (mode == ArmMode.RESTORE_VPB_ARMS_AFTER_SET_ANGLES_TAIL) {
            MutationRequest request = mutationRequest(config, model, state);
            if (request != null) {
                logSyncTrace(config, request.tick, stage + "_before_strategy", request.detection, model, "none");
                applyRealRestore(config, request, stage);
                logSyncTrace(config, request.tick, stage + "_after_strategy", request.detection, model, "none");
            }
        }
    }

    public static void beforeActualModelRender(String stage, PlayerEntityModel model, PlayerEntityRenderState state) {
        CompatConfig config = VpbFpaCompatClient.config();
        if (config == null) {
            return;
        }

        ArmMode mode = config.armMode();
        if (mode == ArmMode.VISUAL_PROBE_AT_ACTUAL_MODEL_RENDER || mode == ArmMode.VISUAL_PROBE_ALL_ARM_LIKE_PARTS) {
            MutationRequest request = mutationRequest(config, model, state);
            if (request != null) {
                applyAllArmLikeVisualProbe(config, request, stage);
            }
            return;
        }

        if (mode == ArmMode.VISUAL_PROBE_EXAGGERATED_ARMS) {
            MutationRequest request = mutationRequest(config, model, state);
            if (request != null) {
                applyVanillaVisualProbe(config, request, stage);
            }
            return;
        }

        if (mode == ArmMode.RESTORE_VPB_ARMS_AFTER_SET_ANGLES_TAIL) {
            MutationRequest request = mutationRequest(config, model, state);
            if (request != null) {
                logSyncTrace(config, request.tick, stage + "_before_strategy", request.detection, model, "none");
                if (config.armRestoreStrategy() == ArmRestoreStrategy.RESTORE_ARMS_THEN_SYNC_SLEEVES_LATE) {
                    syncSleevesFromFinalArms(config, request, stage);
                }
                logSyncTrace(config, request.tick, stage + "_after_strategy", request.detection, model, "none");
            }
        }
    }

    public static void cleanupAfterActualModelRender(String stage, PlayerEntityModel model, PlayerEntityRenderState state) {
        RenderMutationContext context = ACTIVE_RENDER.get();
        if (context == null || context.model != model || context.entityId != state.id) {
            return;
        }

        DetectionResult detection = context.detection;
        if (detection != null) {
            logSyncTrace(VpbFpaCompatClient.config(), currentTick(), stage + "_before_cleanup", detection, model, "none");
        }
        logLayerCompare(VpbFpaCompatClient.config(), currentTick(), stage, model);
        int restored = context.restoreOriginals();
        ACTIVE_RENDER.remove();
        if (restored > 0) {
            status = "cleanup";
            logCleanup(VpbFpaCompatClient.config(), currentTick(), stage, restored);
        } else if (status.equals("restored") || status.startsWith("visual probe")) {
            status = "cleanup";
        }
    }

    public static void heldItemSync(String stage, ArmedEntityRenderState state, ItemRenderState itemState, ItemStack stack, Arm arm) {
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
        boolean shouldLog = intervalElapsed(tick, lastHeldItemSyncTick, config.normalizedPoseTraceIntervalTicks());

        RenderMutationContext context = ACTIVE_RENDER.get();
        LockPoseBridge bridge = activeLockPoseBridge(playerState);
        Object armPose = arm == Arm.RIGHT ? state.rightArmPose : state.leftArmPose;
        Identifier itemId = stack.isEmpty() ? null : Registries.ITEM.getId(stack.getItem());
        String contextState = context == null ? "none" : context.entityId == playerState.id ? "active" : "other_entity";
        String bridgeState = bridge == null ? "unavailable" : "available source=" + (arm == Arm.LEFT ? bridge.leftSource().rotation() : bridge.rightSource().rotation());
        String modelSummary = context == null || context.entityId != playerState.id
                ? "model=unavailable"
                : syncSummary(context.model);

        if (shouldLog) {
            VpbFpaCompatClient.LOGGER.info(
                    "[VPB-FPA HeldItemSync] stage={} item={} detectionItem={} active={} arm={} armPose={} itemStateEmpty={} context={} bridge={} strategy={} sourceMode={} {}",
                    stage,
                    itemId == null ? "none" : itemId.toString(),
                    detection.active() ? detection.itemId() : "none",
                    detection.active(),
                    arm == null ? "unknown" : arm.name().toLowerCase(Locale.ROOT),
                    armPose == null ? "null" : armPose.toString().toLowerCase(Locale.ROOT),
                    itemState == null || itemState.isEmpty(),
                    contextState,
                    bridgeState,
                    config.armRestoreStrategy().id(),
                    config.armRestoreSourceMode().id(),
                    modelSummary
            );
            lastHeldItemSyncTick = tick;
        }

        if (config.armMode() == ArmMode.RESTORE_VPB_ARMS_AFTER_SET_ANGLES_TAIL
                && config.armRestoreStrategy() == ArmRestoreStrategy.RESTORE_BEFORE_HELD_ITEM_TOO) {
            applyHeldItemRestoreProbe(config, context, detection, playerState, tick, stage);
        }
        if ("held_item_feature_head".equals(stage)
                && config.armMode() == ArmMode.RESTORE_VPB_ARMS_AFTER_SET_ANGLES_TAIL
                && config.armRestoreStrategy() == ArmRestoreStrategy.LOCK_PARENT_ARMS_AND_HELD_ITEM) {
            applyHeldItemLock(config, bridge, detection, playerState, arm, tick, stage);
        } else if ("held_item_feature_tail".equals(stage)) {
            cleanupHeldItemLock(config, tick, stage);
        }
    }

    public static void partRender(String stage, ModelPart part) {
        CompatConfig config = VpbFpaCompatClient.config();
        RenderMutationContext context = ACTIVE_RENDER.get();
        MinecraftClient client = MinecraftClient.getInstance();
        if (config == null || context == null || context.detection == null || client.player == null || part == null) {
            return;
        }
        if (isFirstPerson()) {
            return;
        }
        if (!shouldTraceDetection(config, context.detection)) {
            return;
        }

        PartMatch match = findPartMatch(config, context, part);
        if (match == null) {
            return;
        }

        maybeApplyPartRenderRestore(config, context, match, stage);
        logPartTrace(config, client.player.age, stage, context, match);
    }

    public static void endActualModelRenderCommand(String stage, PlayerEntityModel model, PlayerEntityRenderState state) {
        cleanupAfterActualModelRender(stage, model, state);
        ACTIVE_RENDER.remove();
    }

    public static void captureModelSetupArms(PlayerEntityModel model, PlayerEntityRenderState state) {
        CompatConfig config = VpbFpaCompatClient.config();
        VpbWeaponDetector detector = VpbFpaCompatClient.weaponDetector();
        MinecraftClient client = MinecraftClient.getInstance();

        if (!isLegacyRestoreMode(config) || detector == null || client.player == null || model == null || state == null) {
            clear("off");
            return;
        }

        if (isFirstPerson()) {
            clear("skipped first_person");
            logSkip(config, client.player.age, "first_person");
            return;
        }

        boolean local = state.id == client.player.getId();
        if (config.armRestoreLocalPlayerOnly && !local) {
            clear("remote_skipped");
            return;
        }

        DetectionResult detection = detector.detect(state, config);
        if (!detection.active()) {
            clear("inactive_or_item_changed");
            logCleared(config, client.player.age);
            return;
        }

        snapshot = ArmSnapshot.capture(state.id, client.player.age, detection.itemId(), detection.hand().id(), local, model);
        status = "captured";
        logCapture(config, client.player.age, snapshot);
    }

    public static void restore(String stage, PlayerEntityModel model, PlayerEntityRenderState state) {
        CompatConfig config = VpbFpaCompatClient.config();
        if (!isLegacyRestoreMode(config)) {
            return;
        }

        // The old submit-time restore path is intentionally disabled for safety. It was
        // proven to run before a later visible setAngles call and can leak mutable state.
        status = "off";
    }

    public static String status() {
        return status;
    }

    private static MutationRequest mutationRequest(CompatConfig config, PlayerEntityModel model, PlayerEntityRenderState state) {
        VpbWeaponDetector detector = VpbFpaCompatClient.weaponDetector();
        MinecraftClient client = MinecraftClient.getInstance();
        RenderMutationContext context = ACTIVE_RENDER.get();
        if (config == null || !config.enabled || detector == null || client.player == null || model == null || state == null) {
            status = "off";
            return null;
        }

        if (context == null || context.model != model || context.entityId != state.id) {
            return null;
        }

        if (isFirstPerson()) {
            status = "skipped first_person";
            logSkip(config, client.player.age, "first_person");
            context.restoreOriginals();
            freezePose = null;
            return null;
        }

        boolean local = state.id == client.player.getId();
        if (config.armRestoreLocalPlayerOnly && !local) {
            status = "remote_skipped";
            context.restoreOriginals();
            return null;
        }

        DetectionResult detection = detector.detect(state, config);
        if (!detection.active()) {
            status = "cleared inactive";
            logCleared(config, client.player.age);
            context.restoreOriginals();
            freezePose = null;
            return null;
        }

        if (context.itemId != null && !context.itemId.equals(detection.itemId())) {
            status = "cleared inactive";
            logCleared(config, client.player.age);
            context.restoreOriginals();
            freezePose = null;
            return null;
        }

        context.itemId = detection.itemId();
        context.detection = detection;
        return new MutationRequest(context, detection, model, client.player.age);
    }

    private static boolean shouldApplyAimOnlyLock(CompatConfig config) {
        if (config == null || !config.armLockAimOnly) {
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options == null || client.options.useKey == null) {
            return false;
        }

        return client.player.isUsingItem() || client.options.useKey.isPressed();
    }

    private static boolean shouldHideSleeves(CompatConfig config, ArmRestoreStrategy strategy) {
        return config != null && config.armLockHideSleeves && usesLockStrategy(strategy);
    }

    private static void applyRealRestore(CompatConfig config, MutationRequest request, String stage) {
        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(request.model, config.visualProbePartNameFilter);
        ArmRestoreStrategy strategy = config.armRestoreStrategy();
        if (usesLockStrategy(strategy) && !shouldApplyAimOnlyLock(config)) {
            status = "aim idle passthrough";
            request.context.restoreOriginals();
            freezePose = null;
            lockPoseBridge = null;
            logSkip(config, request.tick, "aim_only_not_aiming");
            return;
        }
        FullSource rawSource = fullSourceForMode(config.armRestoreSourceMode(), request.model, candidates);
        Rotation rawRightSource = rawSource.right().rotation();
        Rotation rawLeftSource = rawSource.left().rotation();
        FullSource fullSource = usesLockStrategy(strategy) ? calibratedLockSource(config, rawSource) : rawSource;
        FullSource sleeveSource = usesLockStrategy(strategy) ? calibratedSleeveLockSource(config, fullSource) : fullSource;
        Rotation rightSource = fullSource.right().rotation();
        Rotation leftSource = fullSource.left().rotation();
        if (config.armLockFreezeWhileHoldingGun && usesLockStrategy(strategy)) {
            FullSource frozen = frozenSource(request, fullSource);
            rightSource = frozen.right().rotation();
            leftSource = frozen.left().rotation();
            fullSource = frozen;
            sleeveSource = calibratedSleeveLockSource(config, frozen);
        } else if (!config.armLockFreezeWhileHoldingGun) {
            freezePose = null;
        }
        request.context.rightSource = rightSource;
        request.context.leftSource = leftSource;
        request.context.rightFullSource = fullSource.right();
        request.context.leftFullSource = fullSource.left();
        request.context.rightSleeveFullSource = sleeveSource.right();
        request.context.leftSleeveFullSource = sleeveSource.left();

        if (strategy == ArmRestoreStrategy.OBSERVE_ONLY || strategy == ArmRestoreStrategy.OBSERVE_FULL_TRANSFORM_ONLY) {
            status = "observe only";
            logObserve(config, request.tick, stage, request.detection.itemId());
            logTransformCompare(config, request.tick, stage + "_observe", request.detection, request.model);
            return;
        }
        if (strategy == ArmRestoreStrategy.PARENT_ARM_SOURCE_CUSTOM_ARM_DELTA) {
            status = "delta skipped";
            logSkip(config, request.tick, "delta_strategy_not_implemented_conservative");
            logAuthorityTrace(config, request.tick, stage + "_delta_skipped", request.detection, request.model);
            return;
        }

        if (!isLikelyGunArmPose(rawRightSource, rawLeftSource)) {
            status = "skipped source";
            logSkip(config, request.tick, "source_not_gun_like");
            return;
        }

        if (candidates.isEmpty()) {
            status = "no safe candidates";
            logSkip(config, request.tick, "no_safe_candidates");
            return;
        }

        ArmRestoreLayerMode layerMode = config.armRestoreLayerMode();
        RestoreResult result = applySelectedParts(config, request, candidates, layerMode, strategy, rightSource, leftSource);

        if (result.touchedParts().isEmpty()) {
            status = "no layer candidates";
            logSkip(config, request.tick, "no_layer_candidates");
            return;
        }

        status = "restored";
        publishLockPoseBridge(config, request);
        logLockCalibration(config, request.tick, request.detection, strategy, rawSource, fullSource, sleeveSource);
        logAuthorityTrace(config, request.tick, stage, request.detection, request.model);
        logRealRestore(config, request.tick, stage, request.detection.itemId(), rightSource, result.rightEmfBefore(), result.rightEmfAfter(), result.touchedParts(), layerMode, strategy, result.partial());
    }

    private static void applyVanillaVisualProbe(CompatConfig config, MutationRequest request, String stage) {
        Rotation right = new Rotation(-2.6F, -0.85F, 1.2F);
        Rotation left = new Rotation(-2.6F, 0.85F, -1.2F);
        request.context.saveOriginal(request.model.rightArm);
        request.context.saveOriginal(request.model.rightSleeve);
        request.context.saveOriginal(request.model.leftArm);
        request.context.saveOriginal(request.model.leftSleeve);
        right.applyTo(request.model.rightArm);
        right.applyTo(request.model.rightSleeve);
        left.applyTo(request.model.leftArm);
        left.applyTo(request.model.leftSleeve);
        status = "visual probe";

        if (config.debugLogging && intervalElapsed(request.tick, lastRestoreLogTick, config.normalizedLogIntervalTicks())) {
            VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] visual_probe applied stage={} item={} target=vanilla parts=4 rightArm={} leftArm={}", stage, request.detection.itemId(), right, left);
            lastRestoreLogTick = request.tick;
        }
    }

    private static void applyAllArmLikeVisualProbe(CompatConfig config, MutationRequest request, String stage) {
        Rotation right = new Rotation(-2.6F, -0.85F, 1.2F);
        Rotation left = new Rotation(-2.6F, 0.85F, -1.2F);

        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(request.model, config.visualProbePartNameFilter);
        if (candidates.isEmpty()) {
            status = "no safe candidates";
            logSkip(config, request.tick, "no_safe_candidates");
            if (config.debugLogging && intervalElapsed(request.tick, lastRestoreLogTick, config.normalizedLogIntervalTicks())) {
                VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] visual_probe skipped reason=no_safe_candidates stage={} item={} filter='{}'", stage, request.detection.itemId(), config.visualProbePartNameFilter);
                lastRestoreLogTick = request.tick;
            }
            return;
        }

        int changed = 0;
        for (ModelPartDumper.NamedPart part : candidates) {
            Rotation rotation = part.path().toLowerCase(Locale.ROOT).contains("left") ? left : right;
            request.context.saveOriginal(part.part());
            rotation.applyTo(part.part());
            changed++;
        }

        status = "visual probe all";

        if (config.debugLogging && intervalElapsed(request.tick, lastRestoreLogTick, config.normalizedLogIntervalTicks())) {
            VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] visual_probe applied stage={} item={} target=all parts={} filter='{}'", stage, request.detection.itemId(), changed, config.visualProbePartNameFilter);
            lastRestoreLogTick = request.tick;
        }
    }

    private static RestoreResult applySelectedParts(CompatConfig config, MutationRequest request, List<ModelPartDumper.NamedPart> candidates, ArmRestoreLayerMode layerMode, ArmRestoreStrategy strategy, Rotation rightSource, Rotation leftSource) {
        if (usesSleeveAuthorityStrategy(strategy)) {
            return applySleeveAuthorityStrategy(config, request, candidates, strategy);
        }
        if (usesFullTransformStrategy(strategy)) {
            return applyFullTransformStrategy(config, request, candidates, layerMode, strategy);
        }

        List<TouchedPart> touchedParts = new ArrayList<>();
        int customCandidates = 0;
        int expectedCustomCandidates = expectedCustomCandidates(layerMode, strategy);
        TransformState rightEmfBefore = null;
        TransformState rightEmfAfter = null;

        for (ModelPartDumper.NamedPart candidate : candidates) {
            CandidateInfo info = CandidateInfo.from(candidate);
            if (!selectedByLayerMode(info, layerMode)) {
                continue;
            }
            if (strategy == ArmRestoreStrategy.RESTORE_ARMS_THEN_SYNC_SLEEVES_LATE && info.sleeve()) {
                continue;
            }

            Rotation source = sourceForStrategy(strategy, request.model, candidates, info, rightSource, leftSource);
            TransformState before = TransformState.from(candidate.part());
            if (info.emfCustom()) {
                customCandidates++;
                if (rightEmfBefore == null && info.right() && info.arm()) {
                    rightEmfBefore = before;
                }
            }
            request.context.saveOriginal(candidate.part());
            source.applyTo(candidate.part());
            TransformState after = TransformState.from(candidate.part());
            if (rightEmfAfter == null && info.emfCustom() && info.right() && info.arm()) {
                rightEmfAfter = after;
            }
            touchedParts.add(new TouchedPart(candidate.path(), info.kind(), candidate.part().getClass().getName(), before, after));
        }

        boolean partial = expectedCustomCandidates > 0 && customCandidates < expectedCustomCandidates;
        return new RestoreResult(touchedParts, rightEmfBefore, rightEmfAfter, partial);
    }

    private static RestoreResult applySleeveAuthorityStrategy(CompatConfig config, MutationRequest request, List<ModelPartDumper.NamedPart> candidates, ArmRestoreStrategy strategy) {
        List<TouchedPart> touchedParts = new ArrayList<>();
        TransformState rightEmfBefore = null;
        TransformState rightEmfAfter = null;

        for (ModelPartDumper.NamedPart candidate : candidates) {
            CandidateInfo info = CandidateInfo.from(candidate);
            if ((!info.arm() && !info.sleeve()) || (!info.left() && !info.right())) {
                continue;
            }

            TransformState before = TransformState.from(candidate.part());
            TransformState source = sourceForSleeveAuthority(request.context, strategy, info);
            boolean change = false;
            boolean hide = false;
            boolean neutral = false;

            switch (strategy) {
                case SOURCE_POSE_SLEEVES_ONLY_HIDE_BASE_ARMS -> {
                    change = info.sleeve();
                    hide = info.arm();
                }
                case SOURCE_POSE_SLEEVES_AND_CUSTOM_ONLY -> change = info.sleeve() || (info.emfCustom() && info.arm());
                case SOURCE_POSE_SLEEVES_HIDE_EMF_CUSTOM_ARMS -> {
                    change = info.sleeve();
                    hide = info.emfCustom() && info.arm();
                }
                case SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA,
                     SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_PITCH_ONLY,
                     SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_NO_ROLL,
                     LOCK_PARENT_ARMS_AFTER_EMF_ANIMATE,
                     LOCK_PARENT_ARMS_AND_HELD_ITEM -> change = info.sleeve();
                case PARENT_ARM_SOURCE_CUSTOM_ARM_NEUTRAL -> {
                    change = info.sleeve() || (!info.emfCustom() && info.arm());
                    neutral = info.emfCustom() && info.arm();
                }
                default -> {
                }
            }

            if (!change && !hide && !neutral) {
                continue;
            }
            if (source == null || !source.isFinite()) {
                continue;
            }

            if (shouldHideSleeves(config, strategy) && info.sleeve()) {
                change = false;
                hide = true;
            }

            if (rightEmfBefore == null && info.emfCustom() && info.right() && info.arm()) {
                rightEmfBefore = before;
            }

            request.context.saveOriginal(candidate.part());
            if (change) {
                source.applyRotationTo(candidate.part());
            }
            if (neutral) {
                Rotation.ZERO.applyTo(candidate.part());
            }
            if (hide) {
                TransformState.from(candidate.part()).withHidden(true).applyFullTo(candidate.part());
                logAuthorityHide(config, request.tick, candidate.path());
            }

            TransformState after = TransformState.from(candidate.part());
            if (rightEmfAfter == null && info.emfCustom() && info.right() && info.arm()) {
                rightEmfAfter = after;
            }
            touchedParts.add(new TouchedPart(candidate.path(), info.kind(), candidate.part().getClass().getName(), before, after));
        }

        return new RestoreResult(touchedParts, rightEmfBefore, rightEmfAfter, false);
    }

    private static TransformState sourceForSleeveAuthority(RenderMutationContext context, ArmRestoreStrategy strategy, CandidateInfo info) {
        if (usesLockStrategy(strategy) && info.sleeve()) {
            TransformState sleeveSource = info.left() ? context.leftSleeveFullSource : context.rightSleeveFullSource;
            if (sleeveSource != null) {
                return sleeveSource;
            }
        }
        return info.left() ? context.leftFullSource : context.rightFullSource;
    }

    private static RestoreResult applyFullTransformStrategy(CompatConfig config, MutationRequest request, List<ModelPartDumper.NamedPart> candidates, ArmRestoreLayerMode layerMode, ArmRestoreStrategy strategy) {
        List<TouchedPart> touchedParts = new ArrayList<>();
        TransformState rightEmfBefore = null;
        TransformState rightEmfAfter = null;

        for (ModelPartDumper.NamedPart candidate : candidates) {
            CandidateInfo info = CandidateInfo.from(candidate);
            if (!info.left() && !info.right()) {
                continue;
            }

            TransformState source = switch (strategy) {
                case COPY_FULL_TRANSFORM_TO_ARMS_AND_SLEEVES -> selectedByLayerMode(info, layerMode)
                        ? (info.left() ? request.context.leftFullSource : request.context.rightFullSource)
                        : null;
                case COPY_SLEEVE_TRANSFORM_TO_BASE_ARM -> info.arm()
                        ? matchingSleeveTransform(request.model, candidates, info)
                        : null;
                case COPY_EMF_SLEEVE_TRANSFORM_TO_EMF_ARM -> info.emfCustom() && info.arm()
                        ? matchingEmfSleeveTransform(candidates, info)
                        : null;
                case HIDE_BASE_ARMS_KEEP_SLEEVES -> info.arm()
                        ? TransformState.from(candidate.part()).withHidden(true)
                        : null;
                case HIDE_SLEEVES_KEEP_BASE_ARMS -> info.sleeve()
                        ? TransformState.from(candidate.part()).withHidden(true)
                        : null;
                default -> null;
            };

            if (source == null || !source.isFinite()) {
                continue;
            }

            TransformState before = TransformState.from(candidate.part());
            if (rightEmfBefore == null && info.emfCustom() && info.right() && info.arm()) {
                rightEmfBefore = before;
            }
            request.context.saveOriginal(candidate.part());
            source.applyFullTo(candidate.part());
            TransformState after = TransformState.from(candidate.part());
            if (rightEmfAfter == null && info.emfCustom() && info.right() && info.arm()) {
                rightEmfAfter = after;
            }
            touchedParts.add(new TouchedPart(candidate.path(), info.kind(), candidate.part().getClass().getName(), before, after));
        }

        return new RestoreResult(touchedParts, rightEmfBefore, rightEmfAfter, false);
    }

    private static boolean usesFullTransformStrategy(ArmRestoreStrategy strategy) {
        return strategy == ArmRestoreStrategy.COPY_FULL_TRANSFORM_TO_ARMS_AND_SLEEVES
                || strategy == ArmRestoreStrategy.COPY_SLEEVE_TRANSFORM_TO_BASE_ARM
                || strategy == ArmRestoreStrategy.COPY_EMF_SLEEVE_TRANSFORM_TO_EMF_ARM
                || strategy == ArmRestoreStrategy.HIDE_BASE_ARMS_KEEP_SLEEVES
                || strategy == ArmRestoreStrategy.HIDE_SLEEVES_KEEP_BASE_ARMS;
    }

    private static boolean usesSleeveAuthorityStrategy(ArmRestoreStrategy strategy) {
        return strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_ONLY_HIDE_BASE_ARMS
                || strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_AND_CUSTOM_ONLY
                || strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_HIDE_EMF_CUSTOM_ARMS
                || strategy == ArmRestoreStrategy.PARENT_ARM_SOURCE_CUSTOM_ARM_NEUTRAL
                || strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA
                || strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_PITCH_ONLY
                || strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_NO_ROLL
                || strategy == ArmRestoreStrategy.LOCK_PARENT_ARMS_AFTER_EMF_ANIMATE
                || strategy == ArmRestoreStrategy.LOCK_PARENT_ARMS_AND_HELD_ITEM;
    }

    private static Rotation sourceForStrategy(ArmRestoreStrategy strategy, PlayerEntityModel model, List<ModelPartDumper.NamedPart> candidates, CandidateInfo info, Rotation rightSource, Rotation leftSource) {
        if (info.sleeve() && strategy == ArmRestoreStrategy.COPY_SLEEVES_FROM_FINAL_ARMS) {
            return matchingFinalArmRotation(model, candidates, info, rightSource, leftSource);
        }
        return info.left() ? leftSource : rightSource;
    }

    private static void syncSleevesFromFinalArms(CompatConfig config, MutationRequest request, String stage) {
        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(request.model, config.visualProbePartNameFilter);
        if (candidates.isEmpty()) {
            logSkip(config, request.tick, "no_safe_candidates");
            return;
        }

        ArmRestoreLayerMode layerMode = config.armRestoreLayerMode();
        List<TouchedPart> touchedParts = new ArrayList<>();
        for (ModelPartDumper.NamedPart candidate : candidates) {
            CandidateInfo info = CandidateInfo.from(candidate);
            if (!info.sleeve() || !selectedByLayerMode(info, layerMode)) {
                continue;
            }
            Rotation source = matchingFinalArmRotation(request.model, candidates, info, request.context.rightSource, request.context.leftSource);
            TransformState before = TransformState.from(candidate.part());
            request.context.saveOriginal(candidate.part());
            source.applyTo(candidate.part());
            touchedParts.add(new TouchedPart(candidate.path(), info.kind(), candidate.part().getClass().getName(), before, TransformState.from(candidate.part())));
        }

        if (touchedParts.isEmpty()) {
            logSkip(config, request.tick, "no_late_sleeve_candidates");
            return;
        }

        status = "restored";
        logStrategyTouch(config, request.tick, stage, request.detection.itemId(), "sync_sleeves_late", touchedParts);
    }

    private static Rotation matchingFinalArmRotation(PlayerEntityModel model, List<ModelPartDumper.NamedPart> candidates, CandidateInfo sleeveInfo, Rotation rightFallback, Rotation leftFallback) {
        Rotation customArm = firstCandidateRotation(candidates, sleeveInfo.right(), false);
        if (sleeveInfo.emfCustom() && customArm != null) {
            return customArm;
        }
        if (sleeveInfo.left()) {
            return model.leftArm == null ? leftFallback : Rotation.from(model.leftArm);
        }
        if (sleeveInfo.right()) {
            return model.rightArm == null ? rightFallback : Rotation.from(model.rightArm);
        }
        return sleeveInfo.left() ? leftFallback : rightFallback;
    }

    private static TransformState matchingSleeveTransform(PlayerEntityModel model, List<ModelPartDumper.NamedPart> candidates, CandidateInfo armInfo) {
        ModelPart matchingSameFamily = firstCandidatePart(candidates, armInfo.right(), true, armInfo.emfCustom());
        if (matchingSameFamily != null) {
            return TransformState.from(matchingSameFamily);
        }

        if (!armInfo.emfCustom()) {
            ModelPart vanillaSleeve = armInfo.left() ? model.leftSleeve : model.rightSleeve;
            if (vanillaSleeve != null) {
                return TransformState.from(vanillaSleeve);
            }
        }

        ModelPart anySleeve = firstCandidatePart(candidates, armInfo.right(), true, null);
        if (anySleeve != null) {
            return TransformState.from(anySleeve);
        }

        return TransformState.from(armInfo.left() ? model.leftArm : model.rightArm);
    }

    private static TransformState matchingEmfSleeveTransform(List<ModelPartDumper.NamedPart> candidates, CandidateInfo armInfo) {
        ModelPart sleeve = firstCandidatePart(candidates, armInfo.right(), true, true);
        return sleeve == null ? null : TransformState.from(sleeve);
    }

    private static FullSource fullSourceForMode(ArmRestoreSourceMode sourceMode, PlayerEntityModel model, List<ModelPartDumper.NamedPart> candidates) {
        return switch (sourceMode) {
            case VANILLA_ARM_SOURCE -> new FullSource(TransformState.from(model.rightArm), TransformState.from(model.leftArm));
            case VANILLA_SLEEVE_SOURCE -> new FullSource(
                    model.rightSleeve == null ? TransformState.from(model.rightArm) : TransformState.from(model.rightSleeve),
                    model.leftSleeve == null ? TransformState.from(model.leftArm) : TransformState.from(model.leftSleeve)
            );
            case EMF_CUSTOM_ARM_SOURCE -> new FullSource(
                    firstCandidateTransform(candidates, true, false, true, model.rightArm),
                    firstCandidateTransform(candidates, false, false, true, model.leftArm)
            );
            case EMF_CUSTOM_SLEEVE_SOURCE -> new FullSource(
                    firstCandidateTransform(candidates, true, true, true, model.rightSleeve == null ? model.rightArm : model.rightSleeve),
                    firstCandidateTransform(candidates, false, true, true, model.leftSleeve == null ? model.leftArm : model.leftSleeve)
            );
        };
    }

    private static FullSource calibratedLockSource(CompatConfig config, FullSource source) {
        return new FullSource(
                source.right().withRotationOffset(
                        finiteOffset(config.armLockRightPitchOffset),
                        finiteOffset(config.armLockRightYawOffset),
                        finiteOffset(config.armLockRightRollOffset)
                ),
                source.left().withRotationOffset(
                        finiteOffset(config.armLockLeftPitchOffset),
                        finiteOffset(config.armLockLeftYawOffset),
                        finiteOffset(config.armLockLeftRollOffset)
                )
        );
    }

    private static FullSource calibratedSleeveLockSource(CompatConfig config, FullSource source) {
        return new FullSource(
                source.right().withRotationOffset(
                        finiteOffset(config.armLockRightSleevePitchOffset),
                        finiteOffset(config.armLockRightSleeveYawOffset),
                        finiteOffset(config.armLockRightSleeveRollOffset)
                ),
                source.left().withRotationOffset(
                        finiteOffset(config.armLockLeftSleevePitchOffset),
                        finiteOffset(config.armLockLeftSleeveYawOffset),
                        finiteOffset(config.armLockLeftSleeveRollOffset)
                )
        );
    }

    private static float finiteOffset(float value) {
        return Float.isFinite(value) ? value : 0.0F;
    }

    private static TransformState firstCandidateTransform(List<ModelPartDumper.NamedPart> candidates, boolean right, boolean sleeve, boolean emfCustomRequired, ModelPart fallback) {
        ModelPart part = firstCandidatePart(candidates, right, sleeve, emfCustomRequired);
        return part == null ? TransformState.from(fallback) : TransformState.from(part);
    }

    private static FullSource frozenSource(MutationRequest request, FullSource currentSource) {
        String itemId = request.detection.itemId();
        if (freezePose == null || !freezePose.itemId().equals(itemId)) {
            freezePose = new FreezePose(itemId, currentSource.right(), currentSource.left());
        }
        return new FullSource(freezePose.rightSource(), freezePose.leftSource());
    }

    private static void publishLockPoseBridge(CompatConfig config, MutationRequest request) {
        if (!usesLockStrategy(config.armRestoreStrategy()) || request.context.rightFullSource == null || request.context.leftFullSource == null) {
            return;
        }

        lockPoseBridge = new LockPoseBridge(
                request.context.entityId,
                request.tick,
                request.detection.itemId(),
                request.model,
                request.context.rightFullSource,
                request.context.leftFullSource
        );
    }

    private static LockPoseBridge activeLockPoseBridge(PlayerEntityRenderState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (lockPoseBridge == null || state == null || client.player == null) {
            return null;
        }
        if (lockPoseBridge.entityId() != state.id) {
            return null;
        }
        if (client.player.age - lockPoseBridge.tick() > 1) {
            lockPoseBridge = null;
            return null;
        }
        return lockPoseBridge;
    }

    private static void applyHeldItemRestoreProbe(CompatConfig config, RenderMutationContext context, DetectionResult detection, PlayerEntityRenderState state, int tick, String stage) {
        if (context == null || context.entityId != state.id || context.rightSource == null || context.leftSource == null) {
            logSkip(config, tick, "held_item_no_active_model_context");
            return;
        }
        if (isFirstPerson()) {
            logSkip(config, tick, "first_person");
            return;
        }

        MutationRequest request = new MutationRequest(context, detection, context.model, tick);
        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(context.model, config.visualProbePartNameFilter);
        RestoreResult result = applySelectedParts(config, request, candidates, config.armRestoreLayerMode(), ArmRestoreStrategy.SOURCE_POSE_TO_SELECTED_PARTS, context.rightSource, context.leftSource);
        if (result.touchedParts().isEmpty()) {
            logSkip(config, tick, "held_item_no_layer_candidates");
            return;
        }
        logStrategyTouch(config, tick, stage, detection.itemId(), "restore_before_held_item_too", result.touchedParts());
    }

    private static void applyHeldItemLock(CompatConfig config, LockPoseBridge bridge, DetectionResult detection, PlayerEntityRenderState state, Arm arm, int tick, String stage) {
        if (bridge == null || bridge.entityId() != state.id || bridge.model() == null) {
            logSkip(config, tick, "held_item_no_lock_bridge");
            return;
        }
        if (isFirstPerson()) {
            logSkip(config, tick, "first_person");
            return;
        }
        if (!shouldApplyAimOnlyLock(config)) {
            logSkip(config, tick, "aim_only_not_aiming");
            return;
        }

        ModelPart part = arm == Arm.LEFT ? bridge.model().leftArm : bridge.model().rightArm;
        TransformState source = arm == Arm.LEFT ? bridge.leftSource() : bridge.rightSource();
        if (part == null || source == null || !source.isFinite()) {
            logSkip(config, tick, "held_item_no_accessible_model_part");
            return;
        }

        TransformState original = TransformState.from(part);
        source.applyRotationTo(part);
        HELD_ITEM_LOCK.set(new HeldItemLockContext(part, original));
        if (config.debugLogging && intervalElapsed(tick, lastRestoreLogTick, config.normalizedLogIntervalTicks())) {
            VpbFpaCompatClient.LOGGER.info(
                    "[VPB-FPA ArmFix] held_item_lock applied item={} arm={} source={} stage={}",
                    detection.active() ? detection.itemId() : "none",
                    arm == null ? "unknown" : arm.name().toLowerCase(Locale.ROOT),
                    source.rotation(),
                    stage
            );
            lastRestoreLogTick = tick;
        }
    }

    private static void cleanupHeldItemLock(CompatConfig config, int tick, String stage) {
        HeldItemLockContext heldItemLock = HELD_ITEM_LOCK.get();
        if (heldItemLock == null) {
            return;
        }
        heldItemLock.original().applyFullTo(heldItemLock.part());
        HELD_ITEM_LOCK.remove();
        if (config != null && config.debugLogging && intervalElapsed(tick, lastCleanupLogTick, config.normalizedLogIntervalTicks())) {
            VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] held_item_lock cleanup stage={}", stage);
            lastCleanupLogTick = tick;
        }
    }

    private static PartMatch findPartMatch(CompatConfig config, RenderMutationContext context, ModelPart part) {
        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(context.model, config.visualProbePartNameFilter);
        for (ModelPartDumper.NamedPart candidate : candidates) {
            if (candidate.part() == part) {
                CandidateInfo info = CandidateInfo.from(candidate);
                if ((info.arm() || info.sleeve()) && (info.left() || info.right())) {
                    return new PartMatch(candidate, info);
                }
            }
        }
        return null;
    }

    private static void maybeApplyPartRenderRestore(CompatConfig config, RenderMutationContext context, PartMatch match, String stage) {
        ArmRestoreStrategy strategy = config.armRestoreStrategy();
        if (usesLockStrategy(strategy)) {
            maybeApplyParentArmLock(config, context, match, stage, strategy);
            return;
        }
        if (usesParentDeltaStrategy(strategy)) {
            maybeApplyParentDelta(config, context, match, stage, strategy);
            return;
        }

        boolean restoreAllRelevant = strategy == ArmRestoreStrategy.RESTORE_AT_ARM_PART_RENDER
                && selectedByLayerMode(match.info(), config.armRestoreLayerMode());
        boolean restoreBaseArmOnly = strategy == ArmRestoreStrategy.RESTORE_BASE_ARMS_AT_PART_RENDER_ONLY
                && match.info().arm()
                && !match.info().sleeve()
                && !match.info().emfCustom();
        if (!restoreAllRelevant && !restoreBaseArmOnly) {
            return;
        }
        if (context.rightSource == null || context.leftSource == null) {
            logSkip(config, currentTick(), "part_render_no_source_pose");
            return;
        }

        Rotation source = match.info().left() ? context.leftSource : context.rightSource;
        if (!source.isFinite()) {
            logSkip(config, currentTick(), "part_render_source_not_finite");
            return;
        }

        TransformState before = TransformState.from(match.namedPart().part());
        context.saveOriginal(match.namedPart().part());
        source.applyTo(match.namedPart().part());
        TransformState after = TransformState.from(match.namedPart().part());
        if (config.debugLogging && intervalElapsed(currentTick(), lastRestoreLogTick, config.normalizedLogIntervalTicks())) {
            VpbFpaCompatClient.LOGGER.info(
                    "[VPB-FPA ArmFix] strategy_action=part_render_restore stage={} item={} part={} kind={} layerMode={} strategy={} before={} after={}",
                    stage,
                    context.detection.active() ? context.detection.itemId() : "none",
                    match.namedPart().path(),
                    match.info().kind(),
                    config.armRestoreLayerMode().id(),
                    strategy.id(),
                    before,
                    after
            );
            lastRestoreLogTick = currentTick();
        }
    }

    private static void maybeApplyParentArmLock(CompatConfig config, RenderMutationContext context, PartMatch match, String stage, ArmRestoreStrategy strategy) {
        if (!"emf_vanilla_part_render_head".equals(stage)) {
            return;
        }
        CandidateInfo info = match.info();
        if (info.emfCustom() || !info.arm() || (!info.left() && !info.right())) {
            return;
        }
        if (context.rightSource == null || context.leftSource == null) {
            logSkip(config, currentTick(), "lock_no_source_pose");
            return;
        }

        Rotation source = info.left() ? context.leftSource : context.rightSource;
        if (!source.isFinite()) {
            logSkip(config, currentTick(), "lock_source_not_finite");
            return;
        }

        ModelPart parent = match.namedPart().part();
        Rotation parentBefore = Rotation.from(parent);
        context.saveOriginal(parent);
        source.applyTo(parent);
        Rotation parentAfter = Rotation.from(parent);

        ModelPart customArm = firstCandidatePart(ModelPartDumper.findArmLikeParts(context.model, ""), info.right(), false, true);
        Rotation customBefore = null;
        Rotation customAfter = null;
        if (customArm != null) {
            customBefore = Rotation.from(customArm);
            context.saveOriginal(customArm);
            switch (config.armLockCustomArmMode()) {
                case NEUTRAL -> Rotation.ZERO.applyTo(customArm);
                case SOURCE -> source.applyTo(customArm);
                case HIDDEN -> TransformState.from(customArm).withHidden(true).applyFullTo(customArm);
            }
            customAfter = Rotation.from(customArm);
        }

        status = "locked";
        maybeApplyLockedParentSleeveSync(config, context, info, source, parentAfter, strategy, stage);
        logLockTrace(config, currentTick(), stage, context.detection, match.namedPart().path(), source, parentBefore, parentAfter, customBefore, customAfter, strategy);
    }

    private static void maybeApplyLockedParentSleeveSync(CompatConfig config, RenderMutationContext context, CandidateInfo parentInfo, Rotation source, Rotation parentAfter, ArmRestoreStrategy strategy, String stage) {
        if (shouldHideSleeves(config, strategy)) {
            return;
        }
        if (config.armLockSleeveSyncMode() != ArmLockSleeveSyncMode.LOCKED_PARENT_POSE) {
            return;
        }
        if (!usesLockStrategy(strategy) || parentAfter == null || !parentAfter.isFinite()) {
            return;
        }

        TransformState parentState = TransformState.from(parentInfo.left() ? context.model.leftArm : context.model.rightArm);
        TransformState sleeveState = sleeveStateFromLockedParent(config, parentInfo, parentState);
        if (!sleeveState.isFinite()) {
            logSkip(config, currentTick(), "sleeve_sync_source_not_finite");
            return;
        }

        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(context.model, config.visualProbePartNameFilter);
        List<TouchedPart> touchedParts = new ArrayList<>();
        applyLateSleeveSyncToPart(config, context, touchedParts, parentInfo.left() ? "leftSleeve" : "rightSleeve", parentInfo.left() ? context.model.leftSleeve : context.model.rightSleeve, sleeveState);

        ModelPart emfSleeve = firstCandidatePart(candidates, parentInfo.right(), true, true);
        if (emfSleeve != (parentInfo.left() ? context.model.leftSleeve : context.model.rightSleeve)) {
            applyLateSleeveSyncToPart(config, context, touchedParts, parentInfo.left() ? "leftSleeve/EMF_left_sleeve" : "rightSleeve/EMF_right_sleeve", emfSleeve, sleeveState);
        }

        if (touchedParts.isEmpty()) {
            logSkip(config, currentTick(), "sleeve_sync_no_sleeve_candidates");
            return;
        }

        logSleeveSync(config, currentTick(), stage, context.detection, parentInfo, source, parentState.rotation(), sleeveState.rotation(), touchedParts);
    }

    private static TransformState sleeveStateFromLockedParent(CompatConfig config, CandidateInfo info, TransformState parentState) {
        if (info.left()) {
            return parentState.withRotationOffset(
                    finiteOffset(config.armLockLeftSleevePitchOffset),
                    finiteOffset(config.armLockLeftSleeveYawOffset),
                    finiteOffset(config.armLockLeftSleeveRollOffset)
            );
        }
        return parentState.withRotationOffset(
                finiteOffset(config.armLockRightSleevePitchOffset),
                finiteOffset(config.armLockRightSleeveYawOffset),
                finiteOffset(config.armLockRightSleeveRollOffset)
        );
    }

    private static void applyLateSleeveSyncToPart(CompatConfig config, RenderMutationContext context, List<TouchedPart> touchedParts, String path, ModelPart part, TransformState source) {
        if (part == null || source == null || !source.isFinite()) {
            return;
        }

        TransformState before = TransformState.from(part);
        context.saveOriginal(part);
        source.applyRotationTo(part);
        touchedParts.add(new TouchedPart(path, "sleeve", part.getClass().getName(), before, TransformState.from(part)));
    }

    private static void maybeApplyParentDelta(CompatConfig config, RenderMutationContext context, PartMatch match, String stage, ArmRestoreStrategy strategy) {
        if (!"emf_custom_part_render_head".equals(stage)) {
            return;
        }
        CandidateInfo info = match.info();
        if (!info.emfCustom() || !info.arm() || (!info.left() && !info.right())) {
            return;
        }
        if (context.rightSource == null || context.leftSource == null) {
            logSkip(config, currentTick(), "delta_no_source_pose");
            return;
        }

        ModelPart parentPart = info.left() ? context.model.leftArm : context.model.rightArm;
        Rotation source = info.left() ? context.leftSource : context.rightSource;
        Rotation parent = Rotation.from(parentPart);
        Rotation before = Rotation.from(match.namedPart().part());
        Rotation delta = parentDelta(strategy, source, parent);
        if (!delta.isFinite()) {
            logSkip(config, currentTick(), "delta_not_finite");
            return;
        }

        context.saveOriginal(match.namedPart().part());
        delta.applyTo(match.namedPart().part());
        Rotation after = Rotation.from(match.namedPart().part());
        status = "delta";
        logDeltaTrace(config, currentTick(), match.namedPart().path(), strategy, source, parent, delta, before, after);
    }

    private static boolean usesParentDeltaStrategy(ArmRestoreStrategy strategy) {
        return strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA
                || strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_PITCH_ONLY
                || strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_NO_ROLL;
    }

    private static boolean usesLockStrategy(ArmRestoreStrategy strategy) {
        return strategy == ArmRestoreStrategy.LOCK_PARENT_ARMS_AFTER_EMF_ANIMATE
                || strategy == ArmRestoreStrategy.LOCK_PARENT_ARMS_AND_HELD_ITEM;
    }

    private static Rotation parentDelta(ArmRestoreStrategy strategy, Rotation source, Rotation parent) {
        float pitch = source.pitch - parent.pitch;
        float yaw = source.yaw - parent.yaw;
        float roll = source.roll - parent.roll;
        if (strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_PITCH_ONLY) {
            return new Rotation(pitch, 0.0F, 0.0F);
        }
        if (strategy == ArmRestoreStrategy.SOURCE_POSE_SLEEVES_CUSTOM_ARM_PARENT_DELTA_NO_ROLL) {
            return new Rotation(pitch, yaw, 0.0F);
        }
        return new Rotation(pitch, yaw, roll);
    }

    private static boolean usesScopedRenderMutation(CompatConfig config) {
        if (config == null || !config.enabled) {
            return false;
        }

        ArmMode mode = config.armMode();
        return mode == ArmMode.VISUAL_PROBE_EXAGGERATED_ARMS
                || mode == ArmMode.VISUAL_PROBE_ALL_ARM_LIKE_PARTS
                || mode == ArmMode.VISUAL_PROBE_AFTER_SET_ANGLES_TAIL
                || mode == ArmMode.VISUAL_PROBE_AT_ACTUAL_MODEL_RENDER
                || mode == ArmMode.RESTORE_VPB_ARMS_AFTER_SET_ANGLES_TAIL;
    }

    private static boolean isLegacyRestoreMode(CompatConfig config) {
        return config != null && config.enabled && config.armMode() == ArmMode.RESTORE_MODEL_SETUP_ARMS_AFTER_RENDER_SETUP;
    }

    private static boolean isFirstPerson() {
        MinecraftClient client = MinecraftClient.getInstance();
        Perspective perspective = client.options.getPerspective();
        return perspective != null && perspective.isFirstPerson();
    }

    private static boolean isLikelyGunArmPose(Rotation right, Rotation left) {
        if (!right.isFinite() || !left.isFinite()) {
            return false;
        }

        float totalMotion = Math.abs(right.pitch) + Math.abs(right.yaw) + Math.abs(right.roll)
                + Math.abs(left.pitch) + Math.abs(left.yaw) + Math.abs(left.roll);
        if (totalMotion < 0.2F) {
            return false;
        }

        return right.pitch < -0.45F || left.pitch < -0.45F;
    }

    private static void cleanupActive(String stage) {
        RenderMutationContext context = ACTIVE_RENDER.get();
        if (context == null) {
            return;
        }

        int restored = context.restoreOriginals();
        if (restored > 0) {
            status = "cleanup";
            logCleanup(VpbFpaCompatClient.config(), currentTick(), stage, restored);
        }
        ACTIVE_RENDER.remove();
    }

    private static void clear(String reason) {
        snapshot = null;
        status = reason;
    }

    private static void logCapture(CompatConfig config, int tick, ArmSnapshot snapshot) {
        if (!config.debugLogging || !intervalElapsed(tick, lastCaptureLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA ArmFix] captured stage=vanilla_or_model_setup_tail item={} hand={} rightArm={} leftArm={} rightSleeve={} leftSleeve={}",
                snapshot.itemId,
                snapshot.hand,
                snapshot.rightArm,
                snapshot.leftArm,
                snapshot.rightSleeve,
                snapshot.leftSleeve
        );
        lastCaptureLogTick = tick;
    }

    private static void logRealRestore(CompatConfig config, int tick, String stage, String itemId, Rotation rightSource, TransformState rightEmfBefore, TransformState rightEmfAfter, List<TouchedPart> touchedParts, ArmRestoreLayerMode layerMode, ArmRestoreStrategy strategy, boolean partial) {
        if (config == null || !config.debugLogging || !intervalElapsed(tick, lastRestoreLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        if (partial) {
            VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] restored partial=true reason=missing_emf_candidates stage={} item={} layerMode={} strategy={} sourceMode={} parts={}", stage, itemId, layerMode.id(), strategy.id(), config.armRestoreSourceMode().id(), touchedParts.size());
        }

        if (config.armRestoreDebugCompare) {
            VpbFpaCompatClient.LOGGER.info(
                    "[VPB-FPA ArmFix] restored stage={} item={} layerMode={} strategy={} sourceMode={} rightArmSource={} rightEmfBefore={} rightEmfAfter={} parts={} partial={}",
                    stage,
                    itemId,
                    layerMode.id(),
                    strategy.id(),
                    config.armRestoreSourceMode().id(),
                    rightSource,
                    rightEmfBefore == null ? "missing" : rightEmfBefore,
                    rightEmfAfter == null ? "missing" : rightEmfAfter,
                    touchedParts.size(),
                    partial
            );
            for (TouchedPart touchedPart : touchedParts) {
                VpbFpaCompatClient.LOGGER.info(
                        "[VPB-FPA ArmFix] touched part={} kind={} class={} before={} after={}",
                        touchedPart.path(),
                        touchedPart.kind(),
                        touchedPart.className(),
                        touchedPart.before(),
                        touchedPart.after()
                );
            }
        } else {
            VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] restored stage={} item={} layerMode={} strategy={} sourceMode={} parts={} partial={}", stage, itemId, layerMode.id(), strategy.id(), config.armRestoreSourceMode().id(), touchedParts.size(), partial);
        }
        lastRestoreLogTick = tick;
    }

    private static void logObserve(CompatConfig config, int tick, String stage, String itemId) {
        if (config == null || !config.debugLogging || !intervalElapsed(tick, lastRestoreLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] observe_only stage={} item={} layerMode={} strategy={} sourceMode={}", stage, itemId, config.armRestoreLayerMode().id(), config.armRestoreStrategy().id(), config.armRestoreSourceMode().id());
        lastRestoreLogTick = tick;
    }

    private static void logStrategyTouch(CompatConfig config, int tick, String stage, String itemId, String action, List<TouchedPart> touchedParts) {
        if (config == null || !config.debugLogging || !intervalElapsed(tick, lastRestoreLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] strategy_action={} stage={} item={} layerMode={} strategy={} sourceMode={} parts={}", action, stage, itemId, config.armRestoreLayerMode().id(), config.armRestoreStrategy().id(), config.armRestoreSourceMode().id(), touchedParts.size());
        if (config.armRestoreDebugCompare) {
            for (TouchedPart touchedPart : touchedParts) {
                VpbFpaCompatClient.LOGGER.info(
                        "[VPB-FPA ArmFix] touched part={} kind={} class={} before={} after={}",
                        touchedPart.path(),
                        touchedPart.kind(),
                        touchedPart.className(),
                        touchedPart.before(),
                        touchedPart.after()
                );
            }
        }
        lastRestoreLogTick = tick;
    }

    private static void logLayerCompare(CompatConfig config, int tick, String stage, PlayerEntityModel model) {
        if (config == null || !config.debugLogging || !config.armRestoreDebugCompare
                || !intervalElapsed(tick, lastLayerCompareLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(model, config.visualProbePartNameFilter);
        Rotation rightEmfArm = firstCandidateRotation(candidates, true, false);
        Rotation rightEmfSleeve = firstCandidateRotation(candidates, true, true);
        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA ArmFix] layer_compare stage={} rightArm={} rightSleeve={} rightEmfArm={} rightEmfSleeve={}",
                stage,
                Rotation.from(model.rightArm),
                Rotation.from(model.rightSleeve),
                rightEmfArm == null ? "missing" : rightEmfArm,
                rightEmfSleeve == null ? "missing" : rightEmfSleeve
        );
        lastLayerCompareLogTick = tick;
    }

    private static void logTransformCompare(CompatConfig config, int tick, String stage, DetectionResult detection, PlayerEntityModel model) {
        if (config == null || !config.poseTracing || !config.poseTraceLogging || model == null || detection == null) {
            return;
        }

        int lastTick = LAST_TRANSFORM_COMPARE_TICK.getOrDefault(stage, Integer.MIN_VALUE);
        if (lastTick != Integer.MIN_VALUE && tick - lastTick < config.normalizedPoseTraceIntervalTicks()) {
            return;
        }

        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(model, config.visualProbePartNameFilter);
        RenderMutationContext context = ACTIVE_RENDER.get();
        TransformState sourceRight = context != null && context.model == model && context.rightFullSource != null
                ? context.rightFullSource
                : TransformState.from(model.rightArm);
        TransformState sourceLeft = context != null && context.model == model && context.leftFullSource != null
                ? context.leftFullSource
                : TransformState.from(model.leftArm);
        ModelPart rightEmfArm = firstCandidatePart(candidates, true, false, true);
        ModelPart leftEmfArm = firstCandidatePart(candidates, false, false, true);
        ModelPart rightEmfSleeve = firstCandidatePart(candidates, true, true, true);
        ModelPart leftEmfSleeve = firstCandidatePart(candidates, false, true, true);

        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA TransformCompare] stage={} tick={} item={} active={} sourceMode={} layerMode={} strategy={} sourceRightArm={} sourceLeftArm={} vanillaRightArm={} vanillaLeftArm={} vanillaRightSleeve={} vanillaLeftSleeve={} emfRightArm={} emfLeftArm={} emfRightSleeve={} emfLeftSleeve={}",
                stage,
                tick,
                detection.active() ? detection.itemId() : "none",
                detection.active(),
                config.armRestoreSourceMode().id(),
                config.armRestoreLayerMode().id(),
                config.armRestoreStrategy().id(),
                sourceRight,
                sourceLeft,
                TransformState.from(model.rightArm),
                TransformState.from(model.leftArm),
                TransformState.from(model.rightSleeve),
                TransformState.from(model.leftSleeve),
                rightEmfArm == null ? "missing" : TransformState.from(rightEmfArm),
                leftEmfArm == null ? "missing" : TransformState.from(leftEmfArm),
                rightEmfSleeve == null ? "missing" : TransformState.from(rightEmfSleeve),
                leftEmfSleeve == null ? "missing" : TransformState.from(leftEmfSleeve)
        );
        LAST_TRANSFORM_COMPARE_TICK.put(stage, tick);
    }

    private static void logAuthorityTrace(CompatConfig config, int tick, String stage, DetectionResult detection, PlayerEntityModel model) {
        if (config == null || !config.debugLogging || model == null || detection == null
                || !intervalElapsed(tick, lastAuthorityTraceLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(model, config.visualProbePartNameFilter);
        ModelPart rightEmfArm = firstCandidatePart(candidates, true, false, true);
        ModelPart leftEmfArm = firstCandidatePart(candidates, false, false, true);
        ModelPart rightEmfSleeve = firstCandidatePart(candidates, true, true, true);
        ModelPart leftEmfSleeve = firstCandidatePart(candidates, false, true, true);
        TransformState rightSource = TransformState.from(model.rightArm);
        TransformState leftSource = TransformState.from(model.leftArm);
        RenderMutationContext context = ACTIVE_RENDER.get();
        if (context != null && context.model == model) {
            if (context.rightFullSource != null) {
                rightSource = context.rightFullSource;
            }
            if (context.leftFullSource != null) {
                leftSource = context.leftFullSource;
            }
        }

        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA AuthorityTrace] stage={} strategy={} sourceMode={} item={} baseArmVisible=R:{} L:{} emfCustomArmVisible=R:{} L:{} sleeveVisible=R:{} L:{} emfSleeveVisible=R:{} L:{} baseArmRot=R:{} L:{} emfCustomArmRot=R:{} L:{} sleeveRot=R:{} L:{} emfSleeveRot=R:{} L:{} sourceRot=R:{} L:{}",
                stage,
                config.armRestoreStrategy().id(),
                config.armRestoreSourceMode().id(),
                detection.active() ? detection.itemId() : "none",
                visibleState(model.rightArm),
                visibleState(model.leftArm),
                visibleState(rightEmfArm),
                visibleState(leftEmfArm),
                visibleState(model.rightSleeve),
                visibleState(model.leftSleeve),
                visibleState(rightEmfSleeve),
                visibleState(leftEmfSleeve),
                Rotation.from(model.rightArm),
                Rotation.from(model.leftArm),
                missingRotation(rightEmfArm),
                missingRotation(leftEmfArm),
                Rotation.from(model.rightSleeve),
                Rotation.from(model.leftSleeve),
                missingRotation(rightEmfSleeve),
                missingRotation(leftEmfSleeve),
                rightSource.rotation(),
                leftSource.rotation()
        );
        lastAuthorityTraceLogTick = tick;
    }

    private static void logAuthorityHide(CompatConfig config, int tick, String path) {
        if (config == null || !config.debugLogging) {
            return;
        }

        String key = "authority_hide:" + path;
        int lastTick = LAST_PART_TRACE_TICK.getOrDefault(key, Integer.MIN_VALUE);
        if (lastTick != Integer.MIN_VALUE && tick - lastTick < config.normalizedLogIntervalTicks()) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] authority_hide part={} reason=wrong_inner_arm_layer", path);
        LAST_PART_TRACE_TICK.put(key, tick);
    }

    private static void logDeltaTrace(CompatConfig config, int tick, String path, ArmRestoreStrategy strategy, Rotation source, Rotation parent, Rotation delta, Rotation before, Rotation after) {
        if (config == null || !config.debugLogging || !intervalElapsed(tick, lastDeltaTraceLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA DeltaTrace] part={} strategy={} source={} parent={} delta={} before={} after={}",
                path,
                strategy.id(),
                source,
                parent,
                delta,
                before,
                after
        );
        lastDeltaTraceLogTick = tick;
    }

    private static void logLockCalibration(CompatConfig config, int tick, DetectionResult detection, ArmRestoreStrategy strategy, FullSource rawSource, FullSource calibratedSource, FullSource sleeveSource) {
        if (config == null || !config.debugLogging || !usesLockStrategy(strategy)
                || !intervalElapsed(tick, lastLockCalibrationLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA LockCalibration] item={} strategy={} rightSource={} rightArmCalibrated={} rightSleeveCalibrated={} rightArmOffsets={} rightSleeveOffsets={} leftSource={} leftArmCalibrated={} leftSleeveCalibrated={} leftArmOffsets={} leftSleeveOffsets={} customArmMode={} heldItemBridge={}",
                detection == null || !detection.active() ? "none" : detection.itemId(),
                strategy.id(),
                rawSource.right().rotation(),
                calibratedSource.right().rotation(),
                sleeveSource.right().rotation(),
                offsetString(config.armLockRightPitchOffset, config.armLockRightYawOffset, config.armLockRightRollOffset),
                offsetString(config.armLockRightSleevePitchOffset, config.armLockRightSleeveYawOffset, config.armLockRightSleeveRollOffset),
                rawSource.left().rotation(),
                calibratedSource.left().rotation(),
                sleeveSource.left().rotation(),
                offsetString(config.armLockLeftPitchOffset, config.armLockLeftYawOffset, config.armLockLeftRollOffset),
                offsetString(config.armLockLeftSleevePitchOffset, config.armLockLeftSleeveYawOffset, config.armLockLeftSleeveRollOffset),
                config.armLockCustomArmMode().id(),
                lockPoseBridge == null ? "none" : "available"
        );
        lastLockCalibrationLogTick = tick;
    }

    private static void logSleeveSync(CompatConfig config, int tick, String stage, DetectionResult detection, CandidateInfo info, Rotation source, Rotation parentFinal, Rotation sleeveAfter, List<TouchedPart> touchedParts) {
        if (config == null || !config.debugLogging
                || !intervalElapsed(tick, lastSleeveSyncLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        String side = info.left() ? "left" : "right";
        String offsets = info.left()
                ? offsetString(config.armLockLeftSleevePitchOffset, config.armLockLeftSleeveYawOffset, config.armLockLeftSleeveRollOffset)
                : offsetString(config.armLockRightSleevePitchOffset, config.armLockRightSleeveYawOffset, config.armLockRightSleeveRollOffset);
        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA SleeveSync] mode={} stage={} item={} side={} source={} parentFinal={} sleeveAfter={} sleeveOffsets={} parts={}",
                config.armLockSleeveSyncMode().id(),
                stage,
                detection == null || !detection.active() ? "none" : detection.itemId(),
                side,
                source,
                parentFinal,
                sleeveAfter,
                offsets,
                touchedParts.size()
        );
        if (config.armRestoreDebugCompare) {
            for (TouchedPart touchedPart : touchedParts) {
                VpbFpaCompatClient.LOGGER.info(
                        "[VPB-FPA SleeveSync] touched part={} kind={} class={} before={} after={}",
                        touchedPart.path(),
                        touchedPart.kind(),
                        touchedPart.className(),
                        touchedPart.before(),
                        touchedPart.after()
                );
            }
        }
        lastSleeveSyncLogTick = tick;
    }

    private static String offsetString(float pitch, float yaw, float roll) {
        return String.format(Locale.ROOT, "(%.3f,%.3f,%.3f)", finiteOffset(pitch), finiteOffset(yaw), finiteOffset(roll));
    }

    private static void logLockTrace(CompatConfig config, int tick, String stage, DetectionResult detection, String part, Rotation source, Rotation parentBefore, Rotation parentAfter, Rotation customBefore, Rotation customAfter, ArmRestoreStrategy strategy) {
        if (config == null || !config.debugLogging || !intervalElapsed(tick, lastLockTraceLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA LockTrace] item={} strategy={} freeze={} customArmMode={} stage={} part={} source={} parentBefore={} applied={} customBefore={} customAfter={}",
                detection == null || !detection.active() ? "none" : detection.itemId(),
                strategy.id(),
                config.armLockFreezeWhileHoldingGun,
                config.armLockCustomArmMode().id(),
                stage,
                part,
                source,
                parentBefore,
                parentAfter,
                customBefore == null ? "missing" : customBefore,
                customAfter == null ? "missing" : customAfter
        );
        lastLockTraceLogTick = tick;
    }

    private static void logSyncTrace(CompatConfig config, int tick, String stage, DetectionResult detection, PlayerEntityModel model, String heldItemState) {
        if (config == null || !config.poseTracing || !config.poseTraceLogging || model == null || detection == null) {
            return;
        }

        int lastTick = LAST_SYNC_TRACE_TICK.getOrDefault(stage, Integer.MIN_VALUE);
        if (lastTick != Integer.MIN_VALUE && tick - lastTick < config.normalizedPoseTraceIntervalTicks()) {
            return;
        }

        List<ModelPartDumper.NamedPart> candidates = ModelPartDumper.findArmLikeParts(model, config.visualProbePartNameFilter);
        logAttachmentInfo(config, candidates);
        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA SyncTrace] stage={} tick={} item={} active={} hand={} layerMode={} strategy={} sourceMode={} {} heldItem={} ",
                stage,
                tick,
                detection.active() ? detection.itemId() : "none",
                detection.active(),
                detection.hand().id(),
                config.armRestoreLayerMode().id(),
                config.armRestoreStrategy().id(),
                config.armRestoreSourceMode().id(),
                syncSummary(model, candidates),
                heldItemState
        );
        logTransformCompare(config, tick, stage, detection, model);
        LAST_SYNC_TRACE_TICK.put(stage, tick);
    }

    private static void logPartTrace(CompatConfig config, int tick, String stage, RenderMutationContext context, PartMatch match) {
        if (config == null || !config.poseTracing || !config.poseTraceLogging) {
            return;
        }

        String key = stage + ":" + match.namedPart().path();
        int lastTick = LAST_PART_TRACE_TICK.getOrDefault(key, Integer.MIN_VALUE);
        if (lastTick != Integer.MIN_VALUE && tick - lastTick < config.normalizedPoseTraceIntervalTicks()) {
            return;
        }

        TransformState transform = TransformState.from(match.namedPart().part());
        ModelPartDumper.PartMetadata metadata = ModelPartDumper.metadata(match.namedPart());
        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA PartTrace] stage={} part={} kind={} class={} rot={} item={} layerMode={} strategy={} sourceMode={} emfCustom={}",
                stage,
                match.namedPart().path(),
                match.info().kind(),
                match.namedPart().part().getClass().getName(),
                transform.rotation(),
                context.detection.active() ? context.detection.itemId() : "none",
                config.armRestoreLayerMode().id(),
                config.armRestoreStrategy().id(),
                config.armRestoreSourceMode().id(),
                match.info().emfCustom()
        );
        VpbFpaCompatClient.LOGGER.info(
                "[VPB-FPA TransformTrace] stage={} part={} kind={} class={} {} empty={} cuboids={} children={} attachedTo={} attach={} attachments={} vanillaChildren={} item={} sourceMode={} layerMode={} strategy={} emfCustom={}",
                stage,
                match.namedPart().path(),
                match.info().kind(),
                match.namedPart().part().getClass().getName(),
                transform,
                match.namedPart().part().isEmpty(),
                ModelPartDumper.cuboidCount(match.namedPart().part()),
                ModelPartDumper.childCount(match.namedPart().part()),
                metadata.attachedTo(),
                metadata.attach(),
                metadata.attachmentCount(),
                metadata.vanillaChildrenCount(),
                context.detection.active() ? context.detection.itemId() : "none",
                config.armRestoreSourceMode().id(),
                config.armRestoreLayerMode().id(),
                config.armRestoreStrategy().id(),
                match.info().emfCustom()
        );
        LAST_PART_TRACE_TICK.put(key, tick);
    }

    private static void logAttachmentInfo(CompatConfig config, List<ModelPartDumper.NamedPart> candidates) {
        if (config == null || !config.poseTracing || !config.poseTraceLogging) {
            return;
        }

        for (ModelPartDumper.NamedPart candidate : candidates) {
            CandidateInfo info = CandidateInfo.from(candidate);
            if (!info.emfCustom() || (!info.arm() && !info.sleeve())) {
                continue;
            }
            if (!LOGGED_ATTACHMENT_PARTS.add(candidate.path())) {
                continue;
            }

            ModelPartDumper.PartMetadata metadata = ModelPartDumper.metadata(candidate);
            VpbFpaCompatClient.LOGGER.info(
                    "[VPB-FPA SyncTrace] part={} parent=unknown attachedTo={} attach={} attachments={} vanillaChildren={} inherits=unknown class={}",
                    candidate.path(),
                    metadata.attachedTo(),
                    metadata.attach(),
                    metadata.attachmentCount(),
                    metadata.vanillaChildrenCount(),
                    candidate.part().getClass().getName()
            );
        }
    }

    private static String syncSummary(PlayerEntityModel model) {
        return syncSummary(model, ModelPartDumper.findArmLikeParts(model, ""));
    }

    private static String syncSummary(PlayerEntityModel model, List<ModelPartDumper.NamedPart> candidates) {
        return "vRightArm=" + Rotation.from(model.rightArm)
                + " vLeftArm=" + Rotation.from(model.leftArm)
                + " vRightSleeve=" + Rotation.from(model.rightSleeve)
                + " vLeftSleeve=" + Rotation.from(model.leftSleeve)
                + " emfRightArm=" + missing(firstCandidateRotation(candidates, true, false))
                + " emfLeftArm=" + missing(firstCandidateRotation(candidates, false, false))
                + " emfRightSleeve=" + missing(firstCandidateRotation(candidates, true, true))
                + " emfLeftSleeve=" + missing(firstCandidateRotation(candidates, false, true));
    }

    private static String missing(Rotation rotation) {
        return rotation == null ? "missing" : rotation.toString();
    }

    private static String missingRotation(ModelPart part) {
        return part == null ? "missing" : Rotation.from(part).toString();
    }

    private static String visibleState(ModelPart part) {
        if (part == null) {
            return "missing";
        }
        return part.visible && !part.hidden ? "visible" : "visible=" + part.visible + ",hidden=" + part.hidden;
    }

    private static Rotation firstCandidateRotation(List<ModelPartDumper.NamedPart> candidates, boolean right, boolean sleeve) {
        ModelPart part = firstCandidatePart(candidates, right, sleeve, true);
        return part == null ? null : Rotation.from(part);
    }

    private static ModelPart firstCandidatePart(List<ModelPartDumper.NamedPart> candidates, boolean right, boolean sleeve, Boolean emfCustomRequired) {
        for (ModelPartDumper.NamedPart candidate : candidates) {
            CandidateInfo info = CandidateInfo.from(candidate);
            if (info.right() == right && info.sleeve() == sleeve && (sleeve || info.arm())
                    && (emfCustomRequired == null || info.emfCustom() == emfCustomRequired)) {
                return candidate.part();
            }
        }
        return null;
    }

    private static boolean selectedByLayerMode(CandidateInfo info, ArmRestoreLayerMode layerMode) {
        if (!info.arm() && !info.sleeve()) {
            return false;
        }
        if (!info.left() && !info.right()) {
            return false;
        }

        return switch (layerMode) {
            case ARMS_AND_SLEEVES -> true;
            case ARMS_ONLY -> info.arm();
            case SLEEVES_ONLY -> info.sleeve();
            case VANILLA_ONLY -> !info.emfCustom();
            case EMF_CUSTOM_ONLY -> info.emfCustom();
            case EMF_CUSTOM_ARMS_ONLY -> info.emfCustom() && info.arm();
            case EMF_CUSTOM_SLEEVES_ONLY -> info.emfCustom() && info.sleeve();
        };
    }

    private static int expectedCustomCandidates(ArmRestoreLayerMode layerMode, ArmRestoreStrategy strategy) {
        if (strategy == ArmRestoreStrategy.RESTORE_ARMS_THEN_SYNC_SLEEVES_LATE) {
            return switch (layerMode) {
                case ARMS_AND_SLEEVES, ARMS_ONLY, EMF_CUSTOM_ONLY, EMF_CUSTOM_ARMS_ONLY -> 2;
                case SLEEVES_ONLY, EMF_CUSTOM_SLEEVES_ONLY, VANILLA_ONLY -> 0;
            };
        }

        return switch (layerMode) {
            case ARMS_AND_SLEEVES, EMF_CUSTOM_ONLY -> 4;
            case ARMS_ONLY, SLEEVES_ONLY, EMF_CUSTOM_ARMS_ONLY, EMF_CUSTOM_SLEEVES_ONLY -> 2;
            case VANILLA_ONLY -> 0;
        };
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

    private static void logCleanup(CompatConfig config, int tick, String stage, int parts) {
        if (config == null || !config.debugLogging || !intervalElapsed(tick, lastCleanupLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] cleanup restored_originals stage={} parts={}", stage, parts);
        lastCleanupLogTick = tick;
    }

    private static void logSkip(CompatConfig config, int tick, String reason) {
        if (config == null || !config.debugLogging || !intervalElapsed(tick, lastSkipLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] skipped reason={}", reason);
        lastSkipLogTick = tick;
    }

    private static void logCleared(CompatConfig config, int tick) {
        if (config == null || !config.debugLogging || !intervalElapsed(tick, lastSkipLogTick, config.normalizedLogIntervalTicks())) {
            return;
        }

        VpbFpaCompatClient.LOGGER.info("[VPB-FPA ArmFix] cleared reason=inactive_or_item_changed");
        lastSkipLogTick = tick;
    }

    private static boolean intervalElapsed(int tick, int lastTick, int interval) {
        return lastTick == Integer.MIN_VALUE || tick - lastTick >= interval;
    }

    private static int currentTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player == null ? 0 : client.player.age;
    }

    private record MutationRequest(
            RenderMutationContext context,
            DetectionResult detection,
            PlayerEntityModel model,
            int tick
    ) {
    }

    private record RestoreResult(
            List<TouchedPart> touchedParts,
            TransformState rightEmfBefore,
            TransformState rightEmfAfter,
            boolean partial
    ) {
    }

    private record PartMatch(ModelPartDumper.NamedPart namedPart, CandidateInfo info) {
    }

    private record CandidateInfo(
            String path,
            String kind,
            boolean arm,
            boolean sleeve,
            boolean left,
            boolean right,
            boolean emfCustom
    ) {
        static CandidateInfo from(ModelPartDumper.NamedPart candidate) {
            String path = candidate.path();
            String lowerPath = path.toLowerCase(Locale.ROOT);
            String className = candidate.part().getClass().getName().toLowerCase(Locale.ROOT);
            boolean sleeve = lowerPath.contains("sleeve");
            boolean arm = lowerPath.contains("arm") && !sleeve;
            boolean left = lowerPath.contains("left");
            boolean right = lowerPath.contains("right");
            boolean emfCustom = lowerPath.contains("emf_") || className.contains("emfmodelpartcustom");
            String kind = sleeve ? "sleeve" : arm ? "arm" : "other";
            return new CandidateInfo(path, kind, arm, sleeve, left, right, emfCustom);
        }
    }

    private record TouchedPart(String path, String kind, String className, TransformState before, TransformState after) {
    }

    private record ArmSnapshot(
            int entityId,
            int tick,
            String itemId,
            String hand,
            boolean local,
            Rotation rightArm,
            Rotation leftArm,
            Rotation rightSleeve,
            Rotation leftSleeve
    ) {
        static ArmSnapshot capture(int entityId, int tick, String itemId, String hand, boolean local, PlayerEntityModel model) {
            return new ArmSnapshot(
                    entityId,
                    tick,
                    itemId,
                    hand,
                    local,
                    Rotation.from(model.rightArm),
                    Rotation.from(model.leftArm),
                    Rotation.from(model.rightSleeve),
                    Rotation.from(model.leftSleeve)
            );
        }
    }

    private record Rotation(float pitch, float yaw, float roll) {
        private static final Rotation ZERO = new Rotation(0.0F, 0.0F, 0.0F);

        static Rotation from(ModelPart part) {
            return part == null ? new Rotation(0.0F, 0.0F, 0.0F) : new Rotation(part.pitch, part.yaw, part.roll);
        }

        boolean isFinite() {
            return Float.isFinite(pitch) && Float.isFinite(yaw) && Float.isFinite(roll);
        }

        void applyTo(ModelPart part) {
            if (part == null) {
                return;
            }

            part.pitch = pitch;
            part.yaw = yaw;
            part.roll = roll;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "(%.3f,%.3f,%.3f)", pitch, yaw, roll);
        }
    }

    private record TransformState(
            float pitch,
            float yaw,
            float roll,
            float originX,
            float originY,
            float originZ,
            float xScale,
            float yScale,
            float zScale,
            boolean visible,
            boolean hidden
    ) {
        static TransformState from(ModelPart part) {
            if (part == null) {
                return new TransformState(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, true, false);
            }
            return new TransformState(
                    part.pitch,
                    part.yaw,
                    part.roll,
                    part.originX,
                    part.originY,
                    part.originZ,
                    part.xScale,
                    part.yScale,
                    part.zScale,
                    part.visible,
                    part.hidden
            );
        }

        Rotation rotation() {
            return new Rotation(pitch, yaw, roll);
        }

        boolean isFinite() {
            return Float.isFinite(pitch)
                    && Float.isFinite(yaw)
                    && Float.isFinite(roll)
                    && Float.isFinite(originX)
                    && Float.isFinite(originY)
                    && Float.isFinite(originZ)
                    && Float.isFinite(xScale)
                    && Float.isFinite(yScale)
                    && Float.isFinite(zScale);
        }

        TransformState withHidden(boolean value) {
            return new TransformState(pitch, yaw, roll, originX, originY, originZ, xScale, yScale, zScale, visible, value);
        }

        TransformState withRotationOffset(float pitchOffset, float yawOffset, float rollOffset) {
            return new TransformState(
                    pitch + pitchOffset,
                    yaw + yawOffset,
                    roll + rollOffset,
                    originX,
                    originY,
                    originZ,
                    xScale,
                    yScale,
                    zScale,
                    visible,
                    hidden
            );
        }

        void applyRotationTo(ModelPart part) {
            if (part == null) {
                return;
            }
            part.pitch = pitch;
            part.yaw = yaw;
            part.roll = roll;
        }

        void applyFullTo(ModelPart part) {
            if (part == null) {
                return;
            }
            part.pitch = pitch;
            part.yaw = yaw;
            part.roll = roll;
            part.originX = originX;
            part.originY = originY;
            part.originZ = originZ;
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
            part.visible = visible;
            part.hidden = hidden;
        }

        @Override
        public String toString() {
            return String.format(
                    Locale.ROOT,
                    "rot=(%.3f,%.3f,%.3f) pivot=(%.3f,%.3f,%.3f) scale=(%.3f,%.3f,%.3f) visible=%s hidden=%s",
                    pitch,
                    yaw,
                    roll,
                    originX,
                    originY,
                    originZ,
                    xScale,
                    yScale,
                    zScale,
                    visible,
                    hidden
            );
        }
    }

    private record FullSource(TransformState right, TransformState left) {
    }

    private record FreezePose(String itemId, TransformState rightSource, TransformState leftSource) {
    }

    private record LockPoseBridge(
            int entityId,
            int tick,
            String itemId,
            PlayerEntityModel model,
            TransformState rightSource,
            TransformState leftSource
    ) {
    }

    private record HeldItemLockContext(ModelPart part, TransformState original) {
    }

    private static final class RenderMutationContext {
        private final PlayerEntityModel model;
        private final int entityId;
        private final Map<ModelPart, TransformState> originals = new IdentityHashMap<>();
        private String itemId;
        private DetectionResult detection;
        private Rotation rightSource;
        private Rotation leftSource;
        private TransformState rightFullSource;
        private TransformState leftFullSource;
        private TransformState rightSleeveFullSource;
        private TransformState leftSleeveFullSource;

        private RenderMutationContext(PlayerEntityModel model, int entityId) {
            this.model = model;
            this.entityId = entityId;
        }

        private void saveOriginal(ModelPart part) {
            if (part != null && !originals.containsKey(part)) {
                originals.put(part, TransformState.from(part));
            }
        }

        private int restoreOriginals() {
            List<Map.Entry<ModelPart, TransformState>> entries = new ArrayList<>(originals.entrySet());
            for (Map.Entry<ModelPart, TransformState> entry : entries) {
                entry.getValue().applyFullTo(entry.getKey());
            }
            int restored = entries.size();
            originals.clear();
            return restored;
        }
    }
}
