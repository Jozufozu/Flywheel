package com.jozufozu.flywheel.lib.instance;

import java.util.Objects;

import com.jozufozu.flywheel.api.instance.Instance;
import com.jozufozu.flywheel.api.instance.InstanceHandle;
import com.jozufozu.flywheel.api.instance.InstanceType;
import com.jozufozu.flywheel.api.instance.InstanceWriter;
import com.jozufozu.flywheel.api.layout.Layout;
import com.jozufozu.flywheel.lib.layout.BufferLayout;

import net.minecraft.resources.ResourceLocation;

public class SimpleInstanceType<I extends Instance> implements InstanceType<I> {
	private final Factory<I> factory;
	private final BufferLayout bufferLayout;
	private final Layout layout;
	private final InstanceWriter<I> writer;
	private final ResourceLocation vertexShader;
	private final ResourceLocation cullShader;

	public SimpleInstanceType(Factory<I> factory, BufferLayout bufferLayout, Layout layout, InstanceWriter<I> writer, ResourceLocation vertexShader, ResourceLocation cullShader) {
		this.factory = factory;
		this.bufferLayout = bufferLayout;
		this.layout = layout;
		this.writer = writer;
		this.vertexShader = vertexShader;
		this.cullShader = cullShader;
	}

	public static <I extends Instance> Builder<I> builder(Factory<I> factory) {
		return new Builder<>(factory);
	}

	@Override
	public I create(InstanceHandle handle) {
		return factory.create(this, handle);
	}

	@Override
	public BufferLayout oldLayout() {
		return bufferLayout;
	}

	@Override
	public Layout layout() {
		return layout;
	}

	@Override
	public InstanceWriter<I> writer() {
		return writer;
	}

	@Override
	public ResourceLocation vertexShader() {
		return vertexShader;
	}

	@Override
	public ResourceLocation cullShader() {
		return cullShader;
	}

	@FunctionalInterface
	public interface Factory<I extends Instance> {
		I create(InstanceType<I> type, InstanceHandle handle);
	}

	public static class Builder<I extends Instance> {
		private final Factory<I> factory;
		private BufferLayout bufferLayout;
		private Layout layout;
		private InstanceWriter<I> writer;
		private ResourceLocation vertexShader;
		private ResourceLocation cullShader;

		public Builder(Factory<I> factory) {
			this.factory = factory;
		}

		public Builder<I> bufferLayout(BufferLayout bufferLayout) {
			this.bufferLayout = bufferLayout;
			return this;
		}

		public Builder<I> layout(Layout layout) {
			this.layout = layout;
			return this;
		}

		public Builder<I> writer(InstanceWriter<I> writer) {
			this.writer = writer;
			return this;
		}

		public Builder<I> vertexShader(ResourceLocation vertexShader) {
			this.vertexShader = vertexShader;
			return this;
		}

		public Builder<I> cullShader(ResourceLocation cullShader) {
			this.cullShader = cullShader;
			return this;
		}

		public SimpleInstanceType<I> register() {
			Objects.requireNonNull(bufferLayout);
			Objects.requireNonNull(layout);
			Objects.requireNonNull(writer);
			Objects.requireNonNull(vertexShader);
			Objects.requireNonNull(cullShader);

			var out = new SimpleInstanceType<>(factory, bufferLayout, layout, writer, vertexShader, cullShader);
			return InstanceType.REGISTRY.registerAndGet(out);
		}
	}
}
