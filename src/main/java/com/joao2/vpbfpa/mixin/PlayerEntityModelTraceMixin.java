package com.joao2.vpbfpa.mixin;

import com.joao2.vpbfpa.arm.ArmRestoreManager;
import com.joao2.vpbfpa.trace.PoseTraceLogger;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelTraceMixin {
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("HEAD"))
    private void vpbfpa$tracePlayerModelHead(PlayerEntityRenderState state, CallbackInfo ci) {
        PoseTraceLogger.trace("player_model_set_angles_head", (PlayerEntityModel) (Object) this, state);
    }

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
    private void vpbfpa$tracePlayerModelTail(PlayerEntityRenderState state, CallbackInfo ci) {
        PlayerEntityModel model = (PlayerEntityModel) (Object) this;
        PoseTraceLogger.trace("vanilla_or_model_setup_tail", model, state);
        ArmRestoreManager.captureModelSetupArms(model, state);
        ArmRestoreManager.afterSetAnglesTail("set_angles_tail_after_probe_candidate", model, state);
        PoseTraceLogger.trace("set_angles_tail_after_probe_candidate", model, state);
    }
}
