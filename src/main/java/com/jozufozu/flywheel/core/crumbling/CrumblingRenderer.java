package com.jozufozu.flywheel.core.crumbling;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.gl.GlStateTracker;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.SerialTaskEngine;
import com.jozufozu.flywheel.backend.instancing.instancing.InstancingEngine;
import com.jozufozu.flywheel.core.Contexts;
import com.jozufozu.flywheel.core.shader.WorldProgram;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.jozufozu.flywheel.mixin.LevelRendererAccessor;
import com.jozufozu.flywheel.util.Lazy;
import com.jozufozu.flywheel.util.Pair;
import com.mojang.blaze3d.vertex.PoseStack;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/**
 * Responsible for rendering the block breaking overlay for instanced block entities.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
public class CrumblingRenderer {

	static RenderType _currentLayer;

	private static final Lazy<State> STATE;
	private static final Lazy.KillSwitch<State> INVALIDATOR;

	static {
		Pair<Lazy<State>, Lazy.KillSwitch<State>> state = Lazy.ofKillable(State::new, State::kill);

        STATE = state.first();
		INVALIDATOR = state.second();
	}

	public static void render(ClientLevel level, Camera camera, PoseStack stack) {
		if (!Backend.canUseInstancing(level)) return;

		Int2ObjectMap<List<BlockEntity>> activeStages = getActiveStageBlockEntities(level);
		if (activeStages.isEmpty()) return;

		Vec3 cameraPos = camera.getPosition();

		// XXX Restore state
		GlStateTracker.State restoreState = GlStateTracker.getRestoreState();
		renderCrumbling(activeStages, camera, new RenderLayerEvent(level, null, stack, null, cameraPos.x, cameraPos.y, cameraPos.z));
		restoreState.restore();
	}

	private static void renderCrumbling(Int2ObjectMap<List<BlockEntity>> activeStages, Camera camera, RenderLayerEvent event) {
		State state = STATE.get();
		InstanceManager<BlockEntity> instanceManager = state.instanceManager;
		InstancingEngine<WorldProgram> materials = state.materialManager;

		for (Int2ObjectMap.Entry<List<BlockEntity>> stage : activeStages.int2ObjectEntrySet()) {
			_currentLayer = ModelBakery.DESTROY_TYPES.get(stage.getIntKey());

			// something about when we call this means that the textures are not ready for use on the first frame they should appear
			if (_currentLayer != null) {
				stage.getValue().forEach(instanceManager::add);

				instanceManager.beginFrame(SerialTaskEngine.INSTANCE, camera);

				// XXX Each call applies another restore state even though we are already inside of a restore state
				materials.render(SerialTaskEngine.INSTANCE, event);

				instanceManager.invalidate();
			}
		}
	}

	/**
	 * Associate each breaking stage with a list of all block entities at that stage.
	 */
	private static Int2ObjectMap<List<BlockEntity>> getActiveStageBlockEntities(ClientLevel world) {

		Int2ObjectMap<List<BlockEntity>> breakingEntities = new Int2ObjectArrayMap<>();

		for (Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> entry : ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).flywheel$getDestructionProgress()
				.long2ObjectEntrySet()) {
			BlockPos breakingPos = BlockPos.of(entry.getLongKey());

			SortedSet<BlockDestructionProgress> progresses = entry.getValue();
			if (progresses != null && !progresses.isEmpty()) {
				int blockDamage = progresses.last()
						.getProgress();

				BlockEntity blockEntity = world.getBlockEntity(breakingPos);

				if (blockEntity != null) {
					List<BlockEntity> blockEntities = breakingEntities.computeIfAbsent(blockDamage, $ -> new ArrayList<>());
					blockEntities.add(blockEntity);
				}
			}
		}

		return breakingEntities;
	}

	@SubscribeEvent
	public static void onReloadRenderers(ReloadRenderersEvent event) {
		ClientLevel world = event.getWorld();
        if (Backend.isOn() && world != null) {
			reset();
		}
	}

	public static void reset() {
		INVALIDATOR.killValue();
	}

	private static class State {
		private final InstancingEngine<WorldProgram> materialManager;
		private final InstanceManager<BlockEntity> instanceManager;

		private State() {
			materialManager = InstancingEngine.builder(Contexts.CRUMBLING)
					.setGroupFactory(CrumblingGroup::new)
					.build();
			instanceManager = new CrumblingInstanceManager(materialManager);
			materialManager.addListener(instanceManager);
		}

		private void kill() {
			materialManager.delete();
			instanceManager.invalidate();
		}
	}
}
