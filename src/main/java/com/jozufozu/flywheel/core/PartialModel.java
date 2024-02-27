package com.jozufozu.flywheel.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * A helper class for loading and accessing json models.
 * <br>
 * Creating a PartialModel will make the associated modelLocation automatically load.
 * PartialModels must be initialized the mod class constructor.
 * <br>
 * Once {@link ModelEvent.BakingCompleted} finishes, all PartialModels (with valid modelLocations)
 * will have their bakedModel fields populated.
 * <br>
 * Attempting to create a PartialModel after {@link ModelEvent.RegisterAdditional} will cause an error.
 */
public class PartialModel {

	private static final List<PartialModel> ALL = new ArrayList<>();
	private static boolean tooLate = false;

	protected final ResourceLocation modelLocation;
	protected BakedModel bakedModel;

	public PartialModel(ResourceLocation modelLocation) {
		if (tooLate) throw new RuntimeException("PartialModel '" + modelLocation + "' loaded after ModelEvent.RegisterAdditional");

		this.modelLocation = modelLocation;
		ALL.add(this);
	}

	public static void onModelRegistry(ModelEvent.RegisterAdditional event) {
		for (PartialModel partial : ALL)
			event.register(partial.getLocation());

		tooLate = true;
	}

	public static void onModelBake(ModelEvent.BakingCompleted event) {
		Map<ResourceLocation, BakedModel> models = event.getModels();
		for (PartialModel partial : ALL)
			partial.set(models.get(partial.getLocation()));
	}

	protected void set(BakedModel bakedModel) {
		this.bakedModel = bakedModel;
	}

	public ResourceLocation getLocation() {
		return modelLocation;
	}

	public BakedModel get() {
		return bakedModel;
	}

}
