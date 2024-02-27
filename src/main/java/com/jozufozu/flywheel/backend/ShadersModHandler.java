package com.jozufozu.flywheel.backend;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.neoforged.fml.ModList;

public final class ShadersModHandler {
	public static final String OPTIFINE_ROOT_PACKAGE = "net.optifine";
	public static final String SHADER_PACKAGE = "net.optifine.shaders";

	private static final boolean isOculusLoaded;
	private static final boolean isOptifineInstalled;
	private static final InternalHandler internalHandler;

	static {
		Package optifinePackage = Package.getPackage(OPTIFINE_ROOT_PACKAGE);
		isOptifineInstalled = optifinePackage != null;
		isOculusLoaded = ModList.get()
				.isLoaded("oculus");

		// optfine and oculus are assumed to be mutually exclusive

		if (isOptifineInstalled) {
			Backend.LOGGER.info("Optifine detected.");
			internalHandler = new Optifine();
		} else if (isOculusLoaded) {
			Backend.LOGGER.info("Oculus detected.");
			internalHandler = new Oculus();
		} else {
			Backend.LOGGER.info("No shaders mod detected.");
			internalHandler = new InternalHandler() {};
		}
	}

	private ShadersModHandler() {
	}

	public static void init() {
		// noop, load statics
	}

	public static boolean isOptifineInstalled() {
		return isOptifineInstalled;
	}

	public static boolean isOculusLoaded() {
		return isOculusLoaded;
	}

	public static boolean isShaderPackInUse() {
		return internalHandler.isShaderPackInUse();
	}

	public static boolean isRenderingShadowPass() {
		return internalHandler.isRenderingShadowPass();
	}

	private interface InternalHandler {
		default boolean isShaderPackInUse() {
			return false;
		};

		default boolean isRenderingShadowPass() {
			return false;
		};
	}

	// simple, lovely api calls
	private static class Oculus implements InternalHandler {
		@Override
		public boolean isShaderPackInUse() {
			return false /*IrisApi.getInstance()
					.isShaderPackInUse()*/;
		}

		@Override
		public boolean isRenderingShadowPass() {
			return false /*IrisApi.getInstance()
					.isRenderingShadowPass()*/;
		}
	}

	// evil reflection
	private static class Optifine implements InternalHandler {
		private final BooleanSupplier shadersEnabledSupplier;
		private final BooleanSupplier shadowPassSupplier;
		private final FrustumConstructor shadowFrustumConstructor;

		Optifine() {
			shadersEnabledSupplier = createShadersEnabledSupplier();
			shadowPassSupplier = createShadowPassSupplier();
			shadowFrustumConstructor = createShadowFrustumConstructor();
		}

		@Override
		public boolean isShaderPackInUse() {
			return shadersEnabledSupplier.getAsBoolean();
		}

		@Override
		public boolean isRenderingShadowPass() {
			return shadowPassSupplier.getAsBoolean();
		}

		@Nullable
		public Frustum createShadowFrustum(Camera camera, float partialTicks) {
			var frustum = shadowFrustumConstructor.create(camera, partialTicks);
			if (frustum != null) {
				var position = camera.getPosition();
				frustum.prepare(position.x, position.y, position.z);
			}
			return frustum;
		}

		private static FrustumConstructor createShadowFrustumConstructor() {
			try {
				Class<?> ofShaders = Class.forName("net.optifine.shaders.ShadersRender");
				Method method = ofShaders.getDeclaredMethod("makeShadowFrustum", Camera.class, Float.TYPE);
				method.setAccessible(true);
				return (cam, pt) -> {
					try {
						return (Frustum) method.invoke(null, cam, pt);
					} catch (Exception ignored) {
						return null;
					}
				};
			} catch (Exception ignored) {
				return ($, $$) -> null;
			}
		}

		private static BooleanSupplier createShadowPassSupplier() {
			try {
				Class<?> ofShaders = Class.forName("net.optifine.shaders.Shaders");
				Field field = ofShaders.getDeclaredField("isShadowPass");
				field.setAccessible(true);
				return () -> {
					try {
						return field.getBoolean(null);
					} catch (IllegalAccessException ignored) {
						return false;
					}
				};
			} catch (Exception ignored) {
				return () -> false;
			}
		}

		private static BooleanSupplier createShadersEnabledSupplier() {
			try {
				Class<?> ofShaders = Class.forName("net.optifine.shaders.Shaders");
				Field field = ofShaders.getDeclaredField("shaderPackLoaded");
				field.setAccessible(true);
				return () -> {
					try {
						return field.getBoolean(null);
					} catch (IllegalAccessException ignored) {
						return false;
					}
				};
			} catch (Exception ignored) {
				return () -> false;
			}
		}

		@FunctionalInterface
		public interface FrustumConstructor {
			@Nullable
			Frustum create(Camera camera, float partialTicks);
		}
	}
}
