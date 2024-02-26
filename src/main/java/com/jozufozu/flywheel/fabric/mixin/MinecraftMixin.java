package com.jozufozu.flywheel.fabric.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.event.ForgeEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	public ClientLevel level;

	@Inject(method = "setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V", at = @At("HEAD"))
	private void onHeadSetLevel(CallbackInfo ci) {
		if (level != null) {
			ForgeEvents.unloadWorld(level);
		}
	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At(value = "JUMP", opcode = Opcodes.IFNULL, ordinal = 2))
	private void onClearLevel(CallbackInfo ci) {
		ForgeEvents.unloadWorld(level);
	}
}
