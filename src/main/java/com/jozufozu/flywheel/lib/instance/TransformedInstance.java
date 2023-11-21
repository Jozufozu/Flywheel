package com.jozufozu.flywheel.lib.instance;

import com.jozufozu.flywheel.api.instance.InstanceHandle;
import com.jozufozu.flywheel.api.instance.InstanceType;
import com.jozufozu.flywheel.lib.transform.Transform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;

import net.minecraft.util.Mth;

public class TransformedInstance extends ColoredLitInstance implements Transform<TransformedInstance> {
	private static final Matrix4f ZERO_MATRIX_4f = new Matrix4f();
	private static final Matrix3f ZERO_MATRIX_3f = new Matrix3f();

	public final Matrix4f model = new Matrix4f();
	public final Matrix3f normal = new Matrix3f();

	{
		model.setIdentity();
		normal.setIdentity();
	}

	public TransformedInstance(InstanceType<? extends TransformedInstance> type, InstanceHandle handle) {
		super(type, handle);
	}

	public TransformedInstance setTransform(PoseStack stack) {
		setChanged();

		this.model.load(stack.last()
				.pose());
		this.normal.load(stack.last()
				.normal());
		return this;
	}

	/**
	 * Sets the transform matrices to be all zeros.
	 *
	 * <p>
	 *     This will allow the gpu to quickly discard all geometry for this instance, effectively "turning it off".
	 * </p>
	 */
	public TransformedInstance setEmptyTransform() {
		setChanged();

		model.load(ZERO_MATRIX_4f);
		normal.load(ZERO_MATRIX_3f);
		return this;
	}

	public TransformedInstance loadIdentity() {
		setChanged();

		model.setIdentity();
		normal.setIdentity();
		return this;
	}

	@Override
	public TransformedInstance multiply(Quaternion quaternion) {
		setChanged();

		model.multiply(quaternion);
		normal.mul(quaternion);
		return this;
	}

	@Override
	public TransformedInstance scale(float x, float y, float z) {
		setChanged();

		model.multiply(Matrix4f.createScaleMatrix(x, y, z));

		if (x == y && y == z) {
			if (x < 0.0f) {
				normal.mul(-1.0f);
			}

			return this;
		}

		float invX = 1.0f / x;
		float invY = 1.0f / y;
		float invZ = 1.0f / z;
		float f = Mth.fastInvCubeRoot(Math.abs(invX * invY * invZ));
		normal.mul(Matrix3f.createScaleMatrix(f * invX, f * invY, f * invZ));
		return this;
	}

	@Override
	public TransformedInstance translate(double x, double y, double z) {
		setChanged();

		model.multiplyWithTranslation((float) x, (float) y, (float) z);
		return this;
	}

	@Override
	public TransformedInstance mulPose(Matrix4f pose) {
		setChanged();

		model.multiply(pose);
		return this;
	}

	@Override
	public TransformedInstance mulNormal(Matrix3f normal) {
		setChanged();

		normal.mul(normal);
		return this;
	}
}