package com.jozufozu.flywheel.mixin;

import java.util.List;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

@Environment(EnvType.CLIENT)
@Mixin(ChunkRenderDispatcher.CompiledChunk.class)
public class CancelBlockEntityRenderMixin {

	/**
	 * JUSTIFICATION: when instanced rendering is enabled, many tile entities no longer need
	 * to be processed by the normal game renderer. This method is only called to retrieve the
	 * list of tile entities to render. By filtering the output here, we prevent the game from
	 * doing unnecessary light lookups and frustum checks.
	 */
	@Inject(at = @At("RETURN"), method = "getRenderableBlockEntities", cancellable = true)
	private void noRenderInstancedTiles(CallbackInfoReturnable<List<BlockEntity>> cir) {
		if (Backend.getInstance()
				.canUseInstancing()) {
			List<BlockEntity> tiles = cir.getReturnValue();

			InstancedRenderRegistry r = InstancedRenderRegistry.getInstance();
			tiles.removeIf(r::shouldSkipRender);
		}
	}
}