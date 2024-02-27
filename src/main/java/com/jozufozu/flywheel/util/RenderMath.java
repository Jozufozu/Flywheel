package com.jozufozu.flywheel.util;

import net.neoforged.neoforge.client.model.lighting.QuadLighter;

public class RenderMath {

	/**
	 * Convert a signed, normalized floating point value into a normalized byte.
	 */
	public static byte nb(float f) {
		return (byte) (f * 127);
	}

	/**
	 * Convert a signed byte into a normalized float.
	 */
	public static float f(byte b) {
		return b / 127f;
	}

	/**
	 * Convert an unsigned byte into a normalized float.
	 */
	public static float uf(byte b) {
		return (float) (Byte.toUnsignedInt(b)) / 255f;
	}

	/**
	 * Convert an unsigned, normalized float into an unsigned normalized byte.
	 */
	public static byte unb(float f) {
		return (byte) Math.floor(f * 255);
	}

	public static int nextPowerOf2(int a) {
		int h = Integer.highestOneBit(a);
		return (h == a) ? h : (h << 1);
	}

	public static boolean isPowerOf2(int n) {
		int b = n & (n - 1);
		return b == 0 && n != 0;
	}

	public static double lengthSqr(double x, double y, double z) {
		return x * x + y * y + z * z;
	}

	public static double length(double x, double y, double z) {
		return Math.sqrt(lengthSqr(x, y, z));
	}

	public static float rad(double angle) {
		if (angle == 0) return 0;
		return (float) (angle / 180 * Math.PI);
	}

	public static float deg(double angle) {
		if (angle == 0) return 0;
		return (float) (angle * 180 / Math.PI);
	}

	public static float angleLerp(double pct, double current, double target) {
		return (float) (current + getShortestAngleDiff(current, target) * pct);
	}

	public static float getShortestAngleDiff(double current, double target) {
		current = current % 360;
		target = target % 360;
		return (float) (((((target - current) % 360) + 540) % 360) - 180);
	}

	public static float diffuseLight(float x, float y, float z, boolean shaded) {
		if (!shaded) {
			return 1f;
		}
		return QuadLighter.calculateShade(x, y, z, false);
	}

	public static float diffuseLightNether(float x, float y, float z, boolean shaded) {
		if (!shaded) {
			return 0.9f;
		}
		return QuadLighter.calculateShade(x, y, z, true);
	}
}
