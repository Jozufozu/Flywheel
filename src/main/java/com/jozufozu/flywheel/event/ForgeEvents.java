package com.jozufozu.flywheel.event;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.light.LightUpdater;
import com.jozufozu.flywheel.util.WorldAttached;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ForgeEvents {

	@SubscribeEvent
	public static void addToDebugScreen(CustomizeGuiOverlayEvent.DebugText event) {
		if (Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
			InstancedRenderDispatcher.getDebugString(event.getRight());
		}
	}

	@SubscribeEvent
	public static void unloadWorld(LevelEvent.Unload event) {
		WorldAttached.invalidateWorld(event.getLevel());
	}

	@SubscribeEvent
	public static void tickLight(TickEvent.ClientTickEvent e) {
		if (e.phase == TickEvent.Phase.END && Backend.isGameActive())
			LightUpdater.get(Minecraft.getInstance().level).tick();
	}

}
