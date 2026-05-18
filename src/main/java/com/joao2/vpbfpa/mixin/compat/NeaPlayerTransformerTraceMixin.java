package com.joao2.vpbfpa.mixin.compat;

import com.joao2.vpbfpa.trace.PoseTraceLogger;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.tr7zw.notenoughanimations.logic.PlayerTransformer", remap = false)
public abstract class NeaPlayerTransformerTraceMixin {
    @Inject(method = "updateModel", at = @At("HEAD"), remap = false)
    private void vpbfpa$traceNeaBefore(AbstractClientPlayerEntity player, PlayerEntityModel model, float tickDelta, CallbackInfo originalCallback, CallbackInfo callback) {
        PoseTraceLogger.hookReached("nea_updateModel_head modelClass=" + className(model), model != null);
        PoseTraceLogger.trace("nea_candidate_before", model, player);
    }

    @Inject(method = "updateModel", at = @At("RETURN"), remap = false)
    private void vpbfpa$traceNeaAfter(AbstractClientPlayerEntity player, PlayerEntityModel model, float tickDelta, CallbackInfo originalCallback, CallbackInfo callback) {
        PoseTraceLogger.hookReached("nea_updateModel_tail modelClass=" + className(model), model != null);
        PoseTraceLogger.trace("nea_candidate_after", model, player);
    }

    private static String className(Object object) {
        return object == null ? "null" : object.getClass().getName();
    }
}
