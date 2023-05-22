package com.jozufozu.flywheel.lib.task;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.jozufozu.flywheel.api.task.Plan;
import com.jozufozu.flywheel.api.task.TaskExecutor;

public record ForEachPlan<T, C>(Supplier<List<T>> listSupplier,
								BiConsumer<T, C> action) implements SimplyComposedPlan<C> {
	public static <T, C> Plan<C> of(Supplier<List<T>> iterable, BiConsumer<T, C> forEach) {
		return new ForEachPlan<>(iterable, forEach);
	}

	public static <T, C> Plan<C> of(Supplier<List<T>> iterable, Consumer<T> forEach) {
		return of(iterable, (t, c) -> forEach.accept(t));
	}

	@Override
	public void execute(TaskExecutor taskExecutor, C context, Runnable onCompletion) {
		taskExecutor.execute(() -> PlanUtil.distribute(taskExecutor, context, onCompletion, listSupplier.get(), action));
	}
}