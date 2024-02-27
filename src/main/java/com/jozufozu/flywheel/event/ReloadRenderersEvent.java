package com.jozufozu.flywheel.event;

import javax.annotation.Nullable;

import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.bus.api.Event;

public class ReloadRenderersEvent extends Event {
	private final ClientLevel world;

	public ReloadRenderersEvent(ClientLevel world) {
		this.world = world;
	}

	@Nullable
	public ClientLevel getWorld() {
		return world;
	}
}
