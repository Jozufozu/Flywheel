package com.jozufozu.flywheel.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;

public class FlwConfig {

	private static final FlwConfig INSTANCE = new FlwConfig();

	public final ClientConfig client;

	public FlwConfig() {
		Pair<ClientConfig, ModConfigSpec> client = new ModConfigSpec.Builder().configure(ClientConfig::new);

		this.client = client.getLeft();

		ModLoadingContext.get()
				.registerConfig(ModConfig.Type.CLIENT, client.getRight());
	}

	public static FlwConfig get() {
		return INSTANCE;
	}

	public BackendType getBackendType() {
		return client.backend.get();
	}

	public boolean debugNormals() {
		return client.debugNormals.get();
	}

	public boolean limitUpdates() {
		return client.limitUpdates.get();
	}

	public static void init() {
	}

	public static class ClientConfig {
		public final ModConfigSpec.EnumValue<BackendType> backend;
		public final ModConfigSpec.BooleanValue debugNormals;
		public final ModConfigSpec.BooleanValue limitUpdates;

		public ClientConfig(ModConfigSpec.Builder builder) {
			backend = builder.comment("Select the backend to use.")
					.defineEnum("backend", BackendType.INSTANCING);

			debugNormals = builder.comment("Enable or disable a debug overlay that colors pixels by their normal.")
					.define("debugNormals", false);

			limitUpdates = builder.comment("Enable or disable instance update limiting with distance.")
					.define("limitUpdates", true);
		}
	}
}
