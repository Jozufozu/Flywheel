package com.jozufozu.flywheel.event;

import javax.annotation.Nullable;

import net.neoforged.bus.api.Event;

import org.joml.Matrix4f;

import com.jozufozu.flywheel.backend.RenderLayer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;

public class RenderLayerEvent extends Event {
	private final ClientLevel world;
	public final RenderType type;
	public final PoseStack stack;
	public final Matrix4f viewProjection;
	public final RenderBuffers buffers;
	public final double camX;
	public final double camY;
	public final double camZ;
	public final RenderLayer layer;

	public RenderLayerEvent(ClientLevel world, RenderType type, PoseStack stack, RenderBuffers buffers, double camX, double camY, double camZ) {
		this.world = world;
		this.type = type;
		this.stack = stack;

		viewProjection = new Matrix4f(stack.last()
				.pose());
        viewProjection.mulLocal(RenderSystem.getProjectionMatrix());

		this.buffers = buffers;
		this.camX = camX;
		this.camY = camY;
		this.camZ = camZ;

		this.layer = RenderLayer.getPrimaryLayer(type);
	}

	@Nullable
	public RenderLayer getLayer() {
		return layer;
	}

	public ClientLevel getWorld() {
		return world;
	}

	public RenderType getType() {
		return type;
	}

	public Matrix4f getViewProjection() {
		return viewProjection;
	}

	public double getCamX() {
		return camX;
	}

	public double getCamY() {
		return camY;
	}

	public double getCamZ() {
		return camZ;
	}

	@Override
	public String toString() {
		return "RenderLayerEvent[" + layer + "][" + "world=" + world + ", type=" + type + ", stack=" + stack + ", viewProjection=" + viewProjection + ", buffers=" + buffers + ", camX=" + camX + ", camY=" + camY + ", camZ=" + camZ + ']';
	}
}
