package com.jozufozu.flywheel.backend.engine.instancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL32;

import com.jozufozu.flywheel.api.context.Context;
import com.jozufozu.flywheel.api.event.RenderContext;
import com.jozufozu.flywheel.api.event.RenderStage;
import com.jozufozu.flywheel.api.instance.Instance;
import com.jozufozu.flywheel.api.task.Plan;
import com.jozufozu.flywheel.api.task.TaskExecutor;
import com.jozufozu.flywheel.backend.compile.InstancingPrograms;
import com.jozufozu.flywheel.backend.engine.AbstractEngine;
import com.jozufozu.flywheel.backend.engine.AbstractInstancer;
import com.jozufozu.flywheel.backend.engine.InstanceHandleImpl;
import com.jozufozu.flywheel.backend.engine.InstancerStorage;
import com.jozufozu.flywheel.backend.engine.UniformBuffer;
import com.jozufozu.flywheel.gl.GlStateTracker;
import com.jozufozu.flywheel.gl.GlTextureUnit;
import com.jozufozu.flywheel.lib.context.Contexts;
import com.jozufozu.flywheel.lib.material.MaterialIndices;
import com.jozufozu.flywheel.lib.task.Flag;
import com.jozufozu.flywheel.lib.task.NamedFlag;
import com.jozufozu.flywheel.lib.task.SyncedPlan;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;

public class InstancingEngine extends AbstractEngine {
	private final InstancedDrawManager drawManager = new InstancedDrawManager();

	private final Flag flushFlag = new NamedFlag("flushed");

	public InstancingEngine(int maxOriginDistance) {
		super(maxOriginDistance);
    }

	@Override
	public Plan<RenderContext> createFramePlan() {
		return SyncedPlan.of(this::flushDrawManager);
	}

	private void flushDrawManager() {
		try (var restoreState = GlStateTracker.getRestoreState()) {
			drawManager.flush();
		}
		flushFlag.raise();
	}

	@Override
	public void renderStage(TaskExecutor executor, RenderContext context, RenderStage stage) {
		executor.syncUntil(flushFlag::isRaised);
		if (stage.isLast()) {
			flushFlag.lower();
		}

		var drawSet = drawManager.get(stage);

        if (drawSet.isEmpty()) {
            return;
        }

        try (var state = GlStateTracker.getRestoreState()) {
            setup();

            render(drawSet);
        }
    }

	@Override
	public void renderCrumblingInstances(TaskExecutor executor, RenderContext context, List<Instance> instances, int progress) {
		if (instances.isEmpty()) {
			return;
		}

		if (progress < 0 || progress >= ModelBakery.DESTROY_TYPES.size()) {
			return;
		}

		// Need to wait for flush before we can inspect instancer state.
		executor.syncUntil(flushFlag::isRaised);

		// Sort draw calls into buckets, so we don't have to do as many shader binds.
		var drawMap = getDrawsForInstances(instances);

		if (drawMap.isEmpty()) {
			return;
		}

		try (var state = GlStateTracker.getRestoreState()) {
			var crumblingType = ModelBakery.DESTROY_TYPES.get(progress);

			for (var entry : drawMap.entrySet()) {
				var shader = entry.getKey();

				setup(shader, Contexts.CRUMBLING);

				shader.material().setup();

				int renderTex = RenderSystem.getShaderTexture(0);

				shader.material().clear();

				crumblingType.setupRenderState();

				RenderSystem.setShaderTexture(1, renderTex);
				GlTextureUnit.T1.makeActive();
				RenderSystem.bindTexture(renderTex);

				for (Runnable draw : entry.getValue()) {
					draw.run();
				}
			}
		}
	}

	/**
	 * Get all draw calls for the given instances, grouped by shader state.
	 * @param instances The instances to draw.
	 * @return A mapping of shader states to many runnable draw calls.
	 */
	@NotNull
	private Map<ShaderState, List<Runnable>> getDrawsForInstances(List<Instance> instances) {
		Map<ShaderState, List<Runnable>> out = new HashMap<>();

		for (Instance instance : instances) {
			// Filter out instances that weren't created by this engine.
			// If all is well, we probably shouldn't take the `continue`
			// branches but better to do checked casts.
			if (!(instance.handle() instanceof InstanceHandleImpl impl)) {
				continue;
			}
			if (!(impl.instancer instanceof InstancedInstancer<?> instancer)) {
				continue;
			}

			List<DrawCall> draws = instancer.drawCalls();

			draws.removeIf(DrawCall::isInvalid);

			for (DrawCall draw : draws) {
				out.computeIfAbsent(draw.shaderState, $ -> new ArrayList<>())
						.add(() -> draw.renderOne(impl));
			}
		}
		return out;
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

	private void render(InstancedDrawManager.DrawSet drawSet) {
		for (var entry : drawSet) {
			var shader = entry.getKey();
			var drawCalls = entry.getValue();

			drawCalls.removeIf(DrawCall::isInvalid);

			if (drawCalls.isEmpty()) {
				continue;
			}

			setup(shader, Contexts.WORLD);

			shader.material().setup();

			for (var drawCall : drawCalls) {
				drawCall.render();
			}

			shader.material().clear();
		}
	}

	private void setup(ShaderState desc, Context context) {
		var material = desc.material();
		var vertexType = desc.vertexType();
		var instanceType = desc.instanceType();

		var program = InstancingPrograms.get()
				.get(vertexType, instanceType, context);
		UniformBuffer.syncAndBind(program);

		var uniformLocation = program.getUniformLocation("_flw_materialID_instancing");
		var vertexID = MaterialIndices.getVertexShaderIndex(material);
		var fragmentID = MaterialIndices.getFragmentShaderIndex(material);
		GL32.glUniform2ui(uniformLocation, vertexID, fragmentID);
	}

	@Override
	protected InstancerStorage<? extends AbstractInstancer<?>> getStorage() {
		return drawManager;
	}

	@Override
	public void delete() {
		drawManager.invalidate();
	}
}
