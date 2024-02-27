package com.jozufozu.flywheel.event;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class EntityWorldHandler {

	@SubscribeEvent
	public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide && Backend.isOn()) InstancedRenderDispatcher.getEntities(event.getLevel())
				.queueAdd(event.getEntity());
	}

	@SubscribeEvent
	public static void onEntityLeaveWorld(EntityLeaveLevelEvent event) {
		if (event.getLevel().isClientSide && Backend.isOn()) InstancedRenderDispatcher.getEntities(event.getLevel())
				.remove(event.getEntity());
	}
}
