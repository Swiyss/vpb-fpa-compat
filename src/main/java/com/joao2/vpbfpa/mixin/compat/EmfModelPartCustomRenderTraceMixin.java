package com.joao2.vpbfpa.mixin.compat;

import com.joao2.vpbfpa.arm.ArmRestoreManager;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPartCustom", remap = false)
public abstract class EmfModelPartCustomRenderTraceMixin {
    @Inject(method = "method_22699", at = @At("HEAD"), remap = false)
    private void vpbfpa$emfCustomPartRenderHead(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color, CallbackInfo ci) {
        ArmRestoreManager.partRender("emf_custom_part_render_head", (ModelPart) (Object) this);
    }

    @Inject(method = "method_22699", at = @At("TAIL"), remap = false)
    private void vpbfpa$emfCustomPartRenderTail(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color, CallbackInfo ci) {
        ArmRestoreManager.partRender("emf_custom_part_render_tail", (ModelPart) (Object) this);
    }
}
