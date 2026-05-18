package com.joao2.vpbfpa.detect;

import com.joao2.vpbfpa.config.CompatConfig;
import com.joao2.vpbfpa.config.DetectionMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public final class VpbWeaponDetector {
    private static final String POINTBLANK_NAMESPACE = "pointblank";

    private final EnvironmentProbe environmentProbe;

    public VpbWeaponDetector(EnvironmentProbe environmentProbe) {
        this.environmentProbe = environmentProbe;
    }

    public DetectionResult detect(PlayerEntity player, CompatConfig config) {
        if (!config.enabled || player == null) {
            return DetectionResult.inactive();
        }

        DetectionResult mainHand = detectStack(player.getMainHandStack(), DetectedHand.MAIN, config);
        if (mainHand.active()) {
            return mainHand;
        }

        if (config.includeOffhand) {
            DetectionResult offhand = detectStack(player.getOffHandStack(), DetectedHand.OFFHAND, config);
            if (offhand.active()) {
                return offhand;
            }
        }

        return DetectionResult.inactive();
    }

    public DetectionResult detect(PlayerEntityRenderState state, CompatConfig config) {
        if (!config.enabled || state == null) {
            return DetectionResult.inactive();
        }

        ItemStack mainHandStack = state.getMainHandItemStack();
        DetectionResult mainHand = detectStack(mainHandStack, DetectedHand.MAIN, config);
        if (mainHand.active()) {
            return mainHand;
        }

        if (config.includeOffhand) {
            ItemStack offhandStack = state.mainArm == Arm.RIGHT ? state.leftHandItem : state.rightHandItem;
            DetectionResult offhand = detectStack(offhandStack, DetectedHand.OFFHAND, config);
            if (offhand.active()) {
                return offhand;
            }
        }

        return DetectionResult.inactive();
    }

    private DetectionResult detectStack(ItemStack stack, DetectedHand hand, CompatConfig config) {
        if (stack == null || stack.isEmpty()) {
            return DetectionResult.inactive();
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        DetectionMode mode = config.detectionMode();

        if (mode == DetectionMode.CLASS || mode == DetectionMode.AUTO) {
            if (environmentProbe.gunItemClassFound()) {
                if (environmentProbe.gunItemClass().isInstance(stack.getItem())) {
                    return new DetectionResult(true, hand, itemId.toString(), DetectionMethod.CLASS, false);
                }
                return DetectionResult.inactive();
            }

            if (mode == DetectionMode.CLASS) {
                return DetectionResult.inactive();
            }
        }

        if ((mode == DetectionMode.AUTO && !environmentProbe.gunItemClassFound()) || mode == DetectionMode.REGISTRY) {
            if (POINTBLANK_NAMESPACE.equals(itemId.getNamespace())) {
                return new DetectionResult(true, hand, itemId.toString(), DetectionMethod.REGISTRY_FALLBACK, true);
            }
        }

        return DetectionResult.inactive();
    }
}
