package com.joao2.vpbfpa.mixin;

import com.joao2.vpbfpa.arm.ArmRestoreManager;
import com.joao2.vpbfpa.trace.PoseTraceLogger;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelCommandRenderer.class)
public abstract class ModelCommandRendererTraceMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
            at = @At("HEAD")
    )
    private void vpbfpa$traceActualModelCommandHead(OrderedRenderCommandQueueImpl.ModelCommand<?> command, RenderLayer layer, VertexConsumer vertices, OutlineVertexConsumerProvider outlineProvider, VertexConsumerProvider.Immediate immediate, CallbackInfo ci) {
        trace("actual_model_command_head", command);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;setAngles(Ljava/lang/Object;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void vpbfpa$beginActualModelRenderCommand(OrderedRenderCommandQueueImpl.ModelCommand<?> command, RenderLayer layer, VertexConsumer vertices, OutlineVertexConsumerProvider outlineProvider, VertexConsumerProvider.Immediate immediate, CallbackInfo ci) {
        Model<?> model = command.model();
        Object state = command.state();
        if (model instanceof PlayerEntityModel playerModel && state instanceof PlayerEntityRenderState playerState) {
            ArmRestoreManager.beginActualModelRenderCommand(playerModel, playerState);
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;setAngles(Ljava/lang/Object;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void vpbfpa$traceActualModelSetAnglesAfter(OrderedRenderCommandQueueImpl.ModelCommand<?> command, RenderLayer layer, VertexConsumer vertices, OutlineVertexConsumerProvider outlineProvider, VertexConsumerProvider.Immediate immediate, CallbackInfo ci) {
        trace("actual_model_set_angles_after", command);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
                    ordinal = 0,
                    shift = At.Shift.BEFORE
            )
    )
    private void vpbfpa$traceActualModelRenderHead(OrderedRenderCommandQueueImpl.ModelCommand<?> command, RenderLayer layer, VertexConsumer vertices, OutlineVertexConsumerProvider outlineProvider, VertexConsumerProvider.Immediate immediate, CallbackInfo ci) {
        applyBeforeRender("actual_model_render_head", command);
        trace("actual_model_render_head", command);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void vpbfpa$traceActualModelRenderTail(OrderedRenderCommandQueueImpl.ModelCommand<?> command, RenderLayer layer, VertexConsumer vertices, OutlineVertexConsumerProvider outlineProvider, VertexConsumerProvider.Immediate immediate, CallbackInfo ci) {
        trace("actual_model_render_tail", command);
        cleanup("actual_model_render_tail", command);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/command/OrderedRenderCommandQueueImpl$ModelCommand;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/OutlineVertexConsumerProvider;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V",
            at = @At("RETURN")
    )
    private void vpbfpa$endActualModelRenderCommand(OrderedRenderCommandQueueImpl.ModelCommand<?> command, RenderLayer layer, VertexConsumer vertices, OutlineVertexConsumerProvider outlineProvider, VertexConsumerProvider.Immediate immediate, CallbackInfo ci) {
        cleanup("actual_model_command_return", command);
    }

    private static void trace(String stage, OrderedRenderCommandQueueImpl.ModelCommand<?> command) {
        Model<?> model = command.model();
        Object state = command.state();
        if (model instanceof PlayerEntityModel playerModel && state instanceof PlayerEntityRenderState playerState) {
            PoseTraceLogger.trace(stage, playerModel, playerState);
        }
    }

    private static void applyBeforeRender(String stage, OrderedRenderCommandQueueImpl.ModelCommand<?> command) {
        Model<?> model = command.model();
        Object state = command.state();
        if (model instanceof PlayerEntityModel playerModel && state instanceof PlayerEntityRenderState playerState) {
            ArmRestoreManager.beforeActualModelRender(stage, playerModel, playerState);
        }
    }

    private static void cleanup(String stage, OrderedRenderCommandQueueImpl.ModelCommand<?> command) {
        Model<?> model = command.model();
        Object state = command.state();
        if (model instanceof PlayerEntityModel playerModel && state instanceof PlayerEntityRenderState playerState) {
            ArmRestoreManager.cleanupAfterActualModelRender(stage, playerModel, playerState);
        }
    }
}
