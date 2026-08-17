package io.github.piscescup.linq4j;

import io.github.piscescup.collection.EqualatorMap;
import io.github.piscescup.interfaces.Equalator;
import io.github.piscescup.interfaces.Pair;
import io.github.piscescup.interfaces.exfunction.BinFunction;
import io.github.piscescup.interfaces.exfunction.BinPredicate;
import io.github.piscescup.linq4j.base.Groupable;
import io.github.piscescup.linq4j.base.UnmodifiableGrouping;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

/**
 * Abstract base class for reference-type {@link Enumerable} pipeline stages.
 *
 * <p>A {@code ReferenceEnumPipeline} represents either the source stage of an
 * enumerable query or an intermediate operation that accepts elements of type
 * {@code T_IN} and produces elements of type {@code T_OUT}. It specializes
 * {@link AbstractEnumPipeline} by fixing the exposed enumerable type to
 * {@code Enumerable<T_OUT>}.</p>
 *
 * <p>Pipeline construction uses deferred execution. Invoking an intermediate
 * operation creates a new pipeline stage but does not enumerate the source.
 * Each call to {@link #enumerator()} creates an independent enumeration chain.</p>
 *
 * <p>Pipeline stages contain only query description state. Mutable state that
 * belongs to one traversal, such as indexes, queues, distinct-value caches,
 * buffers, and nested enumerators, is allocated by the corresponding
 * {@link PipelineEnumerator}.</p>
 *
 * @param <T_IN> the type of elements accepted from the upstream pipeline stage
 * @param <T_OUT> the type of elements produced by this pipeline stage
 * @author REN YuanTong
 * @since 1.0.0
 */
