package com.jozufozu.flywheel.event;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public class GatherContextEvent extends Event implements IModBusEvent {

	private final boolean firstLoad;

	public GatherContextEvent(boolean firstLoad) {
		this.firstLoad = firstLoad;
	}

	/**
	 * @return true iff it is the first time the event is fired.
	 */
	public boolean isFirstLoad() {
		return firstLoad;
	}
}
