package com.jozufozu.flywheel.gl.buffer;

import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.system.MemoryUtil;

import com.jozufozu.flywheel.gl.error.GlError;
import com.jozufozu.flywheel.gl.error.GlException;

public class MappedBuffer implements AutoCloseable {
	private final int glBuffer;
	private long ptr;

	public MappedBuffer(int glBuffer, long size) {
		this.glBuffer = glBuffer;

		ptr = GlBuffer.IMPL.mapRange(glBuffer, 0, size, GL_MAP_WRITE_BIT);

		if (ptr == MemoryUtil.NULL) {
			throw new GlException(GlError.poll(), "Could not map buffer");
		}
	}

	public long ptr() {
		return ptr;
	}

	@Override
	public void close() {
		if (ptr == NULL) {
			return;
		}

		GlBuffer.IMPL.unmap(glBuffer);
		ptr = NULL;
	}
}