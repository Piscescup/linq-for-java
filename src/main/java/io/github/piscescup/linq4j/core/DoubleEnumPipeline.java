package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.Linq;
import io.github.piscescup.linq4j.enumerator.*;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.*;
import java.util.function.Supplier;

/**
 * Abstract base class for primitive {@code double} enumerable pipeline stages.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
@ApiStatus.Internal
abstract class DoubleEnumPipeline
    extends AbstractBaseEnumPipeline<Double, DoubleEnumerable>
    implements DoubleEnumerable {

    private final @Nullable DoubleEnumPipeline upstream;
    private final @Nullable Supplier<? extends DoubleEnumerator> sourceSupplier;

    protected DoubleEnumPipeline(
        @NotNull Supplier<? extends DoubleEnumerator> sourceSupplier,
        boolean parallel
    ) {
        super(parallel);
        this.upstream = null;
        this.sourceSupplier =
            NullCheck.requireNonNull(sourceSupplier, "sourceSupplier");
    }

    protected DoubleEnumPipeline(@NotNull DoubleEnumPipeline upstream) {
        super(upstream);
        this.upstream = NullCheck.requireNonNull(upstream, "upstream");
        this.sourceSupplier = null;
    }

    protected abstract boolean opIsStateful();

    protected abstract @NotNull DoubleEnumerator opWrapEnumerator(
        @NotNull DoubleEnumerator upstream
    );

    @Override
    public final @NotNull DoubleEnumerator enumerator() {
        if (sourceSupplier != null) {
            return NullCheck.requireNonNull(
                sourceSupplier.get(),
                "sourceSupplier returned null"
            );
        }

        DoubleEnumPipeline upstream = this.upstream;

        if (upstream == null) {
            throw new IllegalStateException(
                "Non-source pipeline stage does not have an upstream stage."
            );
        }

        return opWrapEnumerator(upstream.enumerator());
    }

    @Override
    public final double aggregateToResult(
        double seed,
        @NotNull DoubleBinaryOperator aggregator
    ) {
        NullCheck.requireNonNull(aggregator, "aggregator");
        double accumulator = seed;

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                accumulator =
                    aggregator.applyAsDouble(accumulator, enumerator.current());
            }
        }

        return accumulator;
    }

    @Override
    public final boolean all(@NotNull DoublePredicate predicate) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (!predicate.test(enumerator.current())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public final boolean any() {
        try (DoubleEnumerator enumerator = enumerator()) {
            return enumerator.moveNext();
        }
    }

    @Override
    public final boolean any(@NotNull DoublePredicate predicate) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (predicate.test(enumerator.current())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public final boolean contains(double value) {
        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (Double.doubleToLongBits(enumerator.current())
                    == Double.doubleToLongBits(value)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public final @NotNull DoubleEnumerable append(double element) {
        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
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

    @Override
    public final @NotNull DoubleEnumerable prepend(double element) {
        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
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

                        setCurrent(upstream.current());
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final @NotNull DoubleEnumerable concat(
        @NotNull DoubleEnumerable after
    ) {
        NullCheck.requireNonNull(after, "after");

        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private @Nullable DoubleEnumerator second;
                    private boolean firstCompleted;

                    @Override
                    protected boolean moveNextCore() {
                        if (!firstCompleted) {
                            if (upstream.moveNext()) {
                                setCurrent(upstream.current());
                                return true;
                            }

                            firstCompleted = true;
                            second = after.enumerator();
                        }

                        DoubleEnumerator second = this.second;

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

    @Override
    public final @NotNull DoubleEnumerable defaultIfEmpty(double defaultValue) {
        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
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

                        if (!upstream.moveNext()) {
                            return false;
                        }

                        setCurrent(upstream.current());
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final double elementAt(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(
                "Index cannot be negative: " + index
            );
        }

        try (DoubleEnumerator enumerator = enumerator()) {
            int currentIndex = 0;

            while (enumerator.moveNext()) {
                if (currentIndex == index) {
                    return enumerator.current();
                }

                currentIndex++;
            }

            throw new IndexOutOfBoundsException(
                "Index " + index
                    + " is out of bounds for a sequence of length "
                    + currentIndex
            );
        }
    }

    @Override
    public final @NotNull OptionalDouble elementAtOrEmpty(int index) {
        if (index < 0) {
            return OptionalDouble.empty();
        }

        try (DoubleEnumerator enumerator = enumerator()) {
            int currentIndex = 0;

            while (enumerator.moveNext()) {
                if (currentIndex == index) {
                    return OptionalDouble.of(enumerator.current());
                }

                currentIndex++;
            }
        }

        return OptionalDouble.empty();
    }

    @Override
    public final double elementAtOrDefault(int index, double defaultValue) {
        if (index < 0) {
            return defaultValue;
        }

        try (DoubleEnumerator enumerator = enumerator()) {
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

    @Override
    public final double first() {
        try (DoubleEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            return enumerator.current();
        }
    }

    @Override
    public final double first(@NotNull DoublePredicate predicate) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                double value = enumerator.current();

                if (predicate.test(value)) {
                    return value;
                }
            }
        }

        throw new NoSuchElementException(
            "No element satisfies the condition."
        );
    }

    @Override
    public final @NotNull OptionalDouble firstOrEmpty() {
        try (DoubleEnumerator enumerator = enumerator()) {
            if (enumerator.moveNext()) {
                return OptionalDouble.of(enumerator.current());
            }
        }

        return OptionalDouble.empty();
    }

    @Override
    public final @NotNull OptionalDouble firstOrEmpty(
        @NotNull DoublePredicate predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                double value = enumerator.current();

                if (predicate.test(value)) {
                    return OptionalDouble.of(value);
                }
            }
        }

        return OptionalDouble.empty();
    }

    @Override
    public final long count() {
        long count = 0L;

        try (DoubleEnumerator enumerator = enumerator()) {
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

    @Override
    public final long count(@NotNull DoublePredicate predicate) {
        NullCheck.requireNonNull(predicate, "predicate");
        long count = 0L;

        try (DoubleEnumerator enumerator = enumerator()) {
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

    @Override
    public final double sum() {
        double sum = 0;

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                sum = sum + enumerator.current();
            }
        }

        return sum;
    }

    @Override
    public final double min() {
        try (DoubleEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            double minimum = enumerator.current();

            while (enumerator.moveNext()) {
                double value = enumerator.current();

                if (Double.compare(value, minimum) < 0) {
                    minimum = value;
                }
            }

            return minimum;
        }
    }

    @Override
    public final double max() {
        try (DoubleEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            double maximum = enumerator.current();

            while (enumerator.moveNext()) {
                double value = enumerator.current();

                if (Double.compare(value, maximum) > 0) {
                    maximum = value;
                }
            }

            return maximum;
        }
    }

    @Override
    public final double average() {
        double sum = 0.0;
        long count = 0L;

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                sum += enumerator.current();

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

        return sum / count;
    }

    @Override
    public final @NotNull DoubleEnumerable where(
        @NotNull DoublePredicate predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            double value = upstream.current();

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

    @Override
    public final @NotNull DoubleEnumerable select(
        @NotNull DoubleUnaryOperator selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    @Override
                    protected boolean moveNextCore() {
                        if (!upstream.moveNext()) {
                            return false;
                        }

                        setCurrent(selector.applyAsDouble(upstream.current()));
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final @NotNull IntEnumerable selectToInt(
        @NotNull DoubleToIntFunction selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new IntEnumPipeline.Head(
            () -> new DoubleToIntBridgeEnumerator(enumerator(), selector),
            isParallel()
        );
    }

    @Override
    public final @NotNull LongEnumerable selectToLong(
        @NotNull DoubleToLongFunction selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new LongEnumPipeline.Head(
            () -> new DoubleToLongBridgeEnumerator(enumerator(), selector),
            isParallel()
        );
    }

    @Override
    public final <R> @NotNull Enumerable<R> selectToObj(
        @NotNull DoubleFunction<? extends R> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return Linq.fromEnumerator(
            () -> new ReferenceBridgeEnumerator<>(enumerator(), selector)
        );
    }

    @Override
    public final @NotNull DoubleEnumerable skip(int count) {
        final int skipCount = Math.max(0, count);

        if (skipCount == 0) {
            return this;
        }

        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private int remaining = skipCount;

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

                        setCurrent(upstream.current());
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final @NotNull DoubleEnumerable take(int count) {
        final int takeCount = Math.max(0, count);

        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private int remaining = takeCount;

                    @Override
                    protected boolean moveNextCore() {
                        if (remaining <= 0 || !upstream.moveNext()) {
                            return false;
                        }

                        remaining--;
                        setCurrent(upstream.current());
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final @NotNull DoubleEnumerable skipWhile(
        @NotNull DoublePredicate predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private boolean skipping = true;

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            double value = upstream.current();

                            if (skipping && predicate.test(value)) {
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

    @Override
    public final @NotNull DoubleEnumerable takeWhile(
        @NotNull DoublePredicate predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        return new StatelessOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private boolean taking = true;

                    @Override
                    protected boolean moveNextCore() {
                        if (!taking || !upstream.moveNext()) {
                            return false;
                        }

                        double value = upstream.current();

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

    @Override
    public final @NotNull DoubleEnumerable skipLast(int count) {
        final int skipCount = Math.max(0, count);

        if (skipCount == 0) {
            return this;
        }

        return new StatefulOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private final double[] queue = new double[skipCount];
                    private int head;
                    private int size;

                    @Override
                    protected boolean moveNextCore() {
                        while (size < skipCount) {
                            if (!upstream.moveNext()) {
                                return false;
                            }

                            queue[(head + size) % skipCount] =
                                upstream.current();

                            size++;
                        }

                        if (!upstream.moveNext()) {
                            return false;
                        }

                        double result = queue[head];
                        queue[head] = upstream.current();
                        head = (head + 1) % skipCount;

                        setCurrent(result);
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final @NotNull DoubleEnumerable takeLast(int count) {
        final int takeCount = Math.max(0, count);

        return new StatefulOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private double[] values;
                    private int size;
                    private int index;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        if (takeCount == 0) {
                            values = new double[0];
                            return;
                        }

                        double[] ring = new double[takeCount];
                        int total = 0;

                        while (upstream.moveNext()) {
                            ring[total % takeCount] = upstream.current();
                            total++;
                        }

                        size = Math.min(total, takeCount);
                        values = new double[size];

                        int start =
                            total <= takeCount ? 0 : total % takeCount;

                        for (int i = 0; i < size; i++) {
                            values[i] =
                                ring[(start + i) % takeCount];
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (index >= size) {
                            return false;
                        }

                        setCurrent(values[index++]);
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final @NotNull DoubleEnumerable reverse() {
        return new StatefulOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private double[] values;
                    private int index;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;
                        values = collectToArray(upstream);
                        index = values.length - 1;
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (index < 0) {
                            return false;
                        }

                        setCurrent(values[index--]);
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final @NotNull DoubleEnumerable shuffle() {
        return new StatefulOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private double[] values;
                    private int index;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;
                        values = collectToArray(upstream);

                        ThreadLocalRandom random =
                            ThreadLocalRandom.current();

                        for (int i = values.length - 1; i > 0; i--) {
                            int j = random.nextInt(i + 1);
                            double temporary = values[i];
                            values[i] = values[j];
                            values[j] = temporary;
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        if (index >= values.length) {
                            return false;
                        }

                        setCurrent(values[index++]);
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final boolean sequenceEqual(@NotNull DoubleEnumerable other) {
        NullCheck.requireNonNull(other, "other");

        try (
            DoubleEnumerator first = enumerator();
            DoubleEnumerator second = other.enumerator()
        ) {
            while (true) {
                boolean firstHas = first.moveNext();
                boolean secondHas = second.moveNext();

                if (firstHas != secondHas) {
                    return false;
                }

                if (!firstHas) {
                    return true;
                }

                if (Double.doubleToLongBits(first.current())
                    != Double.doubleToLongBits(second.current())) {
                    return false;
                }
            }
        }
    }

    @Override
    public final @NotNull DoubleEnumerable distinct() {
        return new StatefulOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private final DoubleHashSet seen = new DoubleHashSet();

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            double value = upstream.current();

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

    @Override
    public final @NotNull DoubleEnumerable except(
        @NotNull DoubleEnumerable other
    ) {
        NullCheck.requireNonNull(other, "other");

        return new StatefulOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private final DoubleHashSet seen = new DoubleHashSet();
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        try (DoubleEnumerator enumerator = other.enumerator()) {
                            while (enumerator.moveNext()) {
                                seen.add(enumerator.current());
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            double value = upstream.current();

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

    @Override
    public final @NotNull DoubleEnumerable intersect(
        @NotNull DoubleEnumerable other
    ) {
        NullCheck.requireNonNull(other, "other");

        return new StatefulOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private final DoubleHashSet remaining = new DoubleHashSet();
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        try (DoubleEnumerator enumerator = other.enumerator()) {
                            while (enumerator.moveNext()) {
                                remaining.add(enumerator.current());
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            double value = upstream.current();

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

    @Override
    public final @NotNull DoubleEnumerable union(
        @NotNull DoubleEnumerable other
    ) {
        NullCheck.requireNonNull(other, "other");

        return new StatefulOp(this) {
            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {
                    private final DoubleHashSet seen = new DoubleHashSet();
                    private @Nullable DoubleEnumerator second;
                    private boolean firstCompleted;

                    @Override
                    protected boolean moveNextCore() {
                        if (!firstCompleted) {
                            while (upstream.moveNext()) {
                                double value = upstream.current();

                                if (seen.add(value)) {
                                    setCurrent(value);
                                    return true;
                                }
                            }

                            firstCompleted = true;
                            second = other.enumerator();
                        }

                        DoubleEnumerator second = this.second;

                        while (second != null && second.moveNext()) {
                            double value = second.current();

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

    // ---------------------------------------------------------------------
// Last
// ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final double last() {
        try (DoubleEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            double result = enumerator.current();

            while (enumerator.moveNext()) {
                result = enumerator.current();
            }

            return result;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final double last(
        @NotNull DoublePredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        double result = 0.0;

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                double value = enumerator.current();

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
    public final @NotNull OptionalDouble lastOrEmpty() {
        boolean found = false;
        double result = 0.0;

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                result = enumerator.current();
                found = true;
            }
        }

        return found
            ? OptionalDouble.of(result)
            : OptionalDouble.empty();
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalDouble lastOrEmpty(
        @NotNull DoublePredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        double result = 0.0;

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                double value = enumerator.current();

                if (predicate.test(value)) {
                    result = value;
                    found = true;
                }
            }
        }

        return found
            ? OptionalDouble.of(result)
            : OptionalDouble.empty();
    }


// ---------------------------------------------------------------------
// Single
// ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final double single() {
        try (DoubleEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            double result = enumerator.current();

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
    public final double single(
        @NotNull DoublePredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        double result = 0.0;

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                double value = enumerator.current();

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
    public final @NotNull OptionalDouble singleOrEmpty() {
        try (DoubleEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                return OptionalDouble.empty();
            }

            double result = enumerator.current();

            if (enumerator.moveNext()) {
                throw new IllegalStateException(
                    "Sequence contains more than one element."
                );
            }

            return OptionalDouble.of(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalDouble singleOrEmpty(
        @NotNull DoublePredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        double result = 0.0;

        try (DoubleEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                double value = enumerator.current();

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
            ? OptionalDouble.of(result)
            : OptionalDouble.empty();
    }


// ---------------------------------------------------------------------
// Ordering
// ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull DoubleEnumerable order() {
        return new StatefulOp(this) {

            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {

                    private double[] values;
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
    public final @NotNull DoubleEnumerable orderDescending() {
        return new StatefulOp(this) {

            @Override
            protected @NotNull DoubleEnumerator opWrapEnumerator(
                @NotNull DoubleEnumerator upstream
            ) {
                return new DoublePipelineEnumerator(upstream) {

                    private double[] values;
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

    @Override
    public final double @NotNull [] toArray() {
        try (DoubleEnumerator enumerator = enumerator()) {
            return collectToArray(enumerator);
        }
    }

    @Override
    public final @NotNull Enumerable<Double> boxed() {
        return Linq.fromEnumerator(
            () -> new ReferenceBridgeEnumerator<>(
                enumerator(),
                Double::valueOf
            )
        );
    }

    private static double @NotNull [] collectToArray(
        @NotNull DoubleEnumerator enumerator
    ) {
        double[] values = new double[16];
        int size = 0;

        while (enumerator.moveNext()) {
            if (size == values.length) {
                int oldCapacity = values.length;
                int newCapacity =
                    oldCapacity + (oldCapacity >> 1) + 1;

                if (newCapacity < 0) {
                    throw new OutOfMemoryError(
                        "Required array size too large."
                    );
                }

                values = Arrays.copyOf(values, newCapacity);
            }

            values[size++] = enumerator.current();
        }

        return Arrays.copyOf(values, size);
    }

    @ApiStatus.Internal
    static final class Head extends DoubleEnumPipeline {

        Head(
            @NotNull Supplier<? extends DoubleEnumerator> sourceSupplier,
            boolean parallel
        ) {
            super(sourceSupplier, parallel);
        }

        Head(@NotNull Supplier<? extends DoubleEnumerator> sourceSupplier) {
            this(sourceSupplier, false);
        }

        @Override
        protected boolean opIsStateful() {
            throw new UnsupportedOperationException(
                "The source stage does not represent an operation."
            );
        }

        @Override
        protected @NotNull DoubleEnumerator opWrapEnumerator(
            @NotNull DoubleEnumerator upstream
        ) {
            throw new UnsupportedOperationException(
                "The source stage has no upstream enumerator."
            );
        }
    }

    abstract static class StatelessOp extends DoubleEnumPipeline {

        protected StatelessOp(@NotNull DoubleEnumPipeline upstream) {
            super(upstream);
        }

        @Override
        protected final boolean opIsStateful() {
            return false;
        }
    }

    abstract static class StatefulOp extends DoubleEnumPipeline {

        protected StatefulOp(@NotNull DoubleEnumPipeline upstream) {
            super(upstream);
        }

        @Override
        protected final boolean opIsStateful() {
            return true;
        }
    }

    private static final class ReferenceBridgeEnumerator<R>
        implements Enumerator<R> {

        private final DoubleEnumerator upstream;
        private final DoubleFunction<? extends R> selector;

        private @Nullable R current;
        private boolean hasCurrent;
        private boolean buffered;
        private boolean finished;
        private boolean closed;

        private ReferenceBridgeEnumerator(
            @NotNull DoubleEnumerator upstream,
            @NotNull DoubleFunction<? extends R> selector
        ) {
            this.upstream =
                NullCheck.requireNonNull(upstream, "upstream");
            this.selector =
                NullCheck.requireNonNull(selector, "selector");
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

            current = selector.apply(upstream.current());
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

            current = selector.apply(upstream.current());
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

        private void ensureOpen() {
            if (closed) {
                throw new IllegalStateException(
                    "The enumerator has already been closed."
                );
            }
        }
    }

    private static final class DoubleToIntBridgeEnumerator
        implements IntEnumerator {

        private final DoubleEnumerator upstream;
        private final DoubleToIntFunction selector;
        private int current;
        private boolean hasCurrent;

        private DoubleToIntBridgeEnumerator(
            DoubleEnumerator upstream,
            DoubleToIntFunction selector
        ) {
            this.upstream = upstream;
            this.selector = selector;
        }

        @Override
        public boolean moveNext() {
            hasCurrent = false;

            if (!upstream.moveNext()) {
                return false;
            }

            current = selector.applyAsInt(upstream.current());
            hasCurrent = true;
            return true;
        }

        @Override
        public int current() {
            if (!hasCurrent) {
                throw new IllegalStateException(
                    "The enumerator is not positioned on an element."
                );
            }

            return current;
        }

        @Override
        public void close() {
            upstream.close();
        }
    }

    private static final class DoubleToLongBridgeEnumerator
        implements LongEnumerator {

        private final DoubleEnumerator upstream;
        private final DoubleToLongFunction selector;
        private long current;
        private boolean hasCurrent;

        private DoubleToLongBridgeEnumerator(
            DoubleEnumerator upstream,
            DoubleToLongFunction selector
        ) {
            this.upstream = upstream;
            this.selector = selector;
        }

        @Override
        public boolean moveNext() {
            hasCurrent = false;

            if (!upstream.moveNext()) {
                return false;
            }

            current = selector.applyAsLong(upstream.current());
            hasCurrent = true;
            return true;
        }

        @Override
        public long current() {
            if (!hasCurrent) {
                throw new IllegalStateException(
                    "The enumerator is not positioned on an element."
                );
            }

            return current;
        }

        @Override
        public void close() {
            upstream.close();
        }
    }
}


/**
 * A lightweight hash set specialized for primitive {@code double} values.
 *
 * <p>This class is intended for internal use by primitive enumerable
 * operations such as {@code distinct}, {@code union}, {@code intersect},
 * and {@code except}. Values are stored directly as primitive values and
 * therefore do not require boxing into {@link Double} objects.</p>
 *
 * <p>Equality is determined using
 * {@link Double#doubleToLongBits(double)}. Consequently, all NaN values are
 * considered equal, while positive zero ({@code 0.0}) and negative zero
 * ({@code -0.0}) are considered distinct values.</p>
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
final class DoubleHashSet {

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
     * The bit representations of the primitive {@code double} values stored
     * in the hash table.
     *
     * <p>Values are represented using
     * {@link Double#doubleToLongBits(double)} so that equality semantics are
     * consistent for NaN and signed zero values.</p>
     */
    private long[] bits;

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
     * Creates an empty primitive {@code double} hash set with the default
     * initial capacity.
     */
    DoubleHashSet() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates an empty primitive {@code double} hash set with sufficient
     * capacity for approximately the specified number of values.
     *
     * @param expectedSize the expected number of values
     * @throws IllegalArgumentException if {@code expectedSize} is negative
     */
    DoubleHashSet(int expectedSize) {
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

        bits = new long[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);
    }

    /**
     * Adds the specified primitive value to this set.
     *
     * <p>All NaN representations are treated as the same value, while
     * {@code 0.0} and {@code -0.0} are treated as distinct values.</p>
     *
     * @param value the value to add
     * @return {@code true} if the value was not already present;
     *         otherwise, {@code false}
     */
    boolean add(double value) {
        long valueBits = Double.doubleToLongBits(value);

        if (size + 1 > threshold) {
            resize(bits.length << 1);
        }

        int mask = bits.length - 1;
        int index = indexFor(valueBits, mask);
        int firstDeleted = -1;

        while (true) {
            byte state = states[index];

            if (state == EMPTY) {
                int insertionIndex =
                    firstDeleted >= 0
                        ? firstDeleted
                        : index;

                bits[insertionIndex] = valueBits;
                states[insertionIndex] = OCCUPIED;
                size++;

                return true;
            }

            if (
                state == OCCUPIED &&
                    bits[index] == valueBits
            ) {
                return false;
            }

            if (
                state == DELETED &&
                    firstDeleted < 0
            ) {
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
    boolean contains(double value) {
        return findIndex(
            Double.doubleToLongBits(value)
        ) >= 0;
    }

    /**
     * Removes the specified primitive value from this set.
     *
     * @param value the value to remove
     * @return {@code true} if the value was present and removed;
     *         otherwise, {@code false}
     */
    boolean remove(double value) {
        int index = findIndex(
            Double.doubleToLongBits(value)
        );

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
     * @return {@code true} if this set is empty;
     *         otherwise, {@code false}
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
     * Locates the table index containing the specified bit representation.
     *
     * @param valueBits the canonical bit representation of the value to locate
     * @return the occupied table index containing {@code valueBits},
     *         or {@code -1} if the value is absent
     */
    private int findIndex(long valueBits) {
        int mask = bits.length - 1;
        int index = indexFor(valueBits, mask);

        while (true) {
            byte state = states[index];

            if (state == EMPTY) {
                return -1;
            }

            if (
                state == OCCUPIED &&
                    bits[index] == valueBits
            ) {
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
        int capacity =
            tableSizeFor(newCapacity);

        long[] oldBits = bits;
        byte[] oldStates = states;

        bits = new long[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);

        for (int i = 0; i < oldBits.length; i++) {
            if (oldStates[i] == OCCUPIED) {
                addRehashed(oldBits[i]);
            }
        }
    }

    /**
     * Adds a canonical bit representation while rebuilding the table.
     *
     * <p>This method assumes that the value does not already occur in the
     * destination table and does not perform capacity checks.</p>
     *
     * @param valueBits the bit representation to add
     */
    private void addRehashed(long valueBits) {
        int mask = bits.length - 1;
        int index = indexFor(valueBits, mask);

        while (states[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }

        bits[index] = valueBits;
        states[index] = OCCUPIED;
    }

    /**
     * Computes the initial table index for the specified bit representation.
     *
     * @param valueBits the bit representation whose hash is computed
     * @param mask the table mask
     * @return the initial table index
     */
    private static int indexFor(
        long valueBits,
        int mask
    ) {
        return mix(valueBits) & mask;
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
     * Applies a bit-mixing function to the specified {@code long} value before
     * selecting a table bucket.
     *
     * @param value the value whose hash is mixed
     * @return the mixed hash value
     */
    private static int mix(long value) {
        long hash = value;

        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdl;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53l;
        hash ^= hash >>> 33;

        return (int) (hash ^ (hash >>> 32));
    }
}
