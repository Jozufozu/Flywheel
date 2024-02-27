package com.jozufozu.flywheel.core;

import com.jozufozu.flywheel.Flywheel;
import com.jozufozu.flywheel.api.struct.StructType;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.core.materials.model.ModelType;
import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.jozufozu.flywheel.core.materials.oriented.OrientedType;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Materials {

	public static final StructType<OrientedData> ORIENTED = new OrientedType();
	public static final StructType<ModelData> TRANSFORMED = new ModelType();

	public static class Names {
		public static final ResourceLocation MODEL = Flywheel.rl("model");
		public static final ResourceLocation ORIENTED = Flywheel.rl("oriented");
		public static final ResourceLocation PASSTHRU = Flywheel.rl("passthru");
	}
}
