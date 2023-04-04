package com.jozufozu.flywheel.glsl;

import java.util.Collection;

import net.minecraft.resources.ResourceLocation;

public interface SourceComponent {
	Collection<? extends SourceComponent> included();

	String source();

	ResourceLocation name();
}