package com.jozufozu.flywheel.backend.instancing.batching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.jozufozu.flywheel.api.RenderStage;
import com.jozufozu.flywheel.api.instancer.InstancedPart;
import com.jozufozu.flywheel.api.instancer.Instancer;
import com.jozufozu.flywheel.api.struct.StructType;
import com.jozufozu.flywheel.backend.instancing.InstancerKey;
import com.jozufozu.flywheel.core.model.Mesh;
import com.jozufozu.flywheel.core.model.Model;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

public class BatchingTransformManager {
	private final Map<InstancerKey<?>, CPUInstancer<?>> instancers = new HashMap<>();
	private final List<UninitializedInstancer> uninitializedInstancers = new ArrayList<>();
	private final List<CPUInstancer<?>> initializedInstancers = new ArrayList<>();
	private final Map<RenderStage, TransformSet> transformSets = new EnumMap<>(RenderStage.class);
	private final Map<RenderStage, TransformSet> transformSetsView = Collections.unmodifiableMap(transformSets);
	private final Map<VertexFormat, BatchedMeshPool> meshPools = new HashMap<>();

	public TransformSet get(RenderStage stage) {
		return transformSets.getOrDefault(stage, TransformSet.EMPTY);
	}

	public Map<RenderStage, TransformSet> getTransformSetsView() {
		return transformSetsView;
	}

	@SuppressWarnings("unchecked")
	public <D extends InstancedPart> Instancer<D> getInstancer(StructType<D> type, Model model, RenderStage stage) {
		InstancerKey<D> key = new InstancerKey<>(type, model, stage);
		CPUInstancer<D> instancer = (CPUInstancer<D>) instancers.get(key);
		if (instancer == null) {
			instancer = new CPUInstancer<>(type);
			instancers.put(key, instancer);
			uninitializedInstancers.add(new UninitializedInstancer(instancer, model, stage));
		}
		return instancer;
	}

	public void flush() {
		for (var instancer : uninitializedInstancers) {
			add(instancer.instancer(), instancer.model(), instancer.stage());
		}
		uninitializedInstancers.clear();

		for (var pool : meshPools.values()) {
			pool.flush();
		}
	}

	public void delete() {
		instancers.clear();

		meshPools.values()
				.forEach(BatchedMeshPool::delete);
		meshPools.clear();

		initializedInstancers.forEach(CPUInstancer::delete);
		initializedInstancers.clear();
	}

	public void clearInstancers() {
		initializedInstancers.forEach(CPUInstancer::clear);
	}

	private void add(CPUInstancer<?> instancer, Model model, RenderStage stage) {
		TransformSet transformSet = transformSets.computeIfAbsent(stage, TransformSet::new);
		var meshes = model.getMeshes();
		for (var entry : meshes.entrySet()) {
			var material = entry.getKey();
			var renderType = material.getBatchingRenderType();
			TransformCall<?> transformCall = new TransformCall<>(instancer, material, alloc(entry.getValue(), renderType.format()));
			transformSet.put(renderType, transformCall);
		}
		initializedInstancers.add(instancer);
	}

	private BatchedMeshPool.BufferedMesh alloc(Mesh mesh, VertexFormat format) {
		return meshPools.computeIfAbsent(format, BatchedMeshPool::new)
				.alloc(mesh);
	}

	public static class TransformSet implements Iterable<Map.Entry<RenderType, Collection<TransformCall<?>>>> {
		public static final TransformSet EMPTY = new TransformSet(ImmutableListMultimap.of());

		final ListMultimap<RenderType, TransformCall<?>> transformCalls;

		public TransformSet(RenderStage renderStage) {
			transformCalls = ArrayListMultimap.create();
		}

		public TransformSet(ListMultimap<RenderType, TransformCall<?>> transformCalls) {
			this.transformCalls = transformCalls;
		}

		public void put(RenderType shaderState, TransformCall<?> transformCall) {
			transformCalls.put(shaderState, transformCall);
		}

		public boolean isEmpty() {
			return transformCalls.isEmpty();
		}

		@NotNull
		@Override
		public Iterator<Map.Entry<RenderType, Collection<TransformCall<?>>>> iterator() {
			return transformCalls.asMap()
					.entrySet()
					.iterator();
		}
	}

	private record UninitializedInstancer(CPUInstancer<?> instancer, Model model, RenderStage stage) {
	}
}