package com.jozufozu.flywheel.api.visualization;

import com.jozufozu.flywheel.api.BackendImplemented;
import com.jozufozu.flywheel.api.instance.InstancerProvider;

import net.minecraft.core.Vec3i;

/**
 * A context object passed on visual creation.
 */
@BackendImplemented
public interface VisualizationContext {
	/**
	 * @return The {@link InstancerProvider} that the visual can use to get instancers to render models.
	 */
	InstancerProvider instancerProvider();

	/**
	 * All models render as if this position is (0, 0, 0).
	 *
	 * @return The origin of the renderer as a world position.
	 */
	Vec3i renderOrigin();

	VisualEmbedding createEmbedding();
}
