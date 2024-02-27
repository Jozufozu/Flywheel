package com.jozufozu.flywheel.mixin;

import net.neoforged.neoforge.common.NeoForge;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.crumbling.CrumblingRenderer;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(value = LevelRenderer.class, priority = 1001) // Higher priority to go after sodium
public class LevelRendererMixin {
	@Shadow
	private ClientLevel level;

	@Shadow
	@Final
	private RenderBuffers renderBuffers;

	@Inject(at = @At("HEAD"), method = "setupRender")
	private void setupRender(Camera camera, Frustum frustum, boolean queue, boolean isSpectator, CallbackInfo ci) {
		NeoForge.EVENT_BUS.post(new BeginFrameEvent(level, camera, frustum));
	}

	@Inject(at = @At("TAIL"), method = "renderSectionLayer")
	private void renderLayer(RenderType type, PoseStack stack, double camX, double camY, double camZ, Matrix4f projection, CallbackInfo ci) {
		NeoForge.EVENT_BUS.post(new RenderLayerEvent(level, type, stack, renderBuffers, camX, camY, camZ));
	}

	@Inject(at = @At("TAIL"), method = "allChanged")
	private void refresh(CallbackInfo ci) {
		Backend.refresh();

		NeoForge.EVENT_BUS.post(new ReloadRenderersEvent(level));
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 2 // after the game renders the breaking overlay normally
	), method = "renderLevel")
	private void renderBlockBreaking(PoseStack stack, float p_228426_2_, long p_228426_3_, boolean p_228426_5_, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f p_228426_9_, CallbackInfo ci) {
		CrumblingRenderer.render(level, camera, stack);
	}
}
