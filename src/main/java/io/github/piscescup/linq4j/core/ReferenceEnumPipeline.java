package io.github.piscescup.linq4j.core;

import io.github.piscescup.collection.EqualatorHashMap;
import io.github.piscescup.collection.EqualatorHashSet;
import io.github.piscescup.collection.EqualatorMap;
import io.github.piscescup.interfaces.Equalator;
import io.github.piscescup.interfaces.HashEqualator;
import io.github.piscescup.interfaces.Pair;
import io.github.piscescup.interfaces.exfunction.BinFunction;
import io.github.piscescup.interfaces.exfunction.BinPredicate;
import io.github.piscescup.linq4j.base.Groupable;
import io.github.piscescup.linq4j.base.UnmodifiableGrouping;
import io.github.piscescup.linq4j.enumerator.*;
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
 * {@link AbstractReferenceEnumPipeline} by fixing the exposed enumerable type to
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
public abstract class ReferenceEnumPipeline<T_IN, T_OUT>
    extends AbstractReferenceEnumPipeline<T_IN, T_OUT, Enumerable<T_OUT>>
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
        @NotNull AbstractReferenceEnumPipeline<?, T_IN, ?> upstream
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
    public final <K, A> @NotNull Enumerable<Pair<K, A>> aggregateBySeed(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull A seed,
        @NotNull BinFunction<? super A, ? super T_OUT, ? extends A> aggregator
    ) {
        return aggregateBySeedInHash(keySelector, seed, aggregator, HashEqualator.defaultHashEqualator());
    }

    /** {@inheritDoc} */
    @Override
    public final <K, A> @NotNull Enumerable<Pair<K, A>> aggregateBySeed(
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<Pair<K, A>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Map<K, A> aggregates =
                        newOrderedEqualityMap(effectiveEqualator);
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

                            aggregates.put(
                                key,
                                aggregator.apply(aggregates.getOrDefault(key, seed), element)
                            );
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
        return aggregateByInHash(keySelector, seedSelector, aggregator, HashEqualator.defaultHashEqualator());
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<Pair<K, A>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Map<K, A> aggregates =
                        newOrderedEqualityMap(effectiveEqualator);
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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

        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<Enumerable<T_OUT>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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

        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return countByInHash(keySelector, HashEqualator.defaultHashEqualator());
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<Pair<K, Integer>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Map<K, Integer> counts =
                        newOrderedEqualityMap(effectiveEqualator);
                    private Iterator<Map.Entry<K, Integer>> iterator;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        while (upstream.moveNext()) {
                            K key = keySelector.apply(upstream.current());

                            counts.merge(
                                key,
                                1,
                                Math::addExact
                            );
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {

                    private final Set<T_OUT> seen = new HashSet<>();

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (seen.add(element)) {
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
    public final @NotNull Enumerable<T_OUT> distinct(
        @Nullable Equalator<? super T_OUT> equalator
    ) {
        final Equalator<? super T_OUT> effectiveEqualator =
            equalator != null
                ? equalator
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Set<T_OUT> seen =
                        newEqualitySet(effectiveEqualator);

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (seen.add(element)) {
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
        return distinctByInHash(keySelector, HashEqualator.defaultHashEqualator());
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Set<K> seenKeys =
                        newEqualitySet(effectiveEqualator);

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (seenKeys.add(keySelector.apply(element))) {
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
        return exceptInHash(other, HashEqualator.defaultHashEqualator());
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Set<T_OUT> seen =
                        newEqualitySet(effectiveEqualator);
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends T_OUT> enumerator = other.enumerator()) {
                            while (enumerator.moveNext()) {
                                seen.add(enumerator.current());
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (seen.add(element)) {
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
        return exceptByInHash(other, keySelector, HashEqualator.defaultHashEqualator());
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Set<K> seenKeys =
                        newEqualitySet(effectiveEqualator);
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends K> enumerator = second.enumerator()) {
                            while (enumerator.moveNext()) {
                                seenKeys.add(enumerator.current());
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (seenKeys.add(keySelector.apply(element))) {
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
        return groupByInHash(keySelector, Function.identity(), HashEqualator.defaultHashEqualator());
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
        return groupByInHash(keySelector, elementSelector, HashEqualator.defaultHashEqualator());
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<Groupable<K, E>> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Map<K, List<E>> groups =
                        newOrderedEqualityMap(effectiveEqualator);
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

                            groups
                                .computeIfAbsent(key, ignored -> new ArrayList<>())
                                .add(element);
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
        @NotNull BinFunction<? super K, ? super Enumerable<T_OUT>, ? extends R> resultSelector
    ) {
        NullCheck.requireNonNull(resultSelector, "resultSelector");
        return groupToResultInHash(keySelector, resultSelector, HashEqualator.defaultHashEqualator());
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
        return groupToResultInHash(keySelector, elementSelector, resultSelector, HashEqualator.defaultHashEqualator());
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
        return groupJoinInHash(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            HashEqualator.defaultHashEqualator()
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Map<K, List<I>> lookup =
                        newEqualityMap(effectiveEqualator);
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

                                lookup
                                    .computeIfAbsent(key, ignored -> new ArrayList<>())
                                    .add(value);
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
                            lookup.getOrDefault(key, Collections.emptyList());

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
        return intersectInHash(other, HashEqualator.defaultHashEqualator());
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Set<T_OUT> remaining =
                        newEqualitySet(effectiveEqualator);
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends T_OUT> enumerator = second.enumerator()) {
                            while (enumerator.moveNext()) {
                                remaining.add(enumerator.current());
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (remaining.remove(element)) {
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
        return intersectByInHash(other, keySelector, HashEqualator.defaultHashEqualator());
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Set<K> remainingKeys =
                        newEqualitySet(effectiveEqualator);
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }
                        initialized = true;

                        try (Enumerator<? extends K> enumerator = second.enumerator()) {
                            while (enumerator.moveNext()) {
                                remainingKeys.add(enumerator.current());
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            T_OUT element = upstream.current();

                            if (remainingKeys.remove(keySelector.apply(element))) {
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
        return joinInhash(inner, outerKeySelector, innerKeySelector, resultSelector, HashEqualator.defaultHashEqualator());
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
                : HashEqualator.defaultHashEqualator();

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Map<K, List<I>> lookup =
                        newEqualityMap(effectiveEqualator);
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

                                lookup
                                    .computeIfAbsent(key, ignored -> new ArrayList<>())
                                    .add(value);
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
                                lookup.getOrDefault(key, Collections.emptyList());
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
        return leftJoinInHash(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            HashEqualator.defaultHashEqualator()
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> leftJoinOnEqualator(
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

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Map<K, List<I>> lookup =
                        newEqualityMap(equalator);
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

                                lookup
                                    .computeIfAbsent(key, ignored -> new ArrayList<>())
                                    .add(value);
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
                                lookup.getOrDefault(key, Collections.emptyList());
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
    public final <K, I> @NotNull Enumerable<Pair<T_OUT, @Nullable I>> leftJoinOnEqualator(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull Equalator<? super K> equalator
    ) {
        BinFunction<T_OUT, I, Pair<T_OUT, I>> selector = Pair::of;
        return leftJoinOnEqualator(inner, outerKeySelector, innerKeySelector, selector, equalator);
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

        return new OrderedOp<>(
            this,
            new OrderCriterion.ReferenceOrderCriterion<>(
                keySelector,
                Comparator.naturalOrder(),
                false
            )
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull OrderedEnumerable<T_OUT> orderBy(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(comparator, "comparator");

        return new OrderedOp<>(
            this,
            new OrderCriterion.ReferenceOrderCriterion<>(
                keySelector,
                comparator,
                false
            )
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K extends Comparable<? super K>> @NotNull OrderedEnumerable<T_OUT> orderByDescending(
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");

        return new OrderedOp<>(
            this,
            new OrderCriterion.ReferenceOrderCriterion<>(
                keySelector,
                Comparator.naturalOrder(),
                true
            )
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull OrderedEnumerable<T_OUT> orderByDescending(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(comparator, "comparator");

        return new OrderedOp<>(
            this,
            new OrderCriterion.ReferenceOrderCriterion<>(
                keySelector,
                comparator,
                true
            )
        );
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByInt(
        @NotNull ToIntFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, new OrderCriterion.IntOrderCriterion<>(keySelector, false));
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByIntDescending(
        @NotNull ToIntFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, new OrderCriterion.IntOrderCriterion<>(keySelector, true));
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByLong(
        @NotNull ToLongFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, new OrderCriterion.LongOrderCriterion<>(keySelector, false));
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByLongDescending(
        @NotNull ToLongFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, new OrderCriterion.LongOrderCriterion<>(keySelector, true));
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByDouble(
        @NotNull ToDoubleFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, new OrderCriterion.DoubleOrderCriterion<>(keySelector, false));
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OrderedEnumerable<T_OUT> orderByDoubleDescending(
        @NotNull ToDoubleFunction<? super T_OUT> keySelector
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        return new OrderedOp<>(this, new OrderCriterion.DoubleOrderCriterion<>(keySelector, true));
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> prepend(@Nullable T_OUT element) {
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, ? super I, ? extends R> resultSelector
    ) {
        return rightJoin(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            HashEqualator.defaultHashEqualator()
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <I, K, R> @NotNull Enumerable<R> rightJoin(
        @NotNull Enumerable<? extends I> inner,
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

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Map<K, List<T_OUT>> lookup =
                        newEqualityMap(equalator);
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

                            lookup
                                .computeIfAbsent(key, ignored -> new ArrayList<>())
                                .add(outer);
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
                                lookup.getOrDefault(key, Collections.emptyList());
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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

    @Override
    @NotNull
    public IntEnumerable selectToInt(
        @NotNull ToIntFunction<? super T_OUT> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new IntEnumPipeline.Head(
            () -> {
                Enumerator<T_OUT> upstream = enumerator();

                return new IntEnumerator() {

                    private int current;

                    @Override
                    public boolean moveNext() {
                        if (!upstream.moveNext()) {
                            return false;
                        }

                        current = selector.applyAsInt(upstream.current());
                        return true;
                    }

                    @Override
                    public int current() {
                        return current;
                    }

                    @Override
                    public void remove() {
                        upstream.remove();
                    }

                    @Override
                    public void close() {
                        upstream.close();
                    }
                };
            },
            isParallel()
        );
    }

    @Override
    @NotNull
    public LongEnumerable selectToLong(
        @NotNull ToLongFunction<? super T_OUT> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new LongEnumPipeline.Head(
            () -> {
                Enumerator<T_OUT> upstream = enumerator();

                return new LongEnumerator() {

                    private long current;

                    @Override
                    public boolean moveNext() {
                        if (!upstream.moveNext()) {
                            return false;
                        }

                        current = selector.applyAsLong(upstream.current());
                        return true;
                    }

                    @Override
                    public long current() {
                        return current;
                    }

                    @Override
                    public void remove() {
                        upstream.remove();
                    }

                    @Override
                    public void close() {
                        upstream.close();
                    }
                };
            },
            isParallel()
        );
    }

    @Override
    @NotNull
    public DoubleEnumerable selectToDouble(
        @NotNull ToDoubleFunction<? super T_OUT> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new DoubleEnumPipeline.Head(
            () -> {
                Enumerator<T_OUT> upstream = enumerator();

                return new DoubleEnumerator() {

                    private double current;

                    @Override
                    public boolean moveNext() {
                        if (!upstream.moveNext()) {
                            return false;
                        }

                        current = selector.applyAsDouble(upstream.current());
                        return true;
                    }

                    @Override
                    public double current() {
                        return current;
                    }

                    @Override
                    public void remove() {
                        upstream.remove();
                    }

                    @Override
                    public void close() {
                        upstream.close();
                    }
                };
            },
            isParallel()
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <R> @NotNull Enumerable<R> selectMany(
        @NotNull Function<? super T_OUT, ? extends Enumerable<? extends R>> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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

        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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

        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return sequenceEqual(other, HashEqualator.defaultHashEqualator());
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
        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return unionInHash(other, HashEqualator.defaultHashEqualator());
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> union(
        @NotNull Enumerable<? extends T_OUT> other,
        @NotNull Equalator<? super T_OUT> comparer
    ) {
        NullCheck.requireNonNull(other, "other");
        NullCheck.requireNonNull(comparer, "comparer");

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Set<T_OUT> seen =
                        newEqualitySet(comparer);
                    private Enumerator<? extends T_OUT> second;
                    private boolean firstDone;

                    @Override
                    protected boolean moveNextCore() {
                        if (!firstDone) {
                            while (upstream.moveNext()) {
                                T_OUT element = upstream.current();

                                if (seen.add(element)) {
                                    setCurrent(element);
                                    return true;
                                }
                            }

                            firstDone = true;
                            second = other.enumerator();
                        }

                        while (second != null && second.moveNext()) {
                            T_OUT element = second.current();

                            if (seen.add(element)) {
                                setCurrent(element);
                                return true;
                            }
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
    public final <K> @NotNull Enumerable<T_OUT> unionBy(
        @NotNull Enumerable<? extends T_OUT> second,
        @NotNull Function<? super T_OUT, ? extends K> keySelector
    ) {
        return unionByInHash(second, keySelector, HashEqualator.defaultHashEqualator());
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

        return new StatefulOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
                    private final Set<K> seenKeys =
                        newEqualitySet(comparer);
                    private Enumerator<? extends T_OUT> secondEnumerator;
                    private boolean firstDone;

                    @Override
                    protected boolean moveNextCore() {
                        if (!firstDone) {
                            while (upstream.moveNext()) {
                                T_OUT element = upstream.current();

                                if (seenKeys.add(keySelector.apply(element))) {
                                    setCurrent(element);
                                    return true;
                                }
                            }

                            firstDone = true;
                            secondEnumerator = second.enumerator();
                        }

                        while (secondEnumerator != null && secondEnumerator.moveNext()) {
                            T_OUT element = secondEnumerator.current();

                            if (seenKeys.add(keySelector.apply(element))) {
                                setCurrent(element);
                                return true;
                            }
                        }

                        return false;
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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

        return new StatelessOp<>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return new PipelineEnumerator<>(upstream) {
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
    public final <K> @NotNull Map<K, T_OUT> toMapOnEqualator(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Equalator<? super K> comparer
    ) {
        return toMapOnEqualator(keySelector, Function.identity(), comparer);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, V> @NotNull Map<K, V> toMapOnEqualator(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends V> elementSelector,
        @NotNull Equalator<? super K> comparer
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(elementSelector, "elementSelector");
        NullCheck.requireNonNull(comparer, "comparer");

        Map<K, V> result = newEqualityMap(comparer);
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
    // HashEqualator overloads
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final <K, A> @NotNull Enumerable<Pair<K, A>> aggregateBySeedInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull A seed,
        @NotNull BinFunction<? super A, ? super T_OUT, ? extends A> aggregator,
        @NotNull HashEqualator<? super K> keyEqualator
    ) {
        NullCheck.requireNonNull(keyEqualator, "keyEqualator");
        return aggregateBySeed(
            keySelector,
            seed,
            aggregator,
            (Equalator<? super K>) keyEqualator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, A> @NotNull Enumerable<Pair<K, A>> aggregateByInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super K, ? extends A> seedSelector,
        @NotNull BinFunction<? super A, ? super T_OUT, ? extends A> aggregator,
        @NotNull HashEqualator<? super K> keyEqualator
    ) {
        NullCheck.requireNonNull(keyEqualator, "keyEqualator");
        return aggregateBy(
            keySelector,
            seedSelector,
            aggregator,
            (Equalator<? super K>) keyEqualator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<Pair<K, Integer>> countByInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull HashEqualator<? super K> keyEqualator
    ) {
        NullCheck.requireNonNull(keyEqualator, "keyEqualator");
        return countBy(keySelector, (Equalator<? super K>) keyEqualator);
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> distinctInHash(
        @NotNull HashEqualator<? super T_OUT> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return distinct((Equalator<? super T_OUT>) equalator);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> distinctByInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull HashEqualator<? super K> keyEqualator
    ) {
        NullCheck.requireNonNull(keyEqualator, "keyEqualator");
        return distinctBy(keySelector, (Equalator<? super K>) keyEqualator);
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> exceptInHash(
        @NotNull Enumerable<? extends T_OUT> other,
        @NotNull HashEqualator<? super T_OUT> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return except(other, (Equalator<? super T_OUT>) equalator);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> exceptByInHash(
        @NotNull Enumerable<? extends K> other,
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return exceptBy(other, keySelector, (Equalator<? super K>) equalator);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<Groupable<K, T_OUT>> groupByInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return groupBy(keySelector, (Equalator<? super K>) equalator);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, E> @NotNull Enumerable<Groupable<K, E>> groupByInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends E> elementSelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return groupBy(
            keySelector,
            elementSelector,
            (Equalator<? super K>) equalator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, R> @NotNull Enumerable<R> groupToResultInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull BinFunction<? super K, ? super Enumerable<T_OUT>, ? extends R> resultSelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return groupToResult(
            keySelector,
            resultSelector,
            (Equalator<? super K>) equalator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, E, R> @NotNull Enumerable<R> groupToResultInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends E> elementSelector,
        @NotNull BinFunction<? super K, ? super Enumerable<E>, ? extends R> resultSelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return groupToResult(
            keySelector,
            elementSelector,
            resultSelector,
            (Equalator<? super K>) equalator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> groupJoinInHash(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, ? super Enumerable<I>, ? extends R> resultSelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return groupJoin(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            (Equalator<? super K>) equalator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> intersectInHash(
        @NotNull Enumerable<? extends T_OUT> other,
        @NotNull HashEqualator<? super T_OUT> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return intersect(other, (Equalator<? super T_OUT>) equalator);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> intersectByInHash(
        @NotNull Enumerable<? extends K> other,
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return intersectBy(other, keySelector, (Equalator<? super K>) equalator);
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> joinInhash(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, ? super I, ? extends R> resultSelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return join(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            (Equalator<? super K>) equalator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I, R> @NotNull Enumerable<R> leftJoinInHash(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T_OUT, @Nullable I, ? extends R> resultSelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return leftJoinOnEqualator(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            (Equalator<? super K>) equalator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, I> @NotNull Enumerable<Pair<T_OUT, @Nullable I>> leftJoinInHash(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return leftJoinOnEqualator(
            inner,
            outerKeySelector,
            innerKeySelector,
            (Equalator<? super K>) equalator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <I, K, R> @NotNull Enumerable<R> rightJoinInHash(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T_OUT, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super @Nullable T_OUT, ? super I, ? extends R> resultSelector,
        @NotNull HashEqualator<? super K> equalator
    ) {
        NullCheck.requireNonNull(equalator, "equalator");
        return rightJoin(
            inner,
            outerKeySelector,
            innerKeySelector,
            resultSelector,
            (Equalator<? super K>) equalator
        );
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<T_OUT> unionInHash(
        @NotNull Enumerable<? extends T_OUT> other,
        @NotNull HashEqualator<? super T_OUT> comparer
    ) {
        NullCheck.requireNonNull(comparer, "comparer");
        return union(other, (Equalator<? super T_OUT>) comparer);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Enumerable<T_OUT> unionByInHash(
        @NotNull Enumerable<? extends T_OUT> second,
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull HashEqualator<? super K> comparer
    ) {
        NullCheck.requireNonNull(comparer, "comparer");
        return unionBy(second, keySelector, (Equalator<? super K>) comparer);
    }

    /** {@inheritDoc} */
    @Override
    public final <K> @NotNull Map<K, T_OUT> toMapInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull HashEqualator<? super K> comparer
    ) {
        NullCheck.requireNonNull(comparer, "comparer");
        return toMapInHash(
            keySelector,
            Function.identity(),
            comparer
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <K, V> @NotNull Map<K, V> toMapInHash(
        @NotNull Function<? super T_OUT, ? extends K> keySelector,
        @NotNull Function<? super T_OUT, ? extends V> elementSelector,
        @NotNull HashEqualator<? super K> comparer
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(elementSelector, "elementSelector");
        NullCheck.requireNonNull(comparer, "comparer");

        Map<K, V> result = new EqualatorHashMap<>(comparer);
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

    /**
     * Creates an insertion-ordered map whose key equality semantics are
     * determined by the specified equalator.
     *
     * <p>For a {@link HashEqualator}, lookups are backed by
     * {@link EqualatorHashMap} while a separate key-order list preserves the
     * first-occurrence order required by grouping-style LINQ operators.</p>
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> newOrderedEqualityMap(
        @NotNull Equalator<? super K> equalator
    ) {
        if (equalator instanceof HashEqualator<?> hashEqualator) {
            return new OrderedHashEqualatorMap<>(
                (HashEqualator<? super K>) hashEqualator
            );
        }

        return new EqualatorMap<>(equalator);
    }

    /**
     * Internal insertion-ordered map that combines hash-based custom key
     * lookup with first-insertion ordering.
     */
    private static final class OrderedHashEqualatorMap<K, V>
        extends AbstractMap<K, V> {

        @NotNull
        private final EqualatorHashMap<K, V> lookup;

        @NotNull
        private final List<K> keyOrder = new ArrayList<>();

        private OrderedHashEqualatorMap(
            @NotNull HashEqualator<? super K> equalator
        ) {
            this.lookup = new EqualatorHashMap<>(equalator);
        }

        @Override
        public int size() {
            return lookup.size();
        }

        @Override
        public boolean containsKey(Object key) {
            return lookup.containsKey(key);
        }

        @Override
        public V get(Object key) {
            return lookup.get(key);
        }

        @Override
        public V put(K key, V value) {
            if (!lookup.containsKey(key)) {
                keyOrder.add(key);
            }
            return lookup.put(key, value);
        }

        @Override
        public void clear() {
            lookup.clear();
            keyOrder.clear();
        }

        @Override
        @NotNull
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                @NotNull
                public Iterator<Entry<K, V>> iterator() {
                    Iterator<K> iterator = keyOrder.iterator();

                    return new Iterator<>() {
                        private K currentKey;
                        private boolean canRemove;

                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Entry<K, V> next() {
                            currentKey = iterator.next();
                            canRemove = true;
                            K key = currentKey;

                            return new SimpleEntry<>(key, lookup.get(key)) {
                                @Override
                                public V setValue(V value) {
                                    V oldValue = lookup.put(key, value);
                                    super.setValue(value);
                                    return oldValue;
                                }
                            };
                        }

                        @Override
                        public void remove() {
                            if (!canRemove) {
                                throw new IllegalStateException();
                            }
                            lookup.remove(currentKey);
                            iterator.remove();
                            currentKey = null;
                            canRemove = false;
                        }
                    };
                }

                @Override
                public int size() {
                    return lookup.size();
                }

                @Override
                public void clear() {
                    OrderedHashEqualatorMap.this.clear();
                }
            };
        }
    }

    /**
     * Creates a set whose element equality semantics are determined by the
     * specified equalator.
     *
     * <p>Hash-capable equalators use {@link EqualatorHashSet}; other
     * equalators use a set view backed by {@link EqualatorMap}.</p>
     *
     * @param equalator the equalator used to compare elements
     * @param <E> the element type
     * @return a set using the specified equality semantics
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private static <E> Set<E> newEqualitySet(
        @NotNull Equalator<? super E> equalator
    ) {
        if (equalator instanceof HashEqualator<?> hashEqualator) {
            return new EqualatorHashSet<>(
                (HashEqualator<? super E>) hashEqualator
            );
        }

        return Collections.newSetFromMap(
            new EqualatorMap<>(equalator)
        );
    }

    /**
     * Creates a map whose key equality semantics are determined by the
     * specified equalator. Hash-capable equalators automatically use
     * {@link EqualatorHashMap}; other equalators use {@link EqualatorMap}.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> newEqualityMap(
        @NotNull Equalator<? super K> equalator
    ) {
        if (equalator instanceof HashEqualator<?> hashEqualator) {
            return new EqualatorHashMap<>(
                (HashEqualator<? super K>) hashEqualator
            );
        }

        return new EqualatorMap<>(equalator);
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
            @NotNull AbstractReferenceEnumPipeline<?, T_IN, ?> upstream
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
            @NotNull AbstractReferenceEnumPipeline<?, T_IN, ?> upstream
        ) {
            super(upstream);
        }

        @Override
        protected final boolean opIsStateful() {
            return true;
        }
    }


    /**
     * Stateful ordered stage.
     *
     * <p>Single-level ordering keeps the JDK's optimized list sort path. Once a
     * secondary ordering criterion is added, the stage switches to a cached-key
     * stable index sort. This avoids repeatedly invoking key selectors during
     * comparison and also preserves primitive keys in primitive arrays.</p>
     *
     * <p>The complete ordering description is immutable. Every
     * {@code ThenBy}-style operation creates a new ordered stage that refers to
     * the original ordering source instead of wrapping the previous ordered
     * stage. Consequently, the upstream sequence is buffered and sorted only
     * once when the resulting ordered enumerable is traversed.</p>
     *
     * @param <T> the element type
     */
    private static final class OrderedOp<T>
        extends StatefulOp<T, T>
        implements OrderedEnumerable<T> {

        @NotNull
        private final AbstractReferenceEnumPipeline<?, T, ?> orderingSource;

        /**
         * Direct element comparator used by {@link #order()} and
         * {@link #order(Comparator)}. It is retained as a fast path for
         * single-level ordering and as the primary comparison when secondary
         * cached-key criteria are appended.
         */
        @Nullable
        private final Comparator<? super T> directComparator;

        /**
         * Key-based ordering criteria created by {@code OrderBy} and
         * {@code ThenBy}-style operations.
         */
        @NotNull
        private final List<OrderCriterion<T>> criteria;

        private OrderedOp(
            @NotNull AbstractReferenceEnumPipeline<?, T, ?> orderingSource,
            @NotNull Comparator<? super T> comparator
        ) {
            super(orderingSource);
            this.orderingSource = orderingSource;
            this.directComparator = comparator;
            this.criteria = Collections.emptyList();
        }

        private OrderedOp(
            @NotNull AbstractReferenceEnumPipeline<?, T, ?> orderingSource,
            @NotNull OrderCriterion<T> criterion
        ) {
            super(orderingSource);
            this.orderingSource = orderingSource;
            this.directComparator = null;
            this.criteria = List.of(criterion);
        }

        private OrderedOp(
            @NotNull AbstractReferenceEnumPipeline<?, T, ?> orderingSource,
            @Nullable Comparator<? super T> directComparator,
            @NotNull List<OrderCriterion<T>> criteria
        ) {
            super(orderingSource);
            this.orderingSource = orderingSource;
            this.directComparator = directComparator;
            this.criteria = criteria;
        }

        @Override
        protected @NotNull Enumerator<T> opWrapEnumerator(
            @NotNull Enumerator<T> upstream
        ) {
            return new PipelineEnumerator<>(upstream) {
                private final List<T> buffer = new ArrayList<>();
                private int[] sortedIndexes;
                private boolean initialized;
                private int index;

                /**
                 * Buffers the upstream sequence and performs the appropriate
                 * sorting strategy exactly once for this enumeration.
                 */
                private void initialize() {
                    if (initialized) {
                        return;
                    }

                    initialized = true;

                    while (upstream.moveNext()) {
                        buffer.add(upstream.current());
                    }

                    if (buffer.size() < 2) {
                        return;
                    }

                    /*
                     * Keep direct comparator ordering on ArrayList.sort().
                     * Benchmark results show that the single-level path is
                     * already competitive, so allocating key caches and index
                     * buffers here would only add unnecessary overhead.
                     */
                    if (directComparator != null && criteria.isEmpty()) {
                        buffer.sort(directComparator);
                        return;
                    }

                    /*
                     * Keep a single OrderBy criterion on ArrayList.sort() for
                     * the same reason. Cached keys become beneficial primarily
                     * when secondary ordering levels are present.
                     */
                    if (directComparator == null && criteria.size() == 1) {
                        buffer.sort(criteria.getFirst().elementComparator());
                        return;
                    }

                    sortByCachedKeys();
                }

                /**
                 * Precomputes all extractable keys and performs a stable sort
                 * over source indexes.
                 */
                private void sortByCachedKeys() {
                    int size = buffer.size();
                    int criterionCount =
                        criteria.size() + (directComparator == null ? 0 : 1);

                    OrderKeyCache[] caches =
                        new OrderKeyCache[criterionCount];

                    int cacheIndex = 0;

                    if (directComparator != null) {
                        caches[cacheIndex++] =
                            new OrderKeyCache.ElementOrderKeyCache<>(
                                buffer,
                                directComparator
                            );
                    }

                    for (OrderCriterion<T> criterion : criteria) {
                        caches[cacheIndex++] = criterion.prepare(buffer);
                    }

                    int[] indexes = new int[size];
                    for (int i = 0; i < size; i++) {
                        indexes[i] = i;
                    }

                    int[] workspace = new int[size];

                    mergeSort(
                        indexes,
                        workspace,
                        0,
                        size,
                        caches
                    );

                    sortedIndexes = indexes;
                }

                @Override
                protected boolean moveNextCore() {
                    initialize();

                    if (index >= buffer.size()) {
                        return false;
                    }

                    if (sortedIndexes == null) {
                        setCurrent(buffer.get(index++));
                    } else {
                        setCurrent(buffer.get(sortedIndexes[index++]));
                    }

                    return true;
                }
            };
        }

        /**
         * Creates a new ordered stage with one additional key-based criterion.
         *
         * @param criterion the criterion to append
         * @return the new ordered stage
         */
        @NotNull
        private OrderedOp<T> appendCriterion(
            @NotNull OrderCriterion<T> criterion
        ) {
            List<OrderCriterion<T>> newCriteria =
                new ArrayList<>(criteria.size() + 1);

            newCriteria.addAll(criteria);
            newCriteria.add(criterion);

            return new OrderedOp<>(
                orderingSource,
                directComparator,
                List.copyOf(newCriteria)
            );
        }

        /** {@inheritDoc} */
        @Override
        public final <K extends Comparable<? super K>> @NotNull OrderedEnumerable<T> thenBy(
            @NotNull Function<? super T, ? extends K> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");

            return appendCriterion(
                new OrderCriterion.ReferenceOrderCriterion<>(
                    keySelector,
                    Comparator.naturalOrder(),
                    false
                )
            );
        }

        /** {@inheritDoc} */
        @Override
        public final <K> @NotNull OrderedEnumerable<T> thenBy(
            @NotNull Function<? super T, ? extends K> keySelector,
            @NotNull Comparator<? super K> keyComparator
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            NullCheck.requireNonNull(keyComparator, "comparator");

            return appendCriterion(
                new OrderCriterion.ReferenceOrderCriterion<>(
                    keySelector,
                    keyComparator,
                    false
                )
            );
        }

        /** {@inheritDoc} */
        @Override
        public final <K extends Comparable<? super K>> @NotNull OrderedEnumerable<T> thenByDescending(
            @NotNull Function<? super T, ? extends K> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");

            return appendCriterion(
                new OrderCriterion.ReferenceOrderCriterion<>(
                    keySelector,
                    Comparator.naturalOrder(),
                    true
                )
            );
        }

        /** {@inheritDoc} */
        @Override
        public final <K> @NotNull OrderedEnumerable<T> thenByDescending(
            @NotNull Function<? super T, ? extends K> keySelector,
            @NotNull Comparator<? super K> keyComparator
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");
            NullCheck.requireNonNull(keyComparator, "comparator");

            return appendCriterion(
                new OrderCriterion.ReferenceOrderCriterion<>(
                    keySelector,
                    keyComparator,
                    true
                )
            );
        }

        /** {@inheritDoc} */
        @Override
        public final @NotNull OrderedEnumerable<T> thenByInt(
            @NotNull ToIntFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");

            return appendCriterion(
                new OrderCriterion.IntOrderCriterion<>(keySelector, false)
            );
        }

        /** {@inheritDoc} */
        @Override
        public final @NotNull OrderedEnumerable<T> thenByIntDescending(
            @NotNull ToIntFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");

            return appendCriterion(
                new OrderCriterion.IntOrderCriterion<>(keySelector, true)
            );
        }

        /** {@inheritDoc} */
        @Override
        public final @NotNull OrderedEnumerable<T> thenByLong(
            @NotNull ToLongFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");

            return appendCriterion(
                new OrderCriterion.LongOrderCriterion<>(keySelector, false)
            );
        }

        /** {@inheritDoc} */
        @Override
        public final @NotNull OrderedEnumerable<T> thenByLongDescending(
            @NotNull ToLongFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");

            return appendCriterion(
                new OrderCriterion.LongOrderCriterion<>(keySelector, true)
            );
        }

        /** {@inheritDoc} */
        @Override
        public final @NotNull OrderedEnumerable<T> thenByDouble(
            @NotNull ToDoubleFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");

            return appendCriterion(
                new OrderCriterion.DoubleOrderCriterion<>(keySelector, false)
            );
        }

        /** {@inheritDoc} */
        @Override
        public final @NotNull OrderedEnumerable<T> thenByDoubleDescending(
            @NotNull ToDoubleFunction<? super T> keySelector
        ) {
            NullCheck.requireNonNull(keySelector, "keySelector");

            return appendCriterion(
                new OrderCriterion.DoubleOrderCriterion<>(keySelector, true)
            );
        }
    }

    /**
     * Performs a stable merge sort over an array of source indexes.
     *
     * <p>The sort compares indexes through precomputed key caches. Equal
     * elements are always taken from the left half first, preserving the
     * original source order when all ordering criteria compare equal.</p>
     *
     * @param indexes the indexes being sorted
     * @param workspace reusable merge workspace
     * @param from inclusive start index
     * @param to exclusive end index
     * @param caches the ordering levels in priority order
     */
    private static void mergeSort(
        int[] indexes,
        int[] workspace,
        int from,
        int to,
        OrderKeyCache[] caches
    ) {
        int length = to - from;

        if (length < 2) {
            return;
        }

        int middle = (from + to) >>> 1;

        mergeSort(indexes, workspace, from, middle, caches);
        mergeSort(indexes, workspace, middle, to, caches);

        /*
         * Skip the merge when both sorted halves are already in the correct
         * relative order. This is especially helpful for partially ordered
         * input.
         */
        if (compareIndexes(
            indexes[middle - 1],
            indexes[middle],
            caches
        ) <= 0) {
            return;
        }

        System.arraycopy(
            indexes,
            from,
            workspace,
            from,
            length
        );

        int left = from;
        int right = middle;
        int target = from;

        while (left < middle && right < to) {
            if (compareIndexes(
                workspace[left],
                workspace[right],
                caches
            ) <= 0) {
                indexes[target++] = workspace[left++];
            } else {
                indexes[target++] = workspace[right++];
            }
        }

        while (left < middle) {
            indexes[target++] = workspace[left++];
        }

        while (right < to) {
            indexes[target++] = workspace[right++];
        }
    }

    /**
     * Compares two buffered source indexes using all ordering levels.
     *
     * @param leftIndex the index of the left source element
     * @param rightIndex the index of the right source element
     * @param caches the ordering levels in priority order
     * @return the first non-zero comparison result, or zero if all criteria
     *         consider both elements equal
     */
    private static int compareIndexes(
        int leftIndex,
        int rightIndex,
        OrderKeyCache[] caches
    ) {
        for (OrderKeyCache cache : caches) {
            int result = cache.compare(leftIndex, rightIndex);

            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    // ---------------------------------------------------------------------
    // Private helpers. These are implementation details, not public API.
    // ---------------------------------------------------------------------

    private static <E> @NotNull Enumerable<E> enumerableOfList(List<E> values) {
        List<E> snapshot = List.copyOf(values);
        return new Head<>(() -> new CollectionEnumerator<>(snapshot), false);
    }

}
