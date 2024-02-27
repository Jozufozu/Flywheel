package com.jozufozu.flywheel.core.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.jozufozu.flywheel.core.virtual.VirtualEmptyBlockGetter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class WorldModelBuilder implements Bufferable {
	private final RenderType layer;

	private PoseStack poseStack = new PoseStack();
	private Map<BlockPos, ModelData> modelData = Collections.emptyMap();
	private BlockAndTintGetter renderWorld = VirtualEmptyBlockGetter.INSTANCE;
	private Collection<StructureTemplate.StructureBlockInfo> blocks = Collections.emptyList();

	public WorldModelBuilder(RenderType layer) {
		this.layer = layer;
	}

	@Override
	public void bufferInto(VertexConsumer consumer, ModelBlockRenderer modelRenderer, RandomSource random) {
		ModelBlockRenderer.enableCaching();
		for (StructureTemplate.StructureBlockInfo info : this.blocks) {
			BlockState state = info.state();
			if (state.getRenderShape() != RenderShape.MODEL) continue;

			BlockPos pos = info.pos();
			long randomSeed = state.getSeed(pos);
			BakedModel model = ModelUtil.VANILLA_RENDERER.getBlockModel(state);
			ModelData data = this.modelData.getOrDefault(pos, ModelData.EMPTY);
			data = model.getModelData(renderWorld, pos, state, data);
			random.setSeed(randomSeed);
			if (!model.getRenderTypes(state, random, data).contains(this.layer)) continue;

			poseStack.pushPose();
			poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
			modelRenderer.tesselateBlock(this.renderWorld, model, state, pos, poseStack, consumer, true, random, randomSeed, OverlayTexture.NO_OVERLAY, data, this.layer);
			poseStack.popPose();
		}
		ModelBlockRenderer.clearCache();
	}

	/**
	 * It is expected that {@code renderWorld.getShade(...)} returns a constant.
	 */
	public WorldModelBuilder withRenderWorld(BlockAndTintGetter renderWorld) {
		this.renderWorld = renderWorld;
		return this;
	}

	public WorldModelBuilder withBlocks(Collection<StructureTemplate.StructureBlockInfo> blocks) {
		this.blocks = blocks;
		return this;
	}

	public WorldModelBuilder withModelData(Map<BlockPos, ModelData> modelData) {
		this.modelData = modelData;
		return this;
	}

	public WorldModelBuilder withPoseStack(PoseStack poseStack) {
		this.poseStack = poseStack;
		return this;
	}

	public BlockModel toModel(String name) {
		return BlockModel.of(this, name);
	}
}
