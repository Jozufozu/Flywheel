package com.jozufozu.flywheel.backend.engine.uniform;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import com.jozufozu.flywheel.api.event.ReloadLevelRendererEvent;
import com.jozufozu.flywheel.api.event.RenderContext;
import com.jozufozu.flywheel.backend.gl.GlStateTracker;
import com.jozufozu.flywheel.config.DebugMode;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class Uniforms {
	public static final int FRAME_INDEX = 0;
	public static final int FOG_INDEX = 1;
	public static final int OPTIONS_INDEX = 2;
	public static final int PLAYER_INDEX = 3;
	public static final int LEVEL_INDEX = 4;
	public static boolean frustumPaused = false;
	public static boolean frustumCapture = false;
	private static @Nullable UniformBuffer<FrameUniforms> frame;
	private static @Nullable UniformBuffer<FogUniforms> fog;
	private static @Nullable UniformBuffer<OptionsUniforms> options;
	private static @Nullable UniformBuffer<PlayerUniforms> player;
	private static @Nullable UniformBuffer<LevelUniforms> level;
	private static boolean optionsRequiresUpdate = false;

	public static UniformBuffer<FrameUniforms> frame() {
		if (frame == null) {
			frame = new UniformBuffer<>(FRAME_INDEX, new FrameUniforms());
		}
		return frame;
	}

	public static UniformBuffer<FogUniforms> fog() {
		if (fog == null) {
			fog = new UniformBuffer<>(FOG_INDEX, new FogUniforms());
		}
		return fog;
	}

	public static UniformBuffer<OptionsUniforms> options() {
		if (options == null) {
			options = new UniformBuffer<>(OPTIONS_INDEX, new OptionsUniforms());
		}
		return options;
	}

	public static UniformBuffer<PlayerUniforms> player() {
		if (player == null) {
			player = new UniformBuffer<>(PLAYER_INDEX, new PlayerUniforms());
		}
		return player;
	}

	public static UniformBuffer<LevelUniforms> level() {
		if (level == null) {
			level = new UniformBuffer<>(LEVEL_INDEX, new LevelUniforms());
		}
		return level;
	}

	public static void bindForDraw() {
		bindFrame();
		bindFog();
		bindOptions();
		bindPlayer();
		bindLevel();
	}

	public static void bindFrame() {
		if (frame != null) {
			frame.bind();
		}
	}

	public static void bindFog() {
		if (fog != null) {
			fog.bind();
		}
	}

	public static void bindOptions() {
		if (options != null) {
			options.bind();
		}
	}

	public static void bindPlayer() {
		if (player != null) {
			player.bind();
		}
	}

	public static void bindLevel() {
		if (level != null) {
			level.bind();
		}
	}

	public static void onFogUpdate() {
		try (var restoreState = GlStateTracker.getRestoreState()) {
			fog().update();
		}
	}

	public static void onOptionsUpdate() {
		// this is sometimes called too early to do an actual update
		optionsRequiresUpdate = true;
	}

	public static void updateContext(RenderContext ctx) {
		var ubo = frame();
		ubo.provider.setContext(ctx);
		ubo.update();

		if (optionsRequiresUpdate) {
			options().update();
			optionsRequiresUpdate = false;
		}

		var player = player();
		player.provider.setContext(ctx);
		player.update();

		var level = level();
		level.provider.setContext(ctx);
		level.update();
	}

	public static void setDebugMode(DebugMode mode) {
		frame().provider.debugMode = mode.ordinal();
	}

	public static void onReloadLevelRenderer(ReloadLevelRendererEvent event) {
		if (frame != null) {
			frame.delete();
			frame = null;
		}

		if (fog != null) {
			fog.delete();
			fog = null;
		}

		if (options != null) {
			options.delete();
			options = null;
		}

		if (player != null) {
			player.delete();
			player = null;
		}

		if (level != null) {
			level.delete();
			level = null;
		}
	}

	static long writeVec4(long ptr, float x, float y, float z, float w) {
		MemoryUtil.memPutFloat(ptr, x);
		MemoryUtil.memPutFloat(ptr + 4, y);
		MemoryUtil.memPutFloat(ptr + 8, z);
		MemoryUtil.memPutFloat(ptr + 12, w);
		return ptr + 16;
	}

	static long writeVec3(long ptr, float camX, float camY, float camZ) {
		MemoryUtil.memPutFloat(ptr, camX);
		MemoryUtil.memPutFloat(ptr + 4, camY);
		MemoryUtil.memPutFloat(ptr + 8, camZ);
		MemoryUtil.memPutFloat(ptr + 12, 0f); // empty component of vec4 because we don't trust std140
		return ptr + 16;
	}

	static long writeVec2(long ptr, float camX, float camY) {
		MemoryUtil.memPutFloat(ptr, camX);
		MemoryUtil.memPutFloat(ptr + 4, camY);
		return ptr + 8;
	}

	static long writeInFluidAndBlock(long ptr, Level level, BlockPos blockPos, Vec3 pos) {
		FluidState fState = level.getFluidState(blockPos);
		BlockState bState = level.getBlockState(blockPos);
		float height = fState.getHeight(level, blockPos);

		if (fState.isEmpty()) {
			MemoryUtil.memPutInt(ptr, 0);
		} else if (pos.y < blockPos.getY() + height) {
			// TODO: handle custom fluids via defines
			if (fState.is(FluidTags.WATER)) {
				MemoryUtil.memPutInt(ptr, 1);
			} else if (fState.is(FluidTags.LAVA)) {
				MemoryUtil.memPutInt(ptr, 2);
			} else {
				MemoryUtil.memPutInt(ptr, -1);
			}
		}

		if (bState.isAir()) {
			MemoryUtil.memPutInt(ptr + 4, 0);
		} else {
			// TODO: handle custom blocks via defines
			if (bState.is(Blocks.POWDER_SNOW)) {
				MemoryUtil.memPutInt(ptr + 4, 0);
			} else {
				MemoryUtil.memPutInt(ptr + 4, -1);
			}
		}

		return ptr + 8;
	}
}
