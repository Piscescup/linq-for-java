package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.Linq;
import io.github.piscescup.linq4j.enumerator.*;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.*;

/**
 * Abstract base class for primitive {@code int} enumerable pipeline stages.
 *
 * <p>An {@code IntEnumPipeline} represents either the source stage of an
 * {@link IntEnumerable} query or an intermediate operation that consumes and
 * produces primitive {@code int} values. Values remain primitive throughout
 * the pipeline and therefore do not require boxing into {@link Integer}
 * objects.</p>
 *
 * <p>Pipeline construction uses deferred execution. Invoking an intermediate
 * operation creates a new pipeline stage but does not enumerate the source.
 * Each call to {@link #enumerator()} creates an independent enumeration
 * chain.</p>
 *
 * <p>Pipeline stages contain only query-description state. Mutable state that
 * belongs to a traversal, such as indexes, primitive buffers, distinct-value
 * sets, and secondary enumerators, is created independently for each
 * enumeration.</p>
 *
 * <p>Pipeline-wide configuration such as sequential or parallel execution
 * mode and close handlers is managed by {@link AbstractBaseEnumPipeline}.
 * All stages belonging to the same pipeline share the same configuration
 * context.</p>
 *
 * <p>Operations that explicitly transition to a reference-type pipeline, such
 * as {@link #boxed()} and {@link #selectToObj(IntFunction)}, create a
 * reference {@link Enumerable}. Boxing therefore occurs only at an explicit
 * primitive-to-reference boundary.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
@ApiStatus.Internal
abstract class IntEnumPipeline
    extends AbstractBaseEnumPipeline<Integer, IntEnumerable>
    implements IntEnumerable {

    /**
     * The immediately preceding pipeline stage.
     *
     * <p>This field is {@code null} only for the source stage.</p>
     */
    private final @Nullable IntEnumPipeline upstream;

    /**
     * Factory used by the source stage to create fresh source enumerators.
     *
     * <p>This field is non-null only for the source stage.</p>
     */
    private final @Nullable Supplier<? extends IntEnumerator> sourceSupplier;

    /**
     * Creates a source stage.
     *
     * <p>The supplied source factory is retained and invoked whenever a new
     * enumeration is requested. It should therefore create a new independent
     * {@link IntEnumerator} for every invocation.</p>
     *
     * @param sourceSupplier the factory used to create source enumerators
     * @param parallel whether the source is configured for parallel evaluation
     */
    protected IntEnumPipeline(
        @NotNull Supplier<? extends IntEnumerator> sourceSupplier,
        boolean parallel
    ) {
        super(parallel);

        this.upstream = null;
        this.sourceSupplier = NullCheck.requireNonNull(
            sourceSupplier,
            "sourceSupplier"
        );
    }

    /**
     * Creates an intermediate stage appended to the specified upstream
     * pipeline.
     *
     * <p>The newly created stage shares the common execution configuration
     * of the upstream pipeline through {@link AbstractBaseEnumPipeline}.</p>
     *
     * @param upstream the immediately preceding pipeline stage
     */
    protected IntEnumPipeline(
        @NotNull IntEnumPipeline upstream
    ) {
        super(upstream);

        this.upstream = NullCheck.requireNonNull(
            upstream,
            "upstream"
        );

        this.sourceSupplier = null;
    }

    /**
     * Returns whether this pipeline operation is stateful.
     *
     * <p>Stateless operations can normally produce each result without
     * retaining information about previously processed elements. Stateful
     * operations may require primitive buffers, sets, or other state during
     * an enumeration.</p>
     *
     * @return {@code true} if the operation requires traversal-specific state;
     *         otherwise {@code false}
     */
    protected abstract boolean opIsStateful();

    /**
     * Wraps an upstream enumerator with the behavior represented by this
     * pipeline stage.
     *
     * <p>The supplied enumerator represents the output of the immediately
     * preceding stage. Mutable state required by this operation should be
     * stored in the returned enumerator rather than in the pipeline stage
     * itself.</p>
     *
     * @param upstream the upstream primitive enumerator
     * @return an enumerator applying this stage to the upstream sequence
     */
    protected abstract @NotNull IntEnumerator opWrapEnumerator(
        @NotNull IntEnumerator upstream
    );

    // ---------------------------------------------------------------------
    // Enumeration
    // ---------------------------------------------------------------------

    /**
     * Creates a new primitive enumerator for this pipeline.
     *
     * <p>For a source stage, the configured source supplier is invoked.
     * For an intermediate stage, an upstream enumerator is created and then
     * wrapped by this stage.</p>
     *
     * @return a new primitive enumerator for this pipeline
     */
    @Override
    public final @NotNull IntEnumerator enumerator() {
        if (sourceSupplier != null) {
            return NullCheck.requireNonNull(
                sourceSupplier.get(),
                "sourceSupplier returned null"
            );
        }

        IntEnumPipeline upstream = this.upstream;

        if (upstream == null) {
            throw new IllegalStateException(
                "Non-source pipeline stage does not have an upstream stage."
            );
        }

        return opWrapEnumerator(
            upstream.enumerator()
        );
    }

    // ---------------------------------------------------------------------
    // Aggregation
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final int aggregateToResult(
        int seed,
        @NotNull IntBinaryOperator aggregator
    ) {
        NullCheck.requireNonNull(
            aggregator,
            "aggregator"
        );

        int accumulator = seed;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                accumulator = aggregator.applyAsInt(
                    accumulator,
                    enumerator.current()
                );
            }
        }

        return accumulator;
    }

    // ---------------------------------------------------------------------
    // Quantifiers
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final boolean all(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (!predicate.test(enumerator.current())) {
                    return false;
                }
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean any() {
        try (IntEnumerator enumerator = enumerator()) {
            return enumerator.moveNext();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean any(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (predicate.test(enumerator.current())) {
                    return true;
                }
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean contains(int value) {
        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (enumerator.current() == value) {
                    return true;
                }
            }
        }

        return false;
    }

    // ---------------------------------------------------------------------
    // Append / prepend / concat
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable append(int element) {
        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private boolean appended;

                    @Override
                    protected boolean moveNextCore() {
                        if (upstream.moveNext()) {
                            setCurrent(
                                upstream.current()
                            );
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
    public final @NotNull IntEnumerable prepend(int element) {
        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private boolean prepended;

                    @Override
                    protected boolean moveNextCore() {
                        if (!prepended) {
                            prepended = true;
                            setCurrent(element);
                            return true;
                        }

                        if (!upstream.moveNext()) {
                            return false;
                        }

                        setCurrent(
                            upstream.current()
                        );

                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable concat(
        @NotNull IntEnumerable after
    ) {
        NullCheck.requireNonNull(
            after,
            "after"
        );

        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private @Nullable IntEnumerator second;

                    private boolean firstCompleted;

                    @Override
                    protected boolean moveNextCore() {
                        if (!firstCompleted) {
                            if (upstream.moveNext()) {
                                setCurrent(
                                    upstream.current()
                                );

                                return true;
                            }

                            firstCompleted = true;
                            second = after.enumerator();
                        }

                        IntEnumerator second = this.second;

                        if (
                            second != null
                                && second.moveNext()
                        ) {
                            setCurrent(
                                second.current()
                            );

                            return true;
                        }

                        return false;
                    }

                    @Override
                    public void close() {
                        try {
                            IntEnumerator second =
                                this.second;

                            if (second != null) {
                                second.close();
                                this.second = null;
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
    // Default
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable defaultIfEmpty(
        int defaultValue
    ) {
        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private boolean checked;
                    private boolean yieldedDefault;

                    @Override
                    protected boolean moveNextCore() {
                        if (!checked) {
                            checked = true;

                            if (upstream.moveNext()) {
                                setCurrent(
                                    upstream.current()
                                );

                                return true;
                            }

                            yieldedDefault = true;
                            setCurrent(defaultValue);

                            return true;
                        }

                        if (yieldedDefault) {
                            return false;
                        }

                        if (!upstream.moveNext()) {
                            return false;
                        }

                        setCurrent(
                            upstream.current()
                        );

                        return true;
                    }
                };
            }
        };
    }

    // ---------------------------------------------------------------------
    // Element access
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final int elementAt(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(
                "Index cannot be negative: " + index
            );
        }

        try (IntEnumerator enumerator = enumerator()) {
            int currentIndex = 0;

            while (enumerator.moveNext()) {
                if (currentIndex == index) {
                    return enumerator.current();
                }

                currentIndex++;
            }

            throw new IndexOutOfBoundsException(
                "Index "
                    + index
                    + " is out of bounds for a sequence of length "
                    + currentIndex
            );
        }
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalInt elementAtOrEmpty(
        int index
    ) {
        if (index < 0) {
            return OptionalInt.empty();
        }

        try (IntEnumerator enumerator = enumerator()) {
            int currentIndex = 0;

            while (enumerator.moveNext()) {
                if (currentIndex == index) {
                    return OptionalInt.of(
                        enumerator.current()
                    );
                }

                currentIndex++;
            }
        }

        return OptionalInt.empty();
    }

    /** {@inheritDoc} */
    @Override
    public final int elementAtOrDefault(
        int index,
        int defaultValue
    ) {
        if (index < 0) {
            return defaultValue;
        }

        try (IntEnumerator enumerator = enumerator()) {
            int currentIndex = 0;

            while (enumerator.moveNext()) {
                if (currentIndex == index) {
                    return enumerator.current();
                }

                currentIndex++;
            }
        }

        return defaultValue;
    }

    // ---------------------------------------------------------------------
    // First
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final int first() {
        try (IntEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            return enumerator.current();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int first(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                int value =
                    enumerator.current();

                if (predicate.test(value)) {
                    return value;
                }
            }
        }

        throw new NoSuchElementException(
            "No element satisfies the condition."
        );
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalInt firstOrEmpty() {
        try (IntEnumerator enumerator = enumerator()) {
            if (enumerator.moveNext()) {
                return OptionalInt.of(
                    enumerator.current()
                );
            }
        }

        return OptionalInt.empty();
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalInt firstOrEmpty(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                int value =
                    enumerator.current();

                if (predicate.test(value)) {
                    return OptionalInt.of(value);
                }
            }
        }

        return OptionalInt.empty();
    }

    // ---------------------------------------------------------------------
// Last
// ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final int last() {
        try (IntEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            int result = enumerator.current();

            while (enumerator.moveNext()) {
                result = enumerator.current();
            }

            return result;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int last(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        int result = 0;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                int value = enumerator.current();

                if (predicate.test(value)) {
                    result = value;
                    found = true;
                }
            }
        }

        if (!found) {
            throw new NoSuchElementException(
                "No element satisfies the condition."
            );
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalInt lastOrEmpty() {
        boolean found = false;
        int result = 0;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                result = enumerator.current();
                found = true;
            }
        }

        return found
            ? OptionalInt.of(result)
            : OptionalInt.empty();
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalInt lastOrEmpty(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        int result = 0;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                int value = enumerator.current();

                if (predicate.test(value)) {
                    result = value;
                    found = true;
                }
            }
        }

        return found
            ? OptionalInt.of(result)
            : OptionalInt.empty();
    }


// ---------------------------------------------------------------------
// Single
// ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final int single() {
        try (IntEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            int result = enumerator.current();

            if (enumerator.moveNext()) {
                throw new IllegalStateException(
                    "Sequence contains more than one element."
                );
            }

            return result;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int single(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        int result = 0;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                int value = enumerator.current();

                if (!predicate.test(value)) {
                    continue;
                }

                if (found) {
                    throw new IllegalStateException(
                        "Sequence contains more than one matching element."
                    );
                }

                result = value;
                found = true;
            }
        }

        if (!found) {
            throw new NoSuchElementException(
                "No element satisfies the condition."
            );
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalInt singleOrEmpty() {
        try (IntEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                return OptionalInt.empty();
            }

            int result = enumerator.current();

            if (enumerator.moveNext()) {
                throw new IllegalStateException(
                    "Sequence contains more than one element."
                );
            }

            return OptionalInt.of(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalInt singleOrEmpty(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        int result = 0;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                int value = enumerator.current();

                if (!predicate.test(value)) {
                    continue;
                }

                if (found) {
                    throw new IllegalStateException(
                        "Sequence contains more than one matching element."
                    );
                }

                result = value;
                found = true;
            }
        }

        return found
            ? OptionalInt.of(result)
            : OptionalInt.empty();
    }


    // ---------------------------------------------------------------------
    // Numeric aggregation
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final long count() {
        long count = 0L;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (count == Long.MAX_VALUE) {
                    throw new ArithmeticException(
                        "The number of elements exceeds Long.MAX_VALUE."
                    );
                }

                count++;
            }
        }

        return count;
    }

    /** {@inheritDoc} */
    @Override
    public final long count(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        long count = 0L;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (predicate.test(enumerator.current())) {
                    if (count == Long.MAX_VALUE) {
                        throw new ArithmeticException(
                            "The number of matching elements exceeds Long.MAX_VALUE."
                        );
                    }

                    count++;
                }
            }
        }

        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int sum() {
        int sum = 0;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                sum = Math.addExact(
                    sum,
                    enumerator.current()
                );
            }
        }

        return sum;
    }

    /** {@inheritDoc} */
    @Override
    public final int min() {
        try (IntEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            int minimum =
                enumerator.current();

            while (enumerator.moveNext()) {
                int value =
                    enumerator.current();

                if (value < minimum) {
                    minimum = value;
                }
            }

            return minimum;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int max() {
        try (IntEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            int maximum =
                enumerator.current();

            while (enumerator.moveNext()) {
                int value =
                    enumerator.current();

                if (value > maximum) {
                    maximum = value;
                }
            }

            return maximum;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final double average() {
        long sum = 0L;
        long count = 0L;

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                sum = Math.addExact(
                    sum,
                    enumerator.current()
                );

                if (count == Long.MAX_VALUE) {
                    throw new ArithmeticException(
                        "The number of elements exceeds Long.MAX_VALUE."
                    );
                }

                count++;
            }
        }

        if (count == 0L) {
            throw new ArithmeticException(
                "Cannot compute average of an empty sequence."
            );
        }

        return (double) sum / count;
    }

    // ---------------------------------------------------------------------
    // Filtering
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable where(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            int value =
                                upstream.current();

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

    // ---------------------------------------------------------------------
    // Projection
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable select(
        @NotNull IntUnaryOperator selector
    ) {
        NullCheck.requireNonNull(
            selector,
            "selector"
        );

        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    @Override
                    protected boolean moveNextCore() {
                        if (!upstream.moveNext()) {
                            return false;
                        }

                        setCurrent(
                            selector.applyAsInt(
                                upstream.current()
                            )
                        );

                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final @NotNull LongEnumerable selectToLong(
        @NotNull IntToLongFunction selector
    ) {
        NullCheck.requireNonNull(
            selector,
            "selector"
        );

        return new LongEnumPipeline.Head(
            () -> {
                IntEnumerator upstream =
                    enumerator();

                return new LongEnumerator() {

                    private long current;
                    private boolean hasCurrent;
                    private boolean finished;
                    private boolean closed;

                    @Override
                    public boolean moveNext() {
                        ensureOpen();

                        if (finished) {
                            hasCurrent = false;
                            return false;
                        }

                        hasCurrent = false;

                        if (!upstream.moveNext()) {
                            finished = true;
                            return false;
                        }

                        current = selector.applyAsLong(
                            upstream.current()
                        );

                        hasCurrent = true;
                        return true;
                    }

                    @Override
                    public long current() {
                        ensureOpen();

                        if (!hasCurrent) {
                            throw new IllegalStateException(
                                "The enumerator is not positioned on an element."
                            );
                        }

                        return current;
                    }

                    @Override
                    public void remove() {
                        ensureOpen();
                        upstream.remove();
                    }

                    @Override
                    public void reset() {
                        ensureOpen();

                        upstream.reset();

                        current = 0L;
                        hasCurrent = false;
                        finished = false;
                    }

                    @Override
                    public void close() {
                        if (closed) {
                            return;
                        }

                        closed = true;

                        current = 0L;
                        hasCurrent = false;
                        finished = true;

                        upstream.close();
                    }

                    private void ensureOpen() {
                        if (closed) {
                            throw new IllegalStateException(
                                "The enumerator has already been closed."
                            );
                        }
                    }
                };
            },
            isParallel()
        );
    }

    @Override
    public final @NotNull DoubleEnumerable selectToDouble(
        @NotNull IntToDoubleFunction selector
    ) {
        NullCheck.requireNonNull(
            selector,
            "selector"
        );

        return new DoubleEnumPipeline.Head(
            () -> {
                IntEnumerator upstream =
                    enumerator();

                return new DoubleEnumerator() {

                    private double current;
                    private boolean hasCurrent;
                    private boolean finished;
                    private boolean closed;

                    @Override
                    public boolean moveNext() {
                        ensureOpen();

                        if (finished) {
                            hasCurrent = false;
                            return false;
                        }

                        hasCurrent = false;

                        if (!upstream.moveNext()) {
                            finished = true;
                            return false;
                        }

                        current = selector.applyAsDouble(
                            upstream.current()
                        );

                        hasCurrent = true;
                        return true;
                    }

                    @Override
                    public double current() {
                        ensureOpen();

                        if (!hasCurrent) {
                            throw new IllegalStateException(
                                "The enumerator is not positioned on an element."
                            );
                        }

                        return current;
                    }

                    @Override
                    public void remove() {
                        ensureOpen();
                        upstream.remove();
                    }

                    @Override
                    public void reset() {
                        ensureOpen();

                        upstream.reset();

                        current = 0.0;
                        hasCurrent = false;
                        finished = false;
                    }

                    @Override
                    public void close() {
                        if (closed) {
                            return;
                        }

                        closed = true;

                        current = 0.0;
                        hasCurrent = false;
                        finished = true;

                        upstream.close();
                    }

                    private void ensureOpen() {
                        if (closed) {
                            throw new IllegalStateException(
                                "The enumerator has already been closed."
                            );
                        }
                    }
                };
            },
            isParallel()
        );
    }

    /** {@inheritDoc} */
    @Override
    public final <R> @NotNull Enumerable<R> selectToObj(
        @NotNull IntFunction<? extends R> selector
    ) {
        NullCheck.requireNonNull(
            selector,
            "selector"
        );

        return Linq.fromEnumerator(
            () -> new ReferenceBridgeEnumerator<>(
                enumerator(),
                selector
            )
        );
    }

    // ---------------------------------------------------------------------
    // Slicing
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable skip(int count) {
        final int skipCount =
            Math.max(0, count);

        if (skipCount == 0) {
            return this;
        }

        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private int remaining =
                        skipCount;

                    @Override
                    protected boolean moveNextCore() {
                        while (remaining > 0) {
                            if (!upstream.moveNext()) {
                                return false;
                            }

                            remaining--;
                        }

                        if (!upstream.moveNext()) {
                            return false;
                        }

                        setCurrent(
                            upstream.current()
                        );

                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable take(int count) {
        final int takeCount =
            Math.max(0, count);

        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private int remaining =
                        takeCount;

                    @Override
                    protected boolean moveNextCore() {
                        if (
                            remaining <= 0
                                || !upstream.moveNext()
                        ) {
                            return false;
                        }

                        remaining--;

                        setCurrent(
                            upstream.current()
                        );

                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable skipWhile(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private boolean skipping =
                        true;

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            int value =
                                upstream.current();

                            if (
                                skipping
                                    && predicate.test(value)
                            ) {
                                continue;
                            }

                            skipping = false;

                            setCurrent(value);
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
    public final @NotNull IntEnumerable takeWhile(
        @NotNull IntPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        return new StatelessOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private boolean taking =
                        true;

                    @Override
                    protected boolean moveNextCore() {
                        if (
                            !taking
                                || !upstream.moveNext()
                        ) {
                            return false;
                        }

                        int value =
                            upstream.current();

                        if (!predicate.test(value)) {
                            taking = false;
                            return false;
                        }

                        setCurrent(value);
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable skipLast(int count) {
        final int skipCount =
            Math.max(0, count);

        if (skipCount == 0) {
            return this;
        }

        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private final int[] queue =
                        new int[skipCount];

                    private int head;
                    private int size;

                    @Override
                    protected boolean moveNextCore() {
                        while (size < skipCount) {
                            if (!upstream.moveNext()) {
                                return false;
                            }

                            queue[
                                (head + size)
                                    % skipCount
                                ] = upstream.current();

                            size++;
                        }

                        if (!upstream.moveNext()) {
                            return false;
                        }

                        int result =
                            queue[head];

                        queue[head] =
                            upstream.current();

                        head =
                            (head + 1)
                                % skipCount;

                        setCurrent(result);
                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable takeLast(int count) {
        final int takeCount =
            Math.max(0, count);

        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private int[] values;

                    private int size;
                    private int index;

                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        if (takeCount == 0) {
                            values = new int[0];
                            return;
                        }

                        int[] ring =
                            new int[takeCount];

                        int total = 0;

                        while (upstream.moveNext()) {
                            ring[
                                total % takeCount
                                ] = upstream.current();

                            total++;
                        }

                        size =
                            Math.min(
                                total,
                                takeCount
                            );

                        values =
                            new int[size];

                        int start =
                            total <= takeCount
                                ? 0
                                : total % takeCount;

                        for (int i = 0; i < size; i++) {
                            values[i] =
                                ring[
                                    (start + i)
                                        % takeCount
                                    ];
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (index >= size) {
                            return false;
                        }

                        setCurrent(
                            values[index++]
                        );

                        return true;
                    }
                };
            }
        };
    }

    // ---------------------------------------------------------------------
    // Reverse / shuffle
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable reverse() {
        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private int[] values;
                    private int index;

                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        values =
                            collectToArray(upstream);

                        index =
                            values.length - 1;
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (index < 0) {
                            return false;
                        }

                        setCurrent(
                            values[index--]
                        );

                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable shuffle() {
        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private int[] values;

                    private int index;

                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        values =
                            collectToArray(upstream);

                        ThreadLocalRandom random =
                            ThreadLocalRandom.current();

                        for (
                            int i = values.length - 1;
                            i > 0;
                            i--
                        ) {
                            int j =
                                random.nextInt(i + 1);

                            int temporary =
                                values[i];

                            values[i] =
                                values[j];

                            values[j] =
                                temporary;
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (index >= values.length) {
                            return false;
                        }

                        setCurrent(
                            values[index++]
                        );

                        return true;
                    }
                };
            }
        };
    }

    // ---------------------------------------------------------------------
    // Sequence equality
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final boolean sequenceEqual(
        @NotNull IntEnumerable other
    ) {
        NullCheck.requireNonNull(
            other,
            "other"
        );

        try (
            IntEnumerator first = enumerator();
            IntEnumerator second = other.enumerator()
        ) {
            while (true) {
                boolean firstHas =
                    first.moveNext();

                boolean secondHas =
                    second.moveNext();

                if (firstHas != secondHas) {
                    return false;
                }

                if (!firstHas) {
                    return true;
                }

                if (
                    first.current()
                        != second.current()
                ) {
                    return false;
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Set operations
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable distinct() {
        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private final IntHashSet seen =
                        new IntHashSet();

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            int value =
                                upstream.current();

                            if (seen.add(value)) {
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
    public final @NotNull IntEnumerable except(
        @NotNull IntEnumerable other
    ) {
        NullCheck.requireNonNull(
            other,
            "other"
        );

        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private final IntHashSet seen =
                        new IntHashSet();

                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        try (
                            IntEnumerator enumerator =
                                other.enumerator()
                        ) {
                            while (enumerator.moveNext()) {
                                seen.add(
                                    enumerator.current()
                                );
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            int value =
                                upstream.current();

                            /*
                             * Values from the second sequence are inserted
                             * before this sequence is traversed. If add()
                             * succeeds, the value is neither excluded nor
                             * previously emitted.
                             */
                            if (seen.add(value)) {
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
    public final @NotNull IntEnumerable intersect(
        @NotNull IntEnumerable other
    ) {
        NullCheck.requireNonNull(
            other,
            "other"
        );

        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private final IntHashSet remaining =
                        new IntHashSet();

                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        try (
                            IntEnumerator enumerator =
                                other.enumerator()
                        ) {
                            while (enumerator.moveNext()) {
                                remaining.add(
                                    enumerator.current()
                                );
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            int value =
                                upstream.current();

                            /*
                             * Removing the value after the first successful
                             * match ensures that every intersection value is
                             * emitted at most once.
                             */
                            if (remaining.remove(value)) {
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
    public final @NotNull IntEnumerable union(
        @NotNull IntEnumerable other
    ) {
        NullCheck.requireNonNull(
            other,
            "other"
        );

        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private final IntHashSet seen =
                        new IntHashSet();

                    private @Nullable IntEnumerator second;

                    private boolean firstCompleted;

                    @Override
                    protected boolean moveNextCore() {
                        if (!firstCompleted) {
                            while (upstream.moveNext()) {
                                int value =
                                    upstream.current();

                                if (seen.add(value)) {
                                    setCurrent(value);
                                    return true;
                                }
                            }

                            firstCompleted = true;
                            second =
                                other.enumerator();
                        }

                        IntEnumerator second =
                            this.second;

                        while (
                            second != null
                                && second.moveNext()
                        ) {
                            int value =
                                second.current();

                            if (seen.add(value)) {
                                setCurrent(value);
                                return true;
                            }
                        }

                        return false;
                    }

                    @Override
                    public void close() {
                        try {
                            IntEnumerator second =
                                this.second;

                            if (second != null) {
                                second.close();
                                this.second = null;
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
    public final @NotNull IntEnumerable order() {
        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private int[] values;
                    private int index;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        values =
                            collectToArray(upstream);

                        Arrays.sort(values);
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (index >= values.length) {
                            return false;
                        }

                        setCurrent(
                            values[index++]
                        );

                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull IntEnumerable orderDescending() {
        return new StatefulOp(this) {

            @Override
            protected @NotNull IntEnumerator opWrapEnumerator(
                @NotNull IntEnumerator upstream
            ) {
                return new IntPipelineEnumerator(upstream) {

                    private int[] values;
                    private int index;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        values =
                            collectToArray(upstream);

                        Arrays.sort(values);

                        index =
                            values.length - 1;
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (index < 0) {
                            return false;
                        }

                        setCurrent(
                            values[index--]
                        );

                        return true;
                    }
                };
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public final int @NotNull [] toArray() {
        try (IntEnumerator enumerator = enumerator()) {
            return collectToArray(enumerator);
        }
    }

    // ---------------------------------------------------------------------
    // Reference bridge
    // ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull Enumerable<Integer> boxed() {
        return Linq.fromEnumerator(
            () -> new ReferenceBridgeEnumerator<>(
                enumerator(),
                Integer::valueOf
            )
        );
    }

    // ---------------------------------------------------------------------
    // Internal primitive buffer support
    // ---------------------------------------------------------------------

    /**
     * Collects all remaining values from the specified enumerator into a
     * primitive {@code int} array.
     *
     * <p>The backing array grows geometrically and stores primitive values
     * directly, avoiding intermediate {@link Integer} objects.</p>
     *
     * @param enumerator the enumerator to consume
     * @return the collected primitive array
     */
    private static int @NotNull [] collectToArray(
        @NotNull IntEnumerator enumerator
    ) {
        int[] values =
            new int[16];

        int size = 0;

        while (enumerator.moveNext()) {
            if (size == values.length) {
                int oldCapacity =
                    values.length;

                int newCapacity =
                    oldCapacity
                        + (oldCapacity >> 1)
                        + 1;

                if (newCapacity < 0) {
                    throw new OutOfMemoryError(
                        "Required array size too large."
                    );
                }

                values =
                    Arrays.copyOf(
                        values,
                        newCapacity
                    );
            }

            values[size++] =
                enumerator.current();
        }

        return Arrays.copyOf(
            values,
            size
        );
    }

    // ---------------------------------------------------------------------
    // Pipeline stage classes
    // ---------------------------------------------------------------------

    /**
     * Source stage of a primitive {@code int} pipeline.
     *
     * <p>A head stage owns the source enumerator factory and creates a fresh
     * primitive enumerator for every enumeration.</p>
     */
    static final class Head
        extends IntEnumPipeline {

        /**
         * Creates a source stage.
         *
         * @param sourceSupplier the source-enumerator factory
         * @param parallel whether the pipeline is initially parallel
         */
        Head(
            @NotNull Supplier<? extends IntEnumerator> sourceSupplier,
            boolean parallel
        ) {
            super(
                sourceSupplier,
                parallel
            );
        }

        /**
         * Creates a sequential source stage.
         *
         * @param sourceSupplier the source-enumerator factory
         */
        Head(
            @NotNull Supplier<? extends IntEnumerator> sourceSupplier
        ) {
            this(
                sourceSupplier,
                false
            );
        }

        @Override
        protected boolean opIsStateful() {
            throw new UnsupportedOperationException(
                "The source stage does not represent an operation."
            );
        }

        @Override
        protected @NotNull IntEnumerator opWrapEnumerator(
            @NotNull IntEnumerator upstream
        ) {
            throw new UnsupportedOperationException(
                "The source stage has no upstream enumerator."
            );
        }
    }

    /**
     * Base class for stateless primitive {@code int} pipeline stages.
     *
     * <p>A stateless stage does not need to retain information about
     * previously processed elements in order to produce its next output
     * value.</p>
     */
    abstract static class StatelessOp
        extends IntEnumPipeline {

        /**
         * Creates a stateless operation appended to the specified upstream
         * pipeline.
         *
         * @param upstream the immediately preceding pipeline stage
         */
        protected StatelessOp(
            @NotNull IntEnumPipeline upstream
        ) {
            super(upstream);
        }

        @Override
        protected final boolean opIsStateful() {
            return false;
        }
    }

    /**
     * Base class for stateful primitive {@code int} pipeline stages.
     *
     * <p>Traversal-specific state must be created by the enumerator returned
     * from {@link #opWrapEnumerator(IntEnumerator)} rather than stored in the
     * pipeline stage itself.</p>
     */
    abstract static class StatefulOp
        extends IntEnumPipeline {

        /**
         * Creates a stateful operation appended to the specified upstream
         * pipeline.
         *
         * @param upstream the immediately preceding pipeline stage
         */
        protected StatefulOp(
            @NotNull IntEnumPipeline upstream
        ) {
            super(upstream);
        }

        @Override
        protected final boolean opIsStateful() {
            return true;
        }
    }

    // ---------------------------------------------------------------------
    // Primitive -> reference bridge
    // ---------------------------------------------------------------------

    /**
     * Adapts an {@link IntEnumerator} to a reference-type
     * {@link Enumerator} through a specified mapping function.
     *
     * <p>This adapter is used only when an operation explicitly crosses from
     * the primitive {@code int} pipeline into a reference-type pipeline.</p>
     *
     * @param <R> the resulting reference element type
     */
    private static final class ReferenceBridgeEnumerator<R>
        implements Enumerator<R> {

        /**
         * Primitive upstream enumerator.
         */
        private final IntEnumerator upstream;

        /**
         * Mapping function that converts primitive values into reference
         * values.
         */
        private final IntFunction<? extends R> selector;

        /**
         * Current mapped value.
         */
        private @Nullable R current;

        /**
         * Whether {@link #current()} currently represents a valid value.
         */
        private boolean hasCurrent;

        /**
         * Whether a value has already been fetched by {@link #hasNext()}.
         */
        private boolean buffered;

        /**
         * Whether the upstream sequence has been exhausted.
         */
        private boolean finished;

        /**
         * Whether this enumerator has been closed.
         */
        private boolean closed;

        /**
         * Creates a primitive-to-reference enumerator adapter.
         *
         * @param upstream the primitive upstream enumerator
         * @param selector the function used to map primitive values
         */
        private ReferenceBridgeEnumerator(
            @NotNull IntEnumerator upstream,
            @NotNull IntFunction<? extends R> selector
        ) {
            this.upstream =
                NullCheck.requireNonNull(
                    upstream,
                    "upstream"
                );

            this.selector =
                NullCheck.requireNonNull(
                    selector,
                    "selector"
                );
        }

        @Override
        public boolean moveNext() {
            ensureOpen();

            if (buffered) {
                buffered = false;
                hasCurrent = true;
                return true;
            }

            if (finished) {
                hasCurrent = false;
                return false;
            }

            hasCurrent = false;

            if (!upstream.moveNext()) {
                finished = true;
                current = null;
                return false;
            }

            current =
                selector.apply(
                    upstream.current()
                );

            hasCurrent = true;

            return true;
        }

        @Override
        public R current() {
            ensureOpen();

            if (!hasCurrent) {
                throw new IllegalStateException(
                    "The enumerator is not positioned on an element."
                );
            }

            return current;
        }

        @Override
        public boolean hasNext() {
            ensureOpen();

            if (buffered) {
                return true;
            }

            if (finished) {
                return false;
            }

            if (!upstream.moveNext()) {
                finished = true;
                hasCurrent = false;
                current = null;
                return false;
            }

            current =
                selector.apply(
                    upstream.current()
                );

            buffered = true;
            hasCurrent = false;

            return true;
        }

        @Override
        public R next() {
            ensureOpen();

            if (buffered) {
                buffered = false;
                hasCurrent = true;
                return current;
            }

            if (!moveNext()) {
                throw new NoSuchElementException();
            }

            return current;
        }

        @Override
        public void remove() {
            upstream.remove();
        }

        @Override
        public void reset() {
            upstream.reset();

            current = null;
            hasCurrent = false;
            buffered = false;
            finished = false;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            closed = true;

            current = null;
            hasCurrent = false;
            buffered = false;
            finished = true;

            upstream.close();
        }

        /**
         * Ensures that this enumerator has not been closed.
         *
         * @throws IllegalStateException if this enumerator has already been
         *         closed
         */
        private void ensureOpen() {
            if (closed) {
                throw new IllegalStateException(
                    "The enumerator has already been closed."
                );
            }
        }
    }


}

/**
 * A lightweight hash set specialized for primitive {@code int} values.
 *
 * <p>This class is intended for internal use by primitive enumerable
 * operations such as {@code distinct}, {@code union}, {@code intersect},
 * and {@code except}. Values are stored directly as primitive integers and
 * therefore do not require boxing into {@link Integer} objects.</p>
 *
 * <p>The implementation uses open addressing with linear probing. The table
 * is automatically resized when the number of stored values reaches the
 * configured load threshold.</p>
 *
 * <p>This class is not thread-safe.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
final class IntHashSet {

    /**
     * Indicates that a slot has never contained a value.
     */
    private static final byte EMPTY = 0;

    /**
     * Indicates that a slot currently contains a value.
     */
    private static final byte OCCUPIED = 1;

    /**
     * Indicates that a value was removed from a previously occupied slot.
     */
    private static final byte DELETED = 2;

    /**
     * The default initial table capacity.
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * The maximum fraction of occupied entries before the table is resized.
     */
    private static final float LOAD_FACTOR = 0.65f;

    /**
     * The primitive values stored in the hash table.
     */
    private int[] values;

    /**
     * State information corresponding to each table slot.
     */
    private byte[] states;

    /**
     * The number of values currently contained in the set.
     */
    private int size;

    /**
     * The number of values at which the table is resized.
     */
    private int threshold;

    /**
     * Creates an empty primitive integer hash set with the default initial
     * capacity.
     */
    IntHashSet() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates an empty primitive integer hash set with sufficient capacity
     * for approximately the specified number of values.
     *
     * @param expectedSize the expected number of values
     */
    IntHashSet(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException(
                "expectedSize cannot be negative: " + expectedSize
            );
        }

        int capacity = tableSizeFor(
            Math.max(
                DEFAULT_CAPACITY,
                (int) Math.ceil(expectedSize / LOAD_FACTOR)
            )
        );

        values = new int[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);
    }

    /**
     * Adds the specified primitive value to this set.
     *
     * @param value the value to add
     * @return {@code true} if the value was not already present;
     *         otherwise, {@code false}
     */
    boolean add(int value) {
        if (size + 1 > threshold) {
            resize(values.length << 1);
        }

        int mask = values.length - 1;
        int index = mix(value) & mask;
        int firstDeleted = -1;

        while (true) {
            byte state = states[index];

            if (state == EMPTY) {
                int insertionIndex =
                    firstDeleted >= 0 ? firstDeleted : index;

                values[insertionIndex] = value;
                states[insertionIndex] = OCCUPIED;
                size++;

                return true;
            }

            if (state == OCCUPIED && values[index] == value) {
                return false;
            }

            if (state == DELETED && firstDeleted < 0) {
                firstDeleted = index;
            }

            index = (index + 1) & mask;
        }
    }

    /**
     * Determines whether this set contains the specified value.
     *
     * @param value the value to locate
     * @return {@code true} if this set contains {@code value};
     *         otherwise, {@code false}
     */
    boolean contains(int value) {
        return findIndex(value) >= 0;
    }

    /**
     * Removes the specified primitive value from this set.
     *
     * @param value the value to remove
     * @return {@code true} if the value was present and removed;
     *         otherwise, {@code false}
     */
    boolean remove(int value) {
        int index = findIndex(value);

        if (index < 0) {
            return false;
        }

        states[index] = DELETED;
        size--;

        return true;
    }

    /**
     * Returns the number of values currently contained in this set.
     *
     * @return the number of values in this set
     */
    int size() {
        return size;
    }

    /**
     * Determines whether this set contains no values.
     *
     * @return {@code true} if this set is empty; otherwise, {@code false}
     */
    boolean isEmpty() {
        return size == 0;
    }

    /**
     * Removes all values from this set.
     */
    void clear() {
        Arrays.fill(states, EMPTY);
        size = 0;
    }

    /**
     * Locates the table index containing the specified value.
     *
     * @param value the value to locate
     * @return the occupied table index containing {@code value},
     *         or {@code -1} if the value is absent
     */
    private int findIndex(int value) {
        int mask = values.length - 1;
        int index = mix(value) & mask;

        while (true) {
            byte state = states[index];

            if (state == EMPTY) {
                return -1;
            }

            if (state == OCCUPIED && values[index] == value) {
                return index;
            }

            index = (index + 1) & mask;
        }
    }

    /**
     * Resizes the backing table and rehashes all currently stored values.
     *
     * @param newCapacity the requested new table capacity
     */
    private void resize(int newCapacity) {
        int capacity = tableSizeFor(newCapacity);

        int[] oldValues = values;
        byte[] oldStates = states;

        values = new int[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);

        int oldSize = size;
        size = 0;

        for (int i = 0; i < oldValues.length; i++) {
            if (oldStates[i] == OCCUPIED) {
                addRehashed(oldValues[i]);
            }
        }

        size = oldSize;
    }

    /**
     * Adds a value while rebuilding the table.
     *
     * <p>This method assumes that the value does not already occur in the
     * destination table and does not perform capacity checks.</p>
     *
     * @param value the value to add
     */
    private void addRehashed(int value) {
        int mask = values.length - 1;
        int index = mix(value) & mask;

        while (states[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }

        values[index] = value;
        states[index] = OCCUPIED;
    }

    /**
     * Computes the resize threshold for the specified table capacity.
     *
     * @param capacity the table capacity
     * @return the maximum number of entries before resizing
     */
    private static int threshold(int capacity) {
        return Math.max(
            1,
            (int) (capacity * LOAD_FACTOR)
        );
    }

    /**
     * Returns a power-of-two table capacity greater than or equal to the
     * requested capacity.
     *
     * @param capacity the requested capacity
     * @return the normalized table capacity
     */
    private static int tableSizeFor(int capacity) {
        if (capacity <= 1) {
            return 1;
        }

        int highest =
            Integer.highestOneBit(capacity - 1);

        if (highest >= (1 << 30)) {
            return 1 << 30;
        }

        return highest << 1;
    }

    /**
     * Applies a bit-mixing function to the specified integer value before
     * selecting a table bucket.
     *
     * @param value the value whose hash is mixed
     * @return the mixed hash value
     */
    private static int mix(int value) {
        int hash = value;

        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        hash ^= hash >>> 16;

        return hash;
    }
}