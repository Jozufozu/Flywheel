package com.jozufozu.flywheel.mixin.instancemanage;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(LevelChunk.class)
public class InstanceAddMixin {

	@Shadow
	@Final
	Level level;

	@Inject(method = "setBlockEntity",
			at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private void blockEntityAdded(BlockEntity be, CallbackInfo ci) {
		if (level.isClientSide && Backend.isOn()) {
			InstancedRenderDispatcher.getBlockEntities(this.level)
					.add(be);
		}
	}
}
