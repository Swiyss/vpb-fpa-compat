package com.joao2.vpbfpa.mixin;

import com.joao2.vpbfpa.arm.ArmRestoreManager;
import com.joao2.vpbfpa.trace.ModelPartDumper;
import com.joao2.vpbfpa.trace.PoseTraceLogger;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererTraceMixin {
    @Shadow
    protected EntityModel<?> model;

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
    private void vpbfpa$traceFinalRenderBefore(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        trace("final_render_before", state);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void vpbfpa$restoreBeforeSubmitModel(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        restore("before_submit_model", state);
        dumpModelParts("before_submit_model", state);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("RETURN"))
    private void vpbfpa$traceFinalRenderAfter(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        restore("render_return", state);
        trace("final_render_after", state);
    }

    private void trace(String stage, LivingEntityRenderState state) {
        if (state instanceof PlayerEntityRenderState playerState && model instanceof PlayerEntityModel playerModel) {
            PoseTraceLogger.trace(stage, playerModel, playerState);
        }
    }

    private void restore(String stage, LivingEntityRenderState state) {
        if (state instanceof PlayerEntityRenderState playerState && model instanceof PlayerEntityModel playerModel) {
            ArmRestoreManager.restore(stage, playerModel, playerState);
        }
    }

    private void dumpModelParts(String stage, LivingEntityRenderState state) {
        if (state instanceof PlayerEntityRenderState && model instanceof PlayerEntityModel playerModel) {
            ModelPartDumper.dumpOnce(playerModel, stage);
        }
    }
}