abstract class ReferenceEnumPipeline<T_IN, T_OUT>
    extends AbstractEnumPipeline<T_IN, T_OUT, Enumerable<T_OUT>>
    implements Enumerable<T_OUT> {

    /**
     * Creates the source stage of a reference enumerable pipeline.
     *
     * @param sourceSupplier the factory used to create source enumerators
     * @param parallel whether the source is configured for parallel evaluation
     */
    protected ReferenceEnumPipeline(
        @NotNull Supplier<? extends Enumerator<T_OUT>> sourceSupplier,
        boolean parallel
    ) {
        super(sourceSupplier, parallel);
    }

    /**
     * Creates an intermediate pipeline stage.
     *
     * @param upstream the immediately preceding pipeline stage
     */
    protected ReferenceEnumPipeline(
        @NotNull AbstractEnumPipeline<?, T_IN, ?> upstream
    ) {
        super(upstream);
    }

    // ---------------------------------------------------------------------
    // Aggregate terminal operations required by Enumerable
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final <A, R> R aggregateToResult(
        A seed,
        @NotNull BinFunction<? super A, ? super T_OUT, ? extends A> aggregator,
        @NotNull Function<? super A, ? extends R> resultSelector
    ) {
        NullCheck.requireNonNull(aggregator, "aggregator");
        NullCheck.requireNonNull(resultSelector, "resultSelector");

        A accumulator = seed;
        try (Enumerator<T_OUT> enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                accumulator = aggregator.apply(accumulator, enumerator.current());
            }
        }
        return resultSelector.apply(accumulator);
    }

    /** {@inheritDoc} */
    @Override
    public final <A> A aggregateToResult(
        @NotNull A seed,
        @NotNull BinFunction<? super A, ? super T_OUT, ? extends A> aggregator
    ) {
        NullCheck.requireNonNull(aggregator, "aggregator");
        A accumulator = seed;
        try (Enumerator<T_OUT> enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                accumulator = aggregator.apply(accumulator, enumerator.current());
            }
        }
        return accumulator;
    }

    /** {@inheritDoc} */
    @Override
    public final T_OUT aggregateToResult(
        @NotNull BinFunction<? super T_OUT, ? super T_OUT, ? extends T_OUT> aggregator
    ) {
        NullCheck.requireNonNull(aggregator, "aggregator");

        try (Enumerator<T_OUT> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            T_OUT accumulator = enumerator.current();
            while (enumerator.moveNext()) {
                accumulator = aggregator.apply(accumulator, enumerator.current());
            }
            return accumulator;
        }
    }

    // ---------------------------------------------------------------------
    // AggregateBy / CountBy
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final <K, A> @NotNull Enumerable<Pair<K, A>> aggregateBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull A seed,
        @NotNull BinFunction<? super A, ? super T_OUT, ? extends A> aggregator
    ) {
        return aggregateBy(keySelector, seed, aggregator, Equalator.defaultEqualator());
    }

    /** {@inheritDoc} */
    @Override
    public final <K, A> @NotNull Enumerable<Pair<K, A>> aggregateBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull A seed,
        @NotNull BinFunction<? super A, ? super T_OUT, ? extends A> aggregator,
        Equalator<? super K> keyEqualator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(aggregator, "aggregator");

        final Equalator<? super K> effectiveEqualator =
            keyEqualator != null
                ? keyEqualator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, Pair<K, A>>(this) {
            @Override
            protected @NotNull Enumerator<Pair<K, A>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, Pair<K, A>>(upstream) {
                    private final EqualatorMap<K, A> aggregates =
                        new EqualatorMap<>(effectiveEqualator);
                    private Iterator<Map.Entry<K, A>> iterator;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();
                            K key = keySelector.apply(element);

                            if (aggregates.containsKey(key)) {
                                aggregates.put(
                                    key,
                                    aggregator.apply(aggregates.get(key), element)
                                );
                            } else {
                                aggregates.put(
                                    key,
                                    aggregator.apply(seed, element)
                                );
                            }
                        }

                        iterator = aggregates.entrySet().iterator();
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (!iterator.hasNext()) {
                            return false;
                        }

                        Map.Entry<K, A> entry = iterator.next();
                        setCurrent(Pair.of(entry.getKey(), entry.getValue()));
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K, A> @NotNull Enumerable<Pair<K, A>> aggregateBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super K, ? extends A> seedSelector,
        @NotNull BinFunction<? super A, ? super T_OUT, ? extends A> aggregator
    ) {
        return aggregateBy(keySelector, seedSelector, aggregator, Equalator.defaultEqualator());
    }

    /** {@inheritDoc} */
    @Override
    public final <K, A> @NotNull Enumerable<Pair<K, A>> aggregateBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super K, ? extends A> seedSelector,
        @NotNull BinFunction<? super A, ? super T_OUT, ? extends A> aggregator,
        @Nullable Equalator<? super K> keyEqualator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(seedSelector, "seedSelector");
        NullCheck.requireNonNull(aggregator, "aggregator");

        final Equalator<? super K> effectiveEqualator =
            keyEqualator != null
                ? keyEqualator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, Pair<K, A>>(this) {
            @Override
            protected @NotNull Enumerator<Pair<K, A>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, Pair<K, A>>(upstream) {
                    private final EqualatorMap<K, A> aggregates =
                        new EqualatorMap<>(effectiveEqualator);
                    private Iterator<Map.Entry<K, A>> iterator;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();
                            K key = keySelector.apply(element);

                            if (aggregates.containsKey(key)) {
                                aggregates.put(
                                    key,
                                    aggregator.apply(aggregates.get(key), element)
                                );
                            } else {
                                aggregates.put(
                                    key,
                                    aggregator.apply(seedSelector.apply(key), element)
                                );
                            }
                        }

                        iterator = aggregates.entrySet().iterator();
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (!iterator.hasNext()) {
                            return false;
                        }

                        Map.Entry<K, A> entry = iterator.next();
                        setCurrent(Pair.of(entry.getKey(), entry.getValue()));
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final boolean all(Predicate<? super T_OUT> predicate) {
        NullCheck.requireNonNull(predicate, "predicate");
        try (Enumerator<T_OUT> enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (!predicate.test(enumerator.current())) {
                    return false;
                }
            }
            return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> append(T_OUT element) {
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private boolean appended;

                    @Override
                    protected boolean moveNextCore() {
                        if (upstream.moveNext()) {
                            setCurrent(upstream.current());
                            return true;
                        }
                        if (!appended) {
                            appended = true;
                            setCurrent(element);
                            return true;
                        }
                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <R> @NotNull Enumerable<R> cast(@NotNull Class<R> targetType) {
        NullCheck.requireNonNull(targetType, "targetType");
        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    @Override
                    protected boolean moveNextCore() {
                        if (!upstream.moveNext()) {
                            return false;
                        }
                        setCurrent(targetType.cast(upstream.current()));
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<Enumerable<T_OUT>> chunk(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than zero.");
        }

        return new StatelessOp<T_OUT, Enumerable<T_OUT>>(this) {
            @Override
            protected @NotNull Enumerator<Enumerable<T_OUT>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, Enumerable<T_OUT>>(upstream) {
                    @Override
                    protected boolean moveNextCore() {
                        List<T_OUT> chunk = new ArrayList<>(size);
                        while (chunk.size() < size && upstream.moveNext()) {
                            chunk.add(upstream.current());
                        }
                        if (chunk.isEmpty()) {
                            return false;
                        }
                        setCurrent(enumerableOfList(chunk));
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> concat(
        @NotNull Enumerable<? extends T_OUT> after
    ) {
        NullCheck.requireNonNull(after, "after");

        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private Enumerator<? extends T_OUT> second;
                    private boolean firstDone;

                    @Override
                    protected boolean moveNextCore() {
                        if (!firstDone) {
                            if (upstream.moveNext()) {
                                setCurrent(upstream.current());
                                return true;
                            }
                            firstDone = true;
                            second = after.enumerator();
                        }

                        if (second != null && second.moveNext()) {
                            setCurrent(second.current());
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void close() {
                        try {
                            if (second != null) {
                                second.close();
                                second = null;
                            }
                        } finally {
                            super.close();
                        }
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<Pair<K, Integer>> countBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        return countBy(keySelector, Equalator.defaultEqualator());
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<Pair<K, Integer>> countBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @Nullable Equalator<? super K> keyEqualator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");

        final Equalator<? super K> effectiveEqualator =
            keyEqualator != null
                ? keyEqualator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, Pair<K, Integer>>(this) {
            @Override
            protected @NotNull Enumerator<Pair<K, Integer>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, Pair<K, Integer>>(upstream) {
                    private final EqualatorMap<K, Integer> counts =
                        new EqualatorMap<>(effectiveEqualator);
                    private Iterator<Map.Entry<K, Integer>> iterator;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        while (upstream.moveNext()) {
                            K key = keySelector.apply(upstream.current());

                            if (counts.containsKey(key)) {
                                counts.put(
                                    key,
                                    Math.addExact(counts.get(key), 1)
                                );
                            } else {
                                counts.put(key, 1);
                            }
                        }

                        iterator = counts.entrySet().iterator();
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (!iterator.hasNext()) {
                            return false;
                        }

                        Map.Entry<K, Integer> entry = iterator.next();
                        setCurrent(Pair.of(entry.getKey(), entry.getValue()));
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> defaultIfEmpty(T_OUT defaultValue) {
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private boolean checked;
                    private boolean yieldedDefault;

                    @Override
                    protected boolean moveNextCore() {
                        if (!checked) {
                            checked = true;
                            if (upstream.moveNext()) {
                                setCurrent(upstream.current());
                                return true;
                            }
                            yieldedDefault = true;
                            setCurrent(defaultValue);
                            return true;
                        }

                        if (yieldedDefault) {
                            return false;
                        }

                        if (upstream.moveNext()) {
                            setCurrent(upstream.current());
                            return true;
                        }
                        return false;
                    }
                };
            }
        };
    }

    // ---------------------------------------------------------------------
    // Distinct / set operations
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> distinct() {
        return distinct(null);
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> distinct(
        @Nullable Equalator<? super T_OUT> equalator
    ) {
        final Equalator<? super T_OUT> effectiveEqualator =
            equalator != null
                ? equalator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final EqualatorMap<T_OUT, Boolean> seen =
                        new EqualatorMap<>(effectiveEqualator);

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (!seen.containsKey(element)) {
                                seen.put(element, Boolean.TRUE);
                                setCurrent(element);
                                return true;
                            }
                        }

                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> distinctBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        return distinctBy(keySelector, null);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> distinctBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @Nullable Equalator<? super K> keyEqualator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");

        final Equalator<? super K> effectiveEqualator =
            keyEqualator != null
                ? keyEqualator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final EqualatorMap<K, Boolean> seenKeys =
                        new EqualatorMap<>(effectiveEqualator);

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();
                            K key = keySelector.apply(element);

                            if (!seenKeys.containsKey(key)) {
                                seenKeys.put(key, Boolean.TRUE);
                                setCurrent(element);
                                return true;
                            }
                        }

                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> except(
        @NotNull Enumerable<? extends T_OUT> other
    ) {
        return except(other, null);
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> except(
        @NotNull Enumerable<? extends T_OUT> other,
        @Nullable Equalator<? super T_OUT> equalator
    ) {
        NullCheck.requireNonNull(other, "other");

        final Equalator<? super T_OUT> effectiveEqualator =
            equalator != null
                ? equalator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final EqualatorMap<T_OUT, Boolean> excluded =
                        new EqualatorMap<>(effectiveEqualator);
                    private final EqualatorMap<T_OUT, Boolean> yielded =
                        new EqualatorMap<>(effectiveEqualator);
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends T_OUT> enumerator = other.enumerator()) {
                            while (enumerator.moveNext()) {
                                excluded.put(enumerator.current(), Boolean.TRUE);
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (
                                !excluded.containsKey(element)
                                    && !yielded.containsKey(element)
                            ) {
                                yielded.put(element, Boolean.TRUE);
                                setCurrent(element);
                                return true;
                            }
                        }

                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> exceptBy(
        @NotNull Enumerable<? extends K> other,
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        return exceptBy(other, keySelector, null);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> exceptBy(
        @NotNull Enumerable<? extends K> second,
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @Nullable Equalator<? super K> equalator
    ) {
        NullCheck.requireNonNull(second, "second");
        NullCheck.requireNonNull(keySelector, "keySelector");

        final Equalator<? super K> effectiveEqualator =
            equalator != null
                ? equalator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final EqualatorMap<K, Boolean> excludedKeys =
                        new EqualatorMap<>(effectiveEqualator);
                    private final EqualatorMap<K, Boolean> yieldedKeys =
                        new EqualatorMap<>(effectiveEqualator);
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends K> enumerator = second.enumerator()) {
                            while (enumerator.moveNext()) {
                                excludedKeys.put(enumerator.current(), Boolean.TRUE);
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();
                            K key = keySelector.apply(element);

                            if (
                                !excludedKeys.containsKey(key)
                                    && !yieldedKeys.containsKey(key)
                            ) {
                                yieldedKeys.put(key, Boolean.TRUE);
                                setCurrent(element);
                                return true;
                            }
                        }

                        return false;
                    }
                };
            }
        };
    }

    // ---------------------------------------------------------------------
    // Grouping
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<Groupable<K, T_OUT>> groupBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        return groupBy(keySelector, Function.identity(), null);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<Groupable<K, T_OUT>> groupBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @Nullable Equalator<? super K> equalator
    ) {
        return groupBy(keySelector, Function.identity(), equalator);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, E> @NotNull Enumerable<Groupable<K, E>> groupBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends E> elementSelector
    ) {
        return groupBy(keySelector, elementSelector, null);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, E> @NotNull Enumerable<Groupable<K, E>> groupBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends E> elementSelector,
        @Nullable Equalator<? super K> equalator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(elementSelector, "elementSelector");

        final Equalator<? super K> effectiveEqualator =
            equalator != null
                ? equalator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, Groupable<K, E>>(this) {
            @Override
            protected @NotNull Enumerator<Groupable<K, E>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, Groupable<K, E>>(upstream) {
                    private final EqualatorMap<K, List<E>> groups =
                        new EqualatorMap<>(effectiveEqualator);
                    private Iterator<Map.Entry<K, List<E>>> iterator;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        while (upstream.moveNext()) {
                            T_OUT sourceElement = upstream.current();
                            K key = keySelector.apply(sourceElement);
                            E element = elementSelector.apply(sourceElement);

                            List<E> group;
                            if (groups.containsKey(key)) {
                                group = groups.get(key);
                            } else {
                                group = new ArrayList<>();
                                groups.put(key, group);
                            }

                            group.add(element);
                        }

                        iterator = groups.entrySet().iterator();
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (!iterator.hasNext()) {
                            return false;
                        }

                        Map.Entry<K, List<E>> entry = iterator.next();
                        setCurrent(
                            new UnmodifiableGrouping<>(
                                entry.getKey(),
                                entry.getValue()
                            )
                        );
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K, R> @NotNull Enumerable<R> groupToResult(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @Nullable BinFunction<? super K, ? super Enumerable<T_OUT>, ? extends R> resultSelector
    ) {
        NullCheck.requireNonNull(resultSelector, "resultSelector");
        return groupToResult(keySelector, resultSelector, null);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, R> @NotNull Enumerable<R> groupToResult(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull BinFunction<? super K, ? super Enumerable<T_OUT>, ? extends R> resultSelector,
        @Nullable Equalator<? super K> equalator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(resultSelector, "resultSelector");
        return groupBy(keySelector, equalator)
            .select(group -> resultSelector.apply(
                group.getGroupKey(),
                enumerableOfList(group.getGroupElements())
            ));
    }

    /** {@inheritDoc} */
    @Override
    public final <K, E, R> @NotNull Enumerable<R> groupToResult(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends E> elementSelector,
        @NotNull BinFunction<? super K, ? super Enumerable<E>, ? extends R> resultSelector
    ) {
        return groupToResult(keySelector, elementSelector, resultSelector, null);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, E, R> @NotNull Enumerable<R> groupToResult(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends E> elementSelector,
        @NotNull BinFunction<? super K, ? super Enumerable<E>, ? extends R> resultSelector,
        @Nullable Equalator<? super K> equalator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(elementSelector, "elementSelector");
        NullCheck.requireNonNull(resultSelector, "resultSelector");
        return this.<K, E>groupBy(keySelector, elementSelector, equalator)
            .select((Groupable<K, E> group) -> resultSelector.apply(
                group.getGroupKey(),
                enumerableOfList(new ArrayList<>(group.getGroupElements()))
            ));
    }

    // ---------------------------------------------------------------------
    // Joins
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> groupJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, ? super Enumerable<I>, ? extends R> resultSelector
    ) {
        return groupJoin(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            null
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> groupJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, ? super Enumerable<I>, ? extends R> resultSelector,
        @Nullable Equalator<? super K> equalator
    ) {
        NullCheck.requireNonNull(inner, "inner");
        NullCheck.requireNonNull(outerKeySelector, "outerKeySelector");
        NullCheck.requireNonNull(innerKeySelector, "innerKeySelector");
        NullCheck.requireNonNull(resultSelector, "resultSelector");

        final Equalator<? super K> effectiveEqualator =
            equalator != null
                ? equalator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private final EqualatorMap<K, List<I>> lookup =
                        new EqualatorMap<>(effectiveEqualator);
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends I> enumerator = inner.enumerator()) {
                            while (enumerator.moveNext()) {
                                I value = enumerator.current();
                                K key = innerKeySelector.apply(value);

                                List<I> group;
                                if (lookup.containsKey(key)) {
                                    group = lookup.get(key);
                                } else {
                                    group = new ArrayList<>();
                                    lookup.put(key, group);
                                }

                                group.add(value);
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (!upstream.moveNext()) {
                            return false;
                        }

                        T_OUT outer = upstream.current();
                        K key = outerKeySelector.apply(outer);
                        List<I> matches =
                            lookup.containsKey(key)
                                ? lookup.get(key)
                                : Collections.emptyList();

                        setCurrent(
                            resultSelector.apply(
                                outer,
                                enumerableOfList(matches)
                            )
                        );
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> intersect(
        @NotNull Enumerable<? extends T_OUT> other
    ) {
        return intersect(other, null);
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> intersect(
        @NotNull Enumerable<? extends T_OUT> second,
        @Nullable Equalator<? super T_OUT> equalator
    ) {
        NullCheck.requireNonNull(second, "second");

        final Equalator<? super T_OUT> effectiveEqualator =
            equalator != null
                ? equalator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final EqualatorMap<T_OUT, Boolean> remaining =
                        new EqualatorMap<>(effectiveEqualator);
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends T_OUT> enumerator = second.enumerator()) {
                            while (enumerator.moveNext()) {
                                remaining.put(enumerator.current(), Boolean.TRUE);
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (remaining.containsKey(element)) {
                                remaining.remove(element);
                                setCurrent(element);
                                return true;
                            }
                        }

                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> intersectBy(
        @NotNull Enumerable<? extends K> other,
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        return intersectBy(other, keySelector, null);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> intersectBy(
        @NotNull Enumerable<? extends K> second,
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @Nullable Equalator<? super K> equalator
    ) {
        NullCheck.requireNonNull(second, "second");
        NullCheck.requireNonNull(keySelector, "keySelector");

        final Equalator<? super K> effectiveEqualator =
            equalator != null
                ? equalator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final EqualatorMap<K, Boolean> remainingKeys =
                        new EqualatorMap<>(effectiveEqualator);
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends K> enumerator = second.enumerator()) {
                            while (enumerator.moveNext()) {
                                remainingKeys.put(enumerator.current(), Boolean.TRUE);
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();
                            K key = keySelector.apply(element);

                            if (remainingKeys.containsKey(key)) {
                                remainingKeys.remove(key);
                                setCurrent(element);
                                return true;
                            }
                        }

                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> join(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, ? super I, ? extends R> resultSelector
    ) {
        return join(inner, outerKeySelector, innerKeySelector, resultSelector, null);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> join(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, ? super I, ? extends R> resultSelector,
        @Nullable Equalator<? super K> equalator
    ) {
        NullCheck.requireNonNull(inner, "inner");
        NullCheck.requireNonNull(outerKeySelector, "outerKeySelector");
        NullCheck.requireNonNull(innerKeySelector, "innerKeySelector");
        NullCheck.requireNonNull(resultSelector, "resultSelector");

        final Equalator<? super K> effectiveEqualator =
            equalator != null
                ? equalator
                : Equalator.defaultEqualator();

        return new StatefulOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private final EqualatorMap<K, List<I>> lookup =
                        new EqualatorMap<>(effectiveEqualator);
                    private List<I> matches = Collections.emptyList();
                    private T_OUT currentOuter;
                    private int matchIndex;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends I> enumerator = inner.enumerator()) {
                            while (enumerator.moveNext()) {
                                I value = enumerator.current();
                                K key = innerKeySelector.apply(value);

                                List<I> group;
                                if (lookup.containsKey(key)) {
                                    group = lookup.get(key);
                                } else {
                                    group = new ArrayList<>();
                                    lookup.put(key, group);
                                }

                                group.add(value);
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (true) {
                            if (matchIndex < matches.size()) {
                                setCurrent(
                                    resultSelector.apply(
                                        currentOuter,
                                        matches.get(matchIndex++)
                                    )
                                );
                                return true;
                            }

                            if (!upstream.moveNext()) {
                                return false;
                            }

                            currentOuter = upstream.current();
                            K key = outerKeySelector.apply(currentOuter);
                            matches =
                                lookup.containsKey(key)
                                    ? lookup.get(key)
                                    : Collections.emptyList();
                            matchIndex = 0;
                        }
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> leftJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, @Nullable I, ? extends R> resultSelector
    ) {
        return leftJoin(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            Equalator.defaultEqualator()
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> leftJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, @Nullable I, ? extends R> resultSelector,
        @NotNull Equalator<? super K> equalator
    ) {
        NullCheck.requireNonNull(inner, "inner");
        NullCheck.requireNonNull(outerKeySelector, "outerKeySelector");
        NullCheck.requireNonNull(innerKeySelector, "innerKeySelector");
        NullCheck.requireNonNull(resultSelector, "resultSelector");
        NullCheck.requireNonNull(equalator, "equalator");

        return new StatefulOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private final EqualatorMap<K, List<I>> lookup =
                        new EqualatorMap<>(equalator);
                    private List<I> matches = Collections.emptyList();
                    private T_OUT currentOuter;
                    private int matchIndex;
                    private boolean pendingUnmatched;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends I> enumerator = inner.enumerator()) {
                            while (enumerator.moveNext()) {
                                I value = enumerator.current();
                                K key = innerKeySelector.apply(value);

                                List<I> group;
                                if (lookup.containsKey(key)) {
                                    group = lookup.get(key);
                                } else {
                                    group = new ArrayList<>();
                                    lookup.put(key, group);
                                }

                                group.add(value);
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (true) {
                            if (matchIndex < matches.size()) {
                                setCurrent(
                                    resultSelector.apply(
                                        currentOuter,
                                        matches.get(matchIndex++)
                                    )
                                );
                                return true;
                            }

                            if (pendingUnmatched) {
                                pendingUnmatched = false;
                                setCurrent(resultSelector.apply(currentOuter, null));
                                return true;
                            }

                            if (!upstream.moveNext()) {
                                return false;
                            }

                            currentOuter = upstream.current();
                            K key = outerKeySelector.apply(currentOuter);
                            matches =
                                lookup.containsKey(key)
                                    ? lookup.get(key)
                                    : Collections.emptyList();
                            matchIndex = 0;
                            pendingUnmatched = matches.isEmpty();
                        }
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I> @NotNull Enumerable<Pair<T_OUT, @Nullable I>> leftJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector
    ) {
        BinFunction<T_OUT, I, Pair<T_OUT, I>> selector = Pair::of;
        return leftJoin(inner, outerKeySelector, innerKeySelector, selector);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I> @NotNull Enumerable<Pair<T_OUT, @Nullable I>> leftJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull Equalator<? super K> equalator
    ) {
        BinFunction<T_OUT, I, Pair<T_OUT, I>> selector = Pair::of;
        return leftJoin(inner, outerKeySelector, innerKeySelector, selector, equalator);
    }

    // ---------------------------------------------------------------------
    // Ordering
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public final @NotNull OrderedEnumerable<T_OUT> order() {
        Comparator<T_OUT> comparator = (left, right) ->
            ((Comparable<Object>) left).compareTo(right);
        return new OrderedOp<>(this, comparator);
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> order(
        @NotNull Comparator<? super T_OUT> comparator
    ) {
        NullCheck.requireNonNull(comparator, "comparator");
        return new OrderedOp<>(this, comparator);
    }

    /** {@inheritDoc} */
    @Override
    public final <K extends Comparable<? super K>> @NotNull OrderedEnumerable<T_OUT> orderBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, Comparator.comparing(keySelector));
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull OrderedEnumerable<T_OUT> orderBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(comparator, "comparator");
        return new OrderedOp<>(this, Comparator.comparing(keySelector, comparator));
    }

    /** {@inheritDoc} */
    @Override
    public final <K extends Comparable<? super K>> @NotNull OrderedEnumerable<T_OUT> orderByDescending(
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, Comparator.comparing(keySelector).reversed());
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull OrderedEnumerable<T_OUT> orderByDescending(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(comparator, "comparator");
        return new OrderedOp<>(this, Comparator.comparing(keySelector, comparator).reversed());
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByInt(
        @NotNull ToIntFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, Comparator.comparingInt(keySelector));
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByIntDescending(
        @NotNull ToIntFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, Comparator.comparingInt(keySelector).reversed());
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByLong(
        @NotNull ToLongFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, Comparator.comparingLong(keySelector));
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByLongDescending(
        @NotNull ToLongFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, Comparator.comparingLong(keySelector).reversed());
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByDouble(
        @NotNull ToDoubleFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, Comparator.comparingDouble(keySelector));
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByDoubleDescending(
        @NotNull ToDoubleFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, Comparator.comparingDouble(keySelector).reversed());
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> prepend(@Nullable T_OUT element) {
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private boolean prepended;

                    @Override
                    protected boolean moveNextCore() {
                        if (!prepended) {
                            prepended = true;
                            setCurrent(element);
                            return true;
                        }
                        if (upstream.moveNext()) {
                            setCurrent(upstream.current());
                            return true;
                        }
                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> reverse() {
        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final List<T_OUT> buffer = new ArrayList<>();
                    private boolean initialized;
                    private int index;

                    @Override
                    protected boolean moveNextCore() {
                        if (!initialized) {
                            initialized = true;
                            while (upstream.moveNext()) {
                                buffer.add(upstream.current());
                            }
                            index = buffer.size() - 1;
                        }
                        if (index < 0) {
                            return false;
                        }
                        setCurrent(buffer.get(index--));
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <I, K, R> @NotNull Enumerable<R> rightJoin(
        @NotNull Iterable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, ? super I, ? extends R> resultSelector
    ) {
        return rightJoin(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            Equalator.defaultEqualator()
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <I, K, R> @NotNull Enumerable<R> rightJoin(
        @NotNull Iterable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, ? super I, ? extends R> resultSelector,
        @NotNull Equalator<? super K> equalator
    ) {
        NullCheck.requireNonNull(inner, "inner");
        NullCheck.requireNonNull(outerKeySelector, "outerKeySelector");
        NullCheck.requireNonNull(innerKeySelector, "innerKeySelector");
        NullCheck.requireNonNull(resultSelector, "resultSelector");
        NullCheck.requireNonNull(equalator, "equalator");

        return new StatefulOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private final EqualatorMap<K, List<T_OUT>> lookup =
                        new EqualatorMap<>(equalator);
                    private Iterator<? extends I> innerIterator;
                    private List<T_OUT> matches = Collections.emptyList();
                    private I currentInner;
                    private int matchIndex;
                    private boolean pendingUnmatched;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        while (upstream.moveNext()) {
                            T_OUT outer = upstream.current();
                            K key = outerKeySelector.apply(outer);

                            List<T_OUT> group;
                            if (lookup.containsKey(key)) {
                                group = lookup.get(key);
                            } else {
                                group = new ArrayList<>();
                                lookup.put(key, group);
                            }

                            group.add(outer);
                        }

                        innerIterator = inner.iterator();
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (true) {
                            if (matchIndex < matches.size()) {
                                setCurrent(
                                    resultSelector.apply(
                                        matches.get(matchIndex++),
                                        currentInner
                                    )
                                );
                                return true;
                            }

                            if (pendingUnmatched) {
                                pendingUnmatched = false;
                                setCurrent(resultSelector.apply(null, currentInner));
                                return true;
                            }

                            if (!innerIterator.hasNext()) {
                                return false;
                            }

                            currentInner = innerIterator.next();
                            K key = innerKeySelector.apply(currentInner);
                            matches =
                                lookup.containsKey(key)
                                    ? lookup.get(key)
                                    : Collections.emptyList();
                            matchIndex = 0;
                            pendingUnmatched = matches.isEmpty();
                        }
                    }
                };
            }
        };
    }

    // ---------------------------------------------------------------------
    // Projection / flattening
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final <R> @NotNull Enumerable<R> select(
        @NotNull Function<? super T_OUT, ? extends R> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");
        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    @Override
                    protected boolean moveNextCore() {
                        if (!upstream.moveNext()) return false;
                        setCurrent(selector.apply(upstream.current()));
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <R> @NotNull Enumerable<R> select(
        @NotNull BinFunction<? super T_OUT, Integer, ? extends R> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");
        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private int index;

                    @Override
                    protected boolean moveNextCore() {
                        if (!upstream.moveNext()) return false;
                        setCurrent(selector.apply(upstream.current(), index++));
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <R> @NotNull Enumerable<R> selectMany(
        @NotNull Function<? super T_OUT, ? extends Enumerable<? extends R>> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");
        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private Enumerator<? extends R> inner;

                    @Override
                    protected boolean moveNextCore() {
                        while (true) {
                            if (inner != null) {
                                if (inner.moveNext()) {
                                    setCurrent(inner.current());
                                    return true;
                                }
                                inner.close();
                                inner = null;
                            }
                            if (!upstream.moveNext()) {
                                return false;
                            }
                            inner = Objects.requireNonNull(
                                selector.apply(upstream.current()),
                                "selector returned null"
                            ).enumerator();
                        }
                    }

                    @Override
                    public void close() {
                        try {
                            if (inner != null) {
                                inner.close();
                                inner = null;
                            }
                        } finally {
                            super.close();
                        }
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <R> @NotNull Enumerable<R> selectMany(
        @NotNull BinFunction<? super T_OUT, Integer, ? extends Enumerable<? extends R>> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");
        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private int index;
                    private Enumerator<? extends R> inner;

                    @Override
                    protected boolean moveNextCore() {
                        while (true) {
                            if (inner != null) {
                                if (inner.moveNext()) {
                                    setCurrent(inner.current());
                                    return true;
                                }
                                inner.close();
                                inner = null;
                            }
                            if (!upstream.moveNext()) {
                                return false;
                            }
                            inner = Objects.requireNonNull(
                                selector.apply(upstream.current(), index++),
                                "selector returned null"
                            ).enumerator();
                        }
                    }

                    @Override
                    public void close() {
                        try {
                            if (inner != null) {
                                inner.close();
                                inner = null;
                            }
                        } finally {
                            super.close();
                        }
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <C, R> @NotNull Enumerable<R> selectMany(
        @NotNull Function<? super T_OUT, ? extends Iterable<? extends C>> collectionSelector,
        @NotNull BinFunction<? super T_OUT, ? super C, ? extends R> resultSelector
    ) {
        NullCheck.requireNonNull(collectionSelector, "collectionSelector");
        NullCheck.requireNonNull(resultSelector, "resultSelector");

        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private T_OUT outer;
                    private Iterator<? extends C> inner;

                    @Override
                    protected boolean moveNextCore() {
                        while (true) {
                            if (inner != null && inner.hasNext()) {
                                setCurrent(resultSelector.apply(outer, inner.next()));
                                return true;
                            }
                            if (!upstream.moveNext()) {
                                return false;
                            }
                            outer = upstream.current();
                            inner = Objects.requireNonNull(
                                collectionSelector.apply(outer),
                                "collectionSelector returned null"
                            ).iterator();
                        }
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <C, R> @NotNull Enumerable<R> selectMany(
        @NotNull BinFunction<? super T_OUT, Integer, ? extends Iterable<? extends C>> collectionSelector,
        @NotNull BinFunction<? super T_OUT, ? super C, ? extends R> resultSelector
    ) {
        NullCheck.requireNonNull(collectionSelector, "collectionSelector");
        NullCheck.requireNonNull(resultSelector, "resultSelector");

        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private int index;
                    private T_OUT outer;
                    private Iterator<? extends C> inner;

                    @Override
                    protected boolean moveNextCore() {
                        while (true) {
                            if (inner != null && inner.hasNext()) {
                                setCurrent(resultSelector.apply(outer, inner.next()));
                                return true;
                            }
                            if (!upstream.moveNext()) {
                                return false;
                            }
                            outer = upstream.current();
                            inner = Objects.requireNonNull(
                                collectionSelector.apply(outer, index++),
                                "collectionSelector returned null"
                            ).iterator();
                        }
                    }
                };
            }
        };
    }

    // ---------------------------------------------------------------------
    // Sequence equality / shuffle / slicing
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final boolean sequenceEqual(
        @NotNull Enumerable<? extends T_OUT> other
    ) {
        return sequenceEqual(other, Equalator.defaultEqualator());
    }

    /** {@inheritDoc} */
    @Override
    public final boolean sequenceEqual(
        @NotNull Enumerable<? extends T_OUT> other,
        @NotNull Equalator<? super T_OUT> equalator
    ) {
        NullCheck.requireNonNull(other, "other");
        NullCheck.requireNonNull(equalator, "equalator");

        try (
            Enumerator<T_OUT> first = enumerator();
            Enumerator<? extends T_OUT> second = other.enumerator()
        ) {
            while (true) {
                boolean firstHas = first.moveNext();
                boolean secondHas = second.moveNext();
                if (firstHas != secondHas) return false;
                if (!firstHas) return true;
                if (!equalator.equals(first.current(), second.current())) return false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> shuffle() {
        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final List<T_OUT> buffer = new ArrayList<>();
                    private boolean initialized;
                    private int index;

                    @Override
                    protected boolean moveNextCore() {
                        if (!initialized) {
                            initialized = true;
                            while (upstream.moveNext()) {
                                buffer.add(upstream.current());
                            }
                            Collections.shuffle(buffer);
                        }
                        if (index >= buffer.size()) return false;
                        setCurrent(buffer.get(index++));
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> skip(int count) {
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private int remaining = Math.max(0, count);

                    @Override
                    protected boolean moveNextCore() {
                        while (remaining > 0) {
                            if (!upstream.moveNext()) return false;
                            remaining--;
                        }
                        if (!upstream.moveNext()) return false;
                        setCurrent(upstream.current());
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> skipLast(int count) {
        final int skipCount = Math.max(0, count);
        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final Deque<T_OUT> queue = new LinkedList<>();

                    @Override
                    protected boolean moveNextCore() {
                        if (skipCount == 0) {
                            if (!upstream.moveNext()) return false;
                            setCurrent(upstream.current());
                            return true;
                        }

                        while (upstream.moveNext()) {
                            queue.addLast(upstream.current());
                            if (queue.size() > skipCount) {
                                setCurrent(queue.removeFirst());
                                return true;
                            }
                        }
                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> skipWhile(
        @NotNull Predicate<? super T_OUT> predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private boolean skipping = true;

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();
                            if (skipping && predicate.test(element)) continue;
                            skipping = false;
                            setCurrent(element);
                            return true;
                        }
                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> skipWhile(
        @NotNull BinPredicate<? super T_OUT, Integer> predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private int index;
                    private boolean skipping = true;

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();
                            if (skipping && predicate.test(element, index++)) continue;
                            skipping = false;
                            setCurrent(element);
                            return true;
                        }
                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> take(int count) {
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private int remaining = Math.max(0, count);

                    @Override
                    protected boolean moveNextCore() {
                        if (remaining <= 0 || !upstream.moveNext()) return false;
                        remaining--;
                        setCurrent(upstream.current());
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> takeLast(int count) {
        final int takeCount = Math.max(0, count);
        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final Deque<T_OUT> queue = new LinkedList<>();
                    private boolean initialized;
                    private Iterator<T_OUT> iterator;

                    @Override
                    protected boolean moveNextCore() {
                        if (!initialized) {
                            initialized = true;
                            if (takeCount == 0) return false;
                            while (upstream.moveNext()) {
                                if (queue.size() == takeCount) queue.removeFirst();
                                queue.addLast(upstream.current());
                            }
                            iterator = queue.iterator();
                        }
                        if (iterator == null || !iterator.hasNext()) return false;
                        setCurrent(iterator.next());
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> takeWhile(
        @NotNull Predicate<? super T_OUT> predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private boolean taking = true;

                    @Override
                    protected boolean moveNextCore() {
                        if (!taking || !upstream.moveNext()) return false;
                        T_OUT element = upstream.current();
                        if (!predicate.test(element)) {
                            taking = false;
                            return false;
                        }
                        setCurrent(element);
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> takeWhile(
        @NotNull BinPredicate<? super T_OUT, Integer> predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private int index;
                    private boolean taking = true;

                    @Override
                    protected boolean moveNextCore() {
                        if (!taking || !upstream.moveNext()) return false;
                        T_OUT element = upstream.current();
                        if (!predicate.test(element, index++)) {
                            taking = false;
                            return false;
                        }
                        setCurrent(element);
                        return true;
                    }
                };
            }
        };
    }

    // ---------------------------------------------------------------------
    // Union / filtering / zip
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> union(
        @NotNull Enumerable<? extends T_OUT> other
    ) {
        return union(other, Equalator.defaultEqualator());
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> union(
        @NotNull Enumerable<? extends T_OUT> other,
        @NotNull Equalator<? super T_OUT> comparer
    ) {
        NullCheck.requireNonNull(other, "other");
        NullCheck.requireNonNull(comparer, "comparer");

        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final EqualatorMap<T_OUT, Boolean> seen =
                        new EqualatorMap<>(comparer);
                    private Enumerator<? extends T_OUT> second;
                    private boolean firstDone;

                    @Override
                    protected boolean moveNextCore() {
                        while (true) {
                            if (!firstDone) {
                                while (upstream.moveNext()) {
                                    T_OUT element = upstream.current();

                                    if (!seen.containsKey(element)) {
                                        seen.put(element, Boolean.TRUE);
                                        setCurrent(element);
                                        return true;
                                    }
                                }

                                firstDone = true;
                                second = other.enumerator();
                            }

                            while (second != null && second.moveNext()) {
                                T_OUT element = second.current();

                                if (!seen.containsKey(element)) {
                                    seen.put(element, Boolean.TRUE);
                                    setCurrent(element);
                                    return true;
                                }
                            }

                            return false;
                        }
                    }

                    @Override
                    public void close() {
                        try {
                            if (second != null) {
                                second.close();
                                second = null;
                            }
                        } finally {
                            super.close();
                        }
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> unionBy(
        @NotNull Enumerable<? extends T_OUT> second,
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        return unionBy(second, keySelector, Equalator.defaultEqualator());
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> unionBy(
        @NotNull Enumerable<? extends T_OUT> second,
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Equalator<? super K> comparer
    ) {
        NullCheck.requireNonNull(second, "second");
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(comparer, "comparer");

        return new StatefulOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private final EqualatorMap<K, Boolean> seenKeys =
                        new EqualatorMap<>(comparer);
                    private Enumerator<? extends T_OUT> secondEnumerator;
                    private boolean firstDone;

                    @Override
                    protected boolean moveNextCore() {
                        while (true) {
                            if (!firstDone) {
                                while (upstream.moveNext()) {
                                    T_OUT element = upstream.current();
                                    K key = keySelector.apply(element);

                                    if (!seenKeys.containsKey(key)) {
                                        seenKeys.put(key, Boolean.TRUE);
                                        setCurrent(element);
                                        return true;
                                    }
                                }

                                firstDone = true;
                                secondEnumerator = second.enumerator();
                            }

                            while (
                                secondEnumerator != null
                                    && secondEnumerator.moveNext()
                            ) {
                                T_OUT element = secondEnumerator.current();
                                K key = keySelector.apply(element);

                                if (!seenKeys.containsKey(key)) {
                                    seenKeys.put(key, Boolean.TRUE);
                                    setCurrent(element);
                                    return true;
                                }
                            }

                            return false;
                        }
                    }

                    @Override
                    public void close() {
                        try {
                            if (secondEnumerator != null) {
                                secondEnumerator.close();
                                secondEnumerator = null;
                            }
                        } finally {
                            super.close();
                        }
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> where(
        @NotNull Predicate<? super T_OUT> predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            T_OUT value = upstream.current();
                            if (predicate.test(value)) {
                                setCurrent(value);
                                return true;
                            }
                        }
                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> where(
        @NotNull BinPredicate<? super T_OUT, Integer> predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, T_OUT>(upstream) {
                    private int index;

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            T_OUT value = upstream.current();
                            if (predicate.test(value, index++)) {
                                setCurrent(value);
                                return true;
                            }
                        }
                        return false;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final <U> @NotNull Enumerable<Pair<T_OUT, U>> zip(
        @NotNull Enumerable<? extends U> second
    ) {
        return zip(second, Pair::of);
    }

    /** {@inheritDoc} */
    @Override
    public final <U, R> @NotNull Enumerable<R> zip(
        @NotNull Enumerable<? extends U> second,
        @NotNull BinFunction<? super T_OUT, ? super U, ? extends R> resultSelector
    ) {
        NullCheck.requireNonNull(second, "second");
        NullCheck.requireNonNull(resultSelector, "resultSelector");

        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<T_OUT, R>(upstream) {
                    private Enumerator<? extends U> secondEnumerator;

                    @Override
                    protected boolean moveNextCore() {
                        if (secondEnumerator == null) {
                            secondEnumerator = second.enumerator();
                        }
                        if (!upstream.moveNext() || !secondEnumerator.moveNext()) {
                            return false;
                        }
                        setCurrent(resultSelector.apply(
                            upstream.current(),
                            secondEnumerator.current()
                        ));
                        return true;
                    }

                    @Override
                    public void close() {
                        try {
                            if (secondEnumerator != null) {
                                secondEnumerator.close();
                                secondEnumerator = null;
                            }
                        } finally {
                            super.close();
                        }
                    }
                };
            }
        };
    }

    // ---------------------------------------------------------------------
    // Map materialization
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Map<K, T_OUT> toMap(
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        return toMap(keySelector, Function.identity());
    }

    /** {@inheritDoc} */
    @Override
    public final <K, V> @NotNull Map<K, V> toMap(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends V> elementSelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(elementSelector, "elementSelector");

        Map<K, V> result = new LinkedHashMap<>();
        try (Enumerator<T_OUT> e = enumerator()) {
            while (e.moveNext()) {
                T_OUT element = e.current();
                K key = Objects.requireNonNull(
                    keySelector.apply(element),
                    "keySelector produced a null key"
                );
                if (result.containsKey(key)) {
                    throw new IllegalStateException("Duplicate key: " + key);
                }
                result.put(key, elementSelector.apply(element));
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Map<K, T_OUT> toMap(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Equalator<? super K> comparer
    ) {
        return toMap(keySelector, Function.identity(), comparer);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, V> @NotNull Map<K, V> toMap(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends V> elementSelector,
        @NotNull Equalator<? super K> comparer
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(elementSelector, "elementSelector");
        NullCheck.requireNonNull(comparer, "comparer");

        EqualatorMap<K, V> result = new EqualatorMap<>(comparer);
        try (Enumerator<T_OUT> e = enumerator()) {
            while (e.moveNext()) {
                T_OUT element = e.current();
                K key = Objects.requireNonNull(
                    keySelector.apply(element),
                    "keySelector produced a null key"
                );
                if (result.containsKey(key)) {
                    throw new IllegalStateException("Duplicate key: " + key);
                }
                result.put(key, elementSelector.apply(element));
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Pipeline stage classes
    // ---------------------------------------------------------------------

    /**
     * Source stage of a reference enumerable pipeline.
     *
     * @param <T> the source element type
     */
    static final class Head<T> extends ReferenceEnumPipeline<T, T> {
        Head(
            @NotNull Supplier<? extends Enumerator<T>> sourceSupplier,
            boolean parallel
        ) {
            super(sourceSupplier, parallel);
        }

        Head(
            @NotNull Supplier<? extends Enumerator<T>> sourceSupplier
        ) {
            this(sourceSupplier, false);
        }

        @Override
        protected boolean opIsStateful() {
            throw new UnsupportedOperationException(
                "The source stage does not represent an operation."
            );
        }

        @Override
        protected @NotNull Enumerator<T> opWrapEnumerator(
            @NotNull Enumerator<T> upstream
        ) {
            throw new UnsupportedOperationException(
                "The source stage has no upstream enumerator."
            );
        }
    }

    /**
     * Base class for stateless reference-pipeline stages.
     *
     * @param <T_IN> the input element type
     * @param <T_OUT> the output element type
     */
    abstract static class StatelessOp<T_IN, T_OUT>
        extends ReferenceEnumPipeline<T_IN, T_OUT> {

        protected StatelessOp(
            @NotNull AbstractEnumPipeline<?, T_IN, ?> upstream
        ) {
            super(upstream);
        }

        @Override
        protected final boolean opIsStateful() {
            return false;
        }
    }

    /**
     * Base class for stateful reference-pipeline stages.
     *
     * @param <T_IN> the input element type
     * @param <T_OUT> the output element type
     */
    abstract static class StatefulOp<T_IN, T_OUT>
        extends ReferenceEnumPipeline<T_IN, T_OUT> {

        protected StatefulOp(
            @NotNull AbstractEnumPipeline<?, T_IN, ?> upstream
        ) {
            super(upstream);
        }

        @Override
        protected final boolean opIsStateful() {
            return true;
        }
    }

    /**
     * Stateful ordered stage. The comparator describes the complete ordering
     * chain. Enumeration buffers the upstream sequence, performs a stable sort,
     * and then exposes the sorted elements.
     *
     * @param <T> the element type
     */
    private static final class OrderedOp<T>
        extends StatefulOp<T, T>
        implements OrderedEnumerable<T> {

        private final AbstractEnumPipeline<?, T, ?> orderingSource;
        private final Comparator<T> comparator;

        OrderedOp(
            @NotNull AbstractEnumPipeline<?, T, ?> orderingSource,
            @NotNull Comparator<? super T> comparator
        ) {
            super(orderingSource);
            this.orderingSource = orderingSource;
            this.comparator = (left, right) -> comparator.compare(left, right);
        }

        @Override
        protected @NotNull Enumerator<T> opWrapEnumerator(
            @NotNull Enumerator<T> upstream
        ) {
            return new PipelineEnumerator<T, T>(upstream) {
                private final List<T> buffer = new ArrayList<>();
                private boolean initialized;
                private int index;

                @Override
                protected boolean moveNextCore() {
                    if (!initialized) {
                        initialized = true;
                        while (upstream.moveNext()) {
                            buffer.add(upstream.current());
                        }
                        buffer.sort(comparator);
                    }
                    if (index >= buffer.size()) return false;
                    setCurrent(buffer.get(index++));
                    return true;
                }
            };
        }

        @Override
        public final <K extends Comparable<? super K>> @NotNull OrderedEnumerable<T> thenBy(
            @NotNull Function<? super T, ? extends K> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            return new OrderedOp<>(
                orderingSource,
                comparator.thenComparing(keySelector)
            );
        }

        @Override
        public final <K> @NotNull OrderedEnumerable<T> thenBy(
            @NotNull Function<? super T, ? extends K> keySelector,
            @NotNull Comparator<? super K> keyComparator
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            NullCheck.requireNonNull(keyComparator, "comparator");
            return new OrderedOp<>(
                orderingSource,
                comparator.thenComparing(keySelector, keyComparator)
            );
        }

        @Override
        public final <K extends Comparable<? super K>> @NotNull OrderedEnumerable<T> thenByDescending(
            @NotNull Function<? super T, ? extends K> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            return new OrderedOp<>(
                orderingSource,
                comparator.thenComparing(keySelector, Comparator.reverseOrder())
            );
        }

        @Override
        public final <K> @NotNull OrderedEnumerable<T> thenByDescending(
            @NotNull Function<? super T, ? extends K> keySelector,
            @NotNull Comparator<? super K> keyComparator
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            NullCheck.requireNonNull(keyComparator, "comparator");
            return new OrderedOp<>(
                orderingSource,
                comparator.thenComparing(keySelector, keyComparator.reversed())
            );
        }

        @Override
        public final @NotNull OrderedEnumerable<T> thenByInt(
            @NotNull ToIntFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            return new OrderedOp<>(orderingSource, comparator.thenComparingInt(keySelector));
        }

        @Override
        public final @NotNull OrderedEnumerable<T> thenByIntDescending(
            @NotNull ToIntFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            Comparator<T> secondary = (left, right) ->
                Integer.compare(keySelector.applyAsInt(right), keySelector.applyAsInt(left));
            return new OrderedOp<>(orderingSource, comparator.thenComparing(secondary));
        }

        @Override
        public final @NotNull OrderedEnumerable<T> thenByLong(
            @NotNull ToLongFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            return new OrderedOp<>(orderingSource, comparator.thenComparingLong(keySelector));
        }

        @Override
        public final @NotNull OrderedEnumerable<T> thenByLongDescending(
            @NotNull ToLongFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            Comparator<T> secondary = (left, right) ->
                Long.compare(keySelector.applyAsLong(right), keySelector.applyAsLong(left));
            return new OrderedOp<>(orderingSource, comparator.thenComparing(secondary));
        }

        @Override
        public final @NotNull OrderedEnumerable<T> thenByDouble(
            @NotNull ToDoubleFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            return new OrderedOp<>(orderingSource, comparator.thenComparingDouble(keySelector));
        }

        @Override
        public final @NotNull OrderedEnumerable<T> thenByDoubleDescending(
            @NotNull ToDoubleFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            Comparator<T> secondary = (left, right) ->
                Double.compare(keySelector.applyAsDouble(right), keySelector.applyAsDouble(left));
            return new OrderedOp<>(orderingSource, comparator.thenComparing(secondary));
        }
    }

    // ---------------------------------------------------------------------
    // Private helpers. These are implementation details, not public API.
    // ---------------------------------------------------------------------

    private static <E> @NotNull Enumerable<E> enumerableOfList(List<E> values) {
        List<E> snapshot = Collections.unmodifiableList(new ArrayList<>(values));
        return new Head<>(() -> new CollectionEnumerator<>(snapshot), false);
    }

}
