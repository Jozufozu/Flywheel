package com.jozufozu.flywheel.core.crumbling;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.gl.GlTextureUnit;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.material.MaterialManager;
import com.jozufozu.flywheel.backend.state.RenderLayer;
import com.jozufozu.flywheel.core.Contexts;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.util.Lazy;
import com.jozufozu.flywheel.util.Pair;

import com.mojang.math.Matrix4f;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelBakery;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Responsible for rendering the block breaking overlay for instanced tiles.
 */
@Environment(EnvType.CLIENT)
public class CrumblingRenderer {

	private static final Lazy<State> STATE;
	private static final Lazy.KillSwitch<State> INVALIDATOR;

	static {
		Pair<Lazy<State>, Lazy.KillSwitch<State>> state = Lazy.ofKillable(State::new, State::kill);

		STATE = state.getFirst();
		INVALIDATOR = state.getSecond();
	}

	public static void renderBreaking(ClientLevel world, Matrix4f viewProjection, double cameraX, double cameraY, double cameraZ) {
		if (!Backend.getInstance()
				.canUseInstancing(world)) return;

		Int2ObjectMap<List<BlockEntity>> activeStages = getActiveStageTiles(world);

		if (activeStages.isEmpty()) return;

		State state = STATE.get();
		RenderType layer = ModelBakery.DESTROY_TYPES.get(0);

		InstanceManager<BlockEntity> renderer = state.instanceManager;

		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		Camera info = Minecraft.getInstance().gameRenderer.getMainCamera();

		MaterialManager<CrumblingProgram> materials = state.materialManager;
		layer.setupRenderState();

		for (Int2ObjectMap.Entry<List<BlockEntity>> stage : activeStages.int2ObjectEntrySet()) {
			int i = stage.getIntKey();
			AbstractTexture breaking = textureManager.getTexture(ModelBakery.BREAKING_LOCATIONS.get(i));

			// something about when we call this means that the textures are not ready for use on the first frame they should appear
			if (breaking != null) {
				stage.getValue().forEach(renderer::add);

				renderer.beginFrame(info);

				GlTextureUnit.T4.makeActive();
				glBindTexture(GL_TEXTURE_2D, breaking.getId());
				materials.render(RenderLayer.SOLID, viewProjection, cameraX, cameraY, cameraZ);

				renderer.invalidate();
			}

		}

		layer.clearRenderState();

		GlTextureUnit.T0.makeActive();
		AbstractTexture breaking = textureManager.getTexture(ModelBakery.BREAKING_LOCATIONS.get(0));
		if (breaking != null) glBindTexture(GL_TEXTURE_2D, breaking.getId());
	}

	/**
	 * Associate each breaking stage with a list of all tile entities at that stage.
	 */
	private static Int2ObjectMap<List<BlockEntity>> getActiveStageTiles(ClientLevel world) {
		Long2ObjectMap<SortedSet<BlockDestructionProgress>> breakingProgressions = Minecraft.getInstance().levelRenderer.destructionProgress;

		Int2ObjectMap<List<BlockEntity>> breakingEntities = new Int2ObjectArrayMap<>();

		for (Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> entry : breakingProgressions.long2ObjectEntrySet()) {
			BlockPos breakingPos = BlockPos.of(entry.getLongKey());

			SortedSet<BlockDestructionProgress> progresses = entry.getValue();
			if (progresses != null && !progresses.isEmpty()) {
				int blockDamage = progresses.last()
						.getProgress();

				BlockEntity BlockEntity = world.getBlockEntity(breakingPos);

				if (BlockEntity != null) {
					List<BlockEntity> tileEntities = breakingEntities.computeIfAbsent(blockDamage, $ -> new ArrayList<>());
					tileEntities.add(BlockEntity);
				}
			}
		}

		return breakingEntities;
	}

	public static void onReloadRenderers(ReloadRenderersEvent event) {
		ClientLevel world = event.getWorld();
		if (Backend.getInstance()
				.canUseInstancing() && world != null) {
			reset();
		}
	}

	public static void reset() {
		INVALIDATOR.killValue();
	}

	private static class State {
		private final MaterialManager<CrumblingProgram> materialManager;
		private final InstanceManager<BlockEntity> instanceManager;

		private State() {
			materialManager = MaterialManager.builder(Contexts.CRUMBLING)
					.setGroupFactory(CrumblingGroup::new)
					.build();
			instanceManager = new CrumblingInstanceManager(materialManager);
		}

		private void kill() {
			materialManager.delete();
			instanceManager.invalidate();
		}
	}
}
