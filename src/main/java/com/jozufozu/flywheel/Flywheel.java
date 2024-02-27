package com.jozufozu.flywheel;

import net.neoforged.bus.api.IEventBus;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.slf4j.Logger;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.ShadersModHandler;
import com.jozufozu.flywheel.config.BackendTypeArgument;
import com.jozufozu.flywheel.config.FlwCommands;
import com.jozufozu.flywheel.config.FlwConfig;
import com.jozufozu.flywheel.core.Contexts;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.StitchedSprite;
import com.jozufozu.flywheel.core.compile.ProgramCompiler;
import com.jozufozu.flywheel.event.ReloadRenderersEvent;
import com.jozufozu.flywheel.mixin.PausedPartialTickAccessor;
import com.jozufozu.flywheel.vanilla.VanillaInstances;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.CrashReportCallables;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

@Mod(Flywheel.ID)
public class Flywheel {

	public static final String ID = "flywheel";
	public static final Logger LOGGER = LogUtils.getLogger();
	private static ArtifactVersion version;

	public Flywheel(IEventBus modEventBus) {
		ModLoadingContext modLoadingContext = ModLoadingContext.get();

		version = modLoadingContext
				.getActiveContainer()
				.getModInfo()
				.getVersion();

		IEventBus forgeEventBus = NeoForge.EVENT_BUS;
		modEventBus.addListener(Flywheel::registerArgumentTypes);

		FlwConfig.init();

		modLoadingContext.registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(
				() -> "FLYWHEEL",
				(serverVersion, isNetwork) -> true
		));

		if (FMLEnvironment.dist.isClient()) {
			Flywheel.clientInit(forgeEventBus, modEventBus);
		}
	}

	private static void clientInit(IEventBus forgeEventBus, IEventBus modEventBus) {
		CrashReportCallables.registerCrashCallable("Flywheel Backend", Backend::getBackendDescriptor);

		ShadersModHandler.init();
		Backend.init();

		forgeEventBus.addListener(FlwCommands::registerClientCommands);
		forgeEventBus.<ReloadRenderersEvent>addListener(ProgramCompiler::invalidateAll);

		modEventBus.addListener(Contexts::flwInit);
		modEventBus.addListener(PartialModel::onModelRegistry);
		modEventBus.addListener(PartialModel::onModelBake);
		modEventBus.addListener(StitchedSprite::onTextureStitchPost);

		VanillaInstances.init();

		// https://github.com/Jozufozu/Flywheel/issues/69
		// Weird issue with accessor loading.
		// Only thing I've seen that's close to a fix is to force the class to load before trying to use it.
		// From the SpongePowered discord:
		// https://discord.com/channels/142425412096491520/626802111455297538/675007581168599041
		LOGGER.debug("Successfully loaded {}", PausedPartialTickAccessor.class.getName());
	}

	private static void registerArgumentTypes(RegisterEvent event) {
		event.register(Registries.COMMAND_ARGUMENT_TYPE, rl("engine"), () -> {
			return ArgumentTypeInfos.registerByClass(BackendTypeArgument.class, SingletonArgumentInfo.contextFree(BackendTypeArgument::getInstance));
		});
	}

	public static ArtifactVersion getVersion() {
		return version;
	}

	public static ResourceLocation rl(String path) {
		return new ResourceLocation(ID, path);
	}
}
