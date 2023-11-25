package com.jozufozu.flywheel.mixin;

import java.util.SortedSet;

import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.api.event.BeginFrameEvent;
import com.jozufozu.flywheel.api.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.api.event.RenderContext;
import com.jozufozu.flywheel.api.event.RenderStage;
import com.jozufozu.flywheel.api.event.RenderStageEvent;
import com.jozufozu.flywheel.impl.visualization.VisualizationManagerImpl;
import com.mojang.blaze3d.vertex.PoseStack;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraftforge.common.MinecraftForge;

@Mixin(value = LevelRenderer.class, priority = 1001) // Higher priority to go after Sodium
public class LevelRendererMixin {
	@Shadow
	private ClientLevel level;

	@Shadow
	@Final
	private RenderBuffers renderBuffers;

	@Shadow
	@Final
	private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

	@Unique
	private RenderContext flywheel$renderContext;

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void flywheel$beginRender(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
		flywheel$renderContext = RenderContext.create((LevelRenderer) (Object) this, level, renderBuffers, poseStack, projectionMatrix, camera, partialTick);

		MinecraftForge.EVENT_BUS.post(new BeginFrameEvent(flywheel$renderContext));
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void flywheel$endRender(CallbackInfo ci) {
		flywheel$renderContext = null;
	}

	@Inject(method = "allChanged", at = @At("RETURN"))
	private void flywheel$refresh(CallbackInfo ci) {
		MinecraftForge.EVENT_BUS.post(new ReloadRenderersEvent(level));
	}

//	// after the game renders the breaking overlay normally
//	@Inject(method = "renderLevel",
//			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;crumblingBufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;", ordinal = 2, shift = Shift.BY, by = 2))
//	private void flywheel$renderCrumbling(CallbackInfo ci) {
//		if (flywheel$renderContext != null) {
//			// TODO: Crumbling
//		}
//	}

	// STAGE DISPATCHING

	@Unique
	private void flywheel$dispatch(RenderStage stage) {
		if (flywheel$renderContext != null) {
			MinecraftForge.EVENT_BUS.post(new RenderStageEvent(flywheel$renderContext, stage));
		}
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=sky"))
	private void flywheel$onStage$beforeSky(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.BEFORE_SKY);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=fog"))
	private void flywheel$onStage$afterSky(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_SKY);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=terrain"))
	private void flywheel$onStage$beforeTerrain(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.BEFORE_TERRAIN);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=entities"))
	private void flywheel$onStage$afterSolidTerrain(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_SOLID_TERRAIN);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;", ordinal = 0))
	private void flywheel$onStage$beforeEntities(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.BEFORE_ENTITIES);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=blockentities"))
	private void flywheel$onStage$beforeBlockEntities(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_ENTITIES);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", ordinal = 0))
	private void flywheel$onStage$afterSolidBlockEntities(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_BLOCK_ENTITIES);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V", ordinal = 0))
	private void flywheel$onStage$beforeCrumbling(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.BEFORE_CRUMBLING);
	}

	@Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;translucentTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;", opcode = Opcodes.GETFIELD, ordinal = 0))
	private void flywheel$onStage$afterFinalEndBatch$fabulous(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_FINAL_END_BATCH);
	}

	@Inject(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;particlesTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;", opcode = Opcodes.GETFIELD, ordinal = 0))
	private void flywheel$onStage$afterTranslucentTerrain$fabulous(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_TRANSLUCENT_TERRAIN);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V", ordinal = 2, shift = Shift.AFTER))
	private void flywheel$onStage$afterFinalEndBatch(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_FINAL_END_BATCH);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V", ordinal = 6, shift = Shift.AFTER))
	private void flywheel$onStage$afterTranslucentTerrain(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_TRANSLUCENT_TERRAIN);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", shift = Shift.AFTER))
	private void flywheel$onStage$afterParticles(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_PARTICLES);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V", shift = Shift.AFTER))
	private void flywheel$onStage$afterWeather(CallbackInfo ci) {
		flywheel$dispatch(RenderStage.AFTER_WEATHER);
	}

	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=destroyProgress"))
	private void flywheel$crumbling(CallbackInfo ci) {
		var vm = VisualizationManagerImpl.get(level);
		if (vm != null) {
			vm.renderCrumbling(flywheel$renderContext, destructionProgress);
		}
	}
}
