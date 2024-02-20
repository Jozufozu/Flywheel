package com.jozufozu.flywheel.backend.engine;

import com.jozufozu.flywheel.api.model.IndexSequence;
import com.jozufozu.flywheel.backend.gl.GlNumericType;
import com.jozufozu.flywheel.backend.gl.array.GlVertexArray;
import com.jozufozu.flywheel.backend.gl.buffer.GlBuffer;
import com.jozufozu.flywheel.lib.memory.MemoryBlock;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

public class IndexPool {
	private final GlBuffer ebo;

	private final Reference2IntMap<IndexSequence> indexCounts;
	private final Reference2IntMap<IndexSequence> firstIndices;

	private boolean dirty;

    public IndexPool() {
        ebo = new GlBuffer();

		indexCounts = new Reference2IntOpenHashMap<>();
		firstIndices = new Reference2IntOpenHashMap<>();

		indexCounts.defaultReturnValue(0);
	}

	public int firstIndex(IndexSequence sequence) {
		return firstIndices.getInt(sequence);
	}

	public void reset() {
		indexCounts.clear();
		firstIndices.clear();
		dirty = true;
	}

	public void updateCount(IndexSequence sequence, int indexCount) {
		int oldCount = indexCounts.getInt(sequence);
		int newCount = Math.max(oldCount, indexCount);

		if (newCount > oldCount) {
			indexCounts.put(sequence, newCount);
			dirty = true;
		}
	}

	public void flush() {
		if (!dirty) {
			return;
		}

		firstIndices.clear();
		dirty = false;

		long totalIndexCount = 0;

		for (int count : indexCounts.values()) {
			totalIndexCount += count;
		}

		final var indexBlock = MemoryBlock.malloc(totalIndexCount * GlNumericType.UINT.byteWidth());
		final long indexPtr = indexBlock.ptr();

		int firstIndex = 0;
		for (Reference2IntMap.Entry<IndexSequence> entries : indexCounts.reference2IntEntrySet()) {
			var indexSequence = entries.getKey();
			var indexCount = entries.getIntValue();

			firstIndices.put(indexSequence, firstIndex);

			indexSequence.fill(indexPtr + (long) firstIndex * GlNumericType.UINT.byteWidth(), indexCount);

			firstIndex += indexCount;
		}

		ebo.upload(indexBlock);
		indexBlock.free();
	}

	public void bind(GlVertexArray vertexArray) {
		vertexArray.setElementBuffer(ebo.handle());
	}

	public void delete() {
		ebo.delete();
	}
}
