package com.jozufozu.flywheel.backend;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RenderWork {
	private static final Queue<Runnable> runs = new ConcurrentLinkedQueue<>();


	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderWorldLast(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
			return;
		}

		while (!runs.isEmpty()) {
			runs.remove()
					.run();
		}
	}

	/**
	 * Queue work to be executed at the end of a frame
	 */
	public static void enqueue(Runnable run) {
		runs.add(run);
	}
}
