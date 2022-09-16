package com.jozufozu.flywheel.backend.instancing.indirect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL32;

import com.jozufozu.flywheel.api.RenderStage;
import com.jozufozu.flywheel.api.context.ContextShader;
import com.jozufozu.flywheel.api.instancer.InstancedPart;
import com.jozufozu.flywheel.api.struct.StructType;
import com.jozufozu.flywheel.backend.gl.GlStateTracker;
import com.jozufozu.flywheel.backend.gl.GlTextureUnit;
import com.jozufozu.flywheel.backend.instancing.Engine;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.TaskEngine;
import com.jozufozu.flywheel.core.RenderContext;
import com.jozufozu.flywheel.util.WeakHashSet;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class IndirectEngine implements Engine {

	public static int MAX_ORIGIN_DISTANCE = 100;

	protected BlockPos originCoordinate = BlockPos.ZERO;

	protected final ContextShader context;

	protected final Map<StructType<?>, IndirectFactory<?>> factories = new HashMap<>();

	protected final List<IndirectModel<?>> uninitializedModels = new ArrayList<>();
	protected final IndirectDrawManager indirectDrawManager = new IndirectDrawManager();

	/**
	 * The set of instance managers that are attached to this engine.
	 */
	private final WeakHashSet<InstanceManager<?>> instanceManagers;

	public IndirectEngine(ContextShader context) {
		this.context = context;

		this.instanceManagers = new WeakHashSet<>();
	}

	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public <D extends InstancedPart> IndirectFactory<D> factory(StructType<D> type) {
		return (IndirectFactory<D>) factories.computeIfAbsent(type, this::createFactory);
	}

	@NotNull
	private <D extends InstancedPart> IndirectFactory<D> createFactory(StructType<D> type) {
		return new IndirectFactory<>(type, uninitializedModels::add);
	}

	@Override
	public void renderStage(TaskEngine taskEngine, RenderContext context, RenderStage stage) {
		try (var restoreState = GlStateTracker.getRestoreState()) {
			setup();
	
			for (var list : indirectDrawManager.lists.values()) {
				list.submit(stage);
			}
		}
	}

	private void setup() {
		GlTextureUnit.T2.makeActive();
		Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();

		RenderSystem.depthMask(true);
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(GL32.GL_LEQUAL);
		RenderSystem.enableCull();
	}

	@Override
	public void delete() {
		factories.values()
				.forEach(IndirectFactory::delete);

		indirectDrawManager.lists.values()
				.forEach(IndirectCullingGroup::delete);

		factories.clear();
	}

	@Override
	public Vec3i getOriginCoordinate() {
		return originCoordinate;
	}

	@Override
	public void attachManagers(InstanceManager<?>... listener) {
		instanceManagers.addAll(List.of(listener));
	}

	@Override
	public boolean maintainOriginCoordinate(Camera camera) {
		Vec3 cameraPos = camera.getPosition();

		double distanceSqr = Vec3.atLowerCornerOf(originCoordinate)
				.subtract(cameraPos)
				.lengthSqr();

		if (distanceSqr > MAX_ORIGIN_DISTANCE * MAX_ORIGIN_DISTANCE) {
			shiftListeners(Mth.floor(cameraPos.x), Mth.floor(cameraPos.y), Mth.floor(cameraPos.z));
			return true;
		}
		return false;
	}

	@Override
	public void beginFrame(TaskEngine taskEngine, RenderContext context) {
		try (var restoreState = GlStateTracker.getRestoreState()) {
			for (var model : uninitializedModels) {
				model.init(indirectDrawManager);
			}
			uninitializedModels.clear();
	
			for (IndirectCullingGroup<?> value : indirectDrawManager.lists.values()) {
				value.beginFrame();
			}
		}
	}

	private void shiftListeners(int cX, int cY, int cZ) {
		originCoordinate = new BlockPos(cX, cY, cZ);

		factories.values().forEach(IndirectFactory::clear);

		instanceManagers.forEach(InstanceManager::onOriginShift);
	}

	@Override
	public void addDebugInfo(List<String> info) {
		info.add("GL46 Indirect");
		info.add("Origin: " + originCoordinate.getX() + ", " + originCoordinate.getY() + ", " + originCoordinate.getZ());
	}
}