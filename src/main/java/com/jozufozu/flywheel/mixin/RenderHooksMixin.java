package com.jozufozu.flywheel.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.math.Matrix4f;

import com.mojang.math.Vector3d;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import net.minecraft.client.renderer.LevelRenderer;

import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.phys.Vec3;

import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.OptifineHandler;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.core.crumbling.CrumblingRenderer;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.jozufozu.flywheel.fabric.event.FlywheelEvents;
import com.jozufozu.flywheel.fabric.util.MatrixUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public class RenderHooksMixin {

	@Shadow
	private ClientLevel level;

	@Shadow
	@Final
	private RenderBuffers renderBuffers;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(at = @At("HEAD"), method = "setupRender")
	private void setupRender(Camera info, Frustum frustum, boolean p_228437_3_, int frameCount, boolean isSpectator, CallbackInfo ci) {
		FlywheelEvents.BEGIN_FRAME.invoker().handleEvent(
				new BeginFrameEvent(level, info, minecraft.gameRenderer, minecraft.gameRenderer.lightTexture(), frustum));
	}

	/**
	 * JUSTIFICATION: This method is called once per layer per frame. It allows us to perform
	 * layer-correct custom rendering. RenderWorldLast is not refined enough for rendering world objects.
	 * This should probably be a forge event.
	 */
	@Inject(at = @At("TAIL"), method = "renderChunkLayer")
	private void renderLayer(RenderType type, PoseStack stack, double camX, double camY, double camZ, Matrix4f matrix4f, CallbackInfo ci) {

		RenderBuffers renderBuffers = this.renderBuffers;

		FlywheelEvents.RENDER_LAYER.invoker().handleEvent(new RenderLayerEvent(level, type, stack, renderBuffers, camX, camY, camZ));

		if (!OptifineHandler.usingShaders()) GL20.glUseProgram(0);

		renderBuffers.bufferSource().endBatch(type);
	}

	@Inject(at = @At("TAIL"), method = "allChanged")
	private void refresh(CallbackInfo ci) {
		Backend.getInstance()
				.refresh();

		FlywheelEvents.RELOAD_RENDERERS.invoker().handleEvent(new ReloadRenderersEvent(level));
	}


	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 2 // after the game renders the breaking overlay normally
	), method = "renderLevel")
	private void renderBlockBreaking(PoseStack stack, float p_228426_2_, long p_228426_3_, boolean p_228426_5_, Camera info, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f p_228426_9_, CallbackInfo ci) {
		if (!Backend.getInstance()
				.available()) return;

		Matrix4f view = stack.last()
				.pose();
		Matrix4f viewProjection = view.copy();
		MatrixUtil.multiplyBackward(viewProjection, Backend.getInstance()
												.getProjectionMatrix());

		Vec3 cameraPos = info.getPosition();
		CrumblingRenderer.renderBreaking(level, viewProjection, cameraPos.x, cameraPos.y, cameraPos.z);

		if (!OptifineHandler.usingShaders()) GL20.glUseProgram(0);
	}

	// Instancing

	@Inject(at = @At("TAIL"), method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V")
	private void checkUpdate(BlockPos pos, BlockState lastState, BlockState newState, CallbackInfo ci) {
		InstancedRenderDispatcher.getTiles(level)
				.update(level.getBlockEntity(pos));
	}
}
