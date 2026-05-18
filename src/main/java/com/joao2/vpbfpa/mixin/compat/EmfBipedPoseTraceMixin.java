package com.joao2.vpbfpa.mixin.compat;

import com.joao2.vpbfpa.trace.PoseTraceLogger;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "traben.entity_model_features.models.animation.state.EMFBipedPose", remap = false)
public abstract class EmfBipedPoseTraceMixin {
    @Inject(method = "applyTo", at = @At("HEAD"), remap = false)
    private void vpbfpa$traceEmfBefore(BipedEntityModel<?> model, CallbackInfo ci) {
        PoseTraceLogger.hookReached("emf_applyTo_head modelClass=" + className(model), model instanceof PlayerEntityModel);
        if (model instanceof PlayerEntityModel playerModel) {
            PoseTraceLogger.traceCurrent("emf_candidate_before", playerModel);
        }
    }

    @Inject(method = "applyTo", at = @At("RETURN"), remap = false)
    private void vpbfpa$traceEmfAfter(BipedEntityModel<?> model, CallbackInfo ci) {
        PoseTraceLogger.hookReached("emf_applyTo_tail modelClass=" + className(model), model instanceof PlayerEntityModel);
        if (model instanceof PlayerEntityModel playerModel) {
            PoseTraceLogger.traceCurrent("emf_candidate_after", playerModel);
        }
    }

    private static String className(Object object) {
        return object == null ? "null" : object.getClass().getName();
    }
}
