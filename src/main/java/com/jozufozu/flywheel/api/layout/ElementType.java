package com.jozufozu.flywheel.api.layout;

public sealed interface ElementType permits ScalarElementType, VectorElementType, MatrixElementType {
	int byteSize();
}
