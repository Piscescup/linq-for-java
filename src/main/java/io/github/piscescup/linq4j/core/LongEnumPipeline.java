package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.Linq;
import io.github.piscescup.linq4j.enumerator.*;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.*;
import java.util.function.Supplier;

/**
 * Abstract base class for primitive {@code long} enumerable pipeline stages.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
@ApiStatus.Internal
abstract class LongEnumPipeline
    extends AbstractBaseEnumPipeline<Long, LongEnumerable>
    implements LongEnumerable {

    private final @Nullable LongEnumPipeline upstream;
    private final @Nullable Supplier<? extends LongEnumerator> sourceSupplier;

    protected LongEnumPipeline(
        @NotNull Supplier<? extends LongEnumerator> sourceSupplier,
        boolean parallel
    ) {
        super(parallel);
        this.upstream = null;
        this.sourceSupplier =
            NullCheck.requireNonNull(sourceSupplier, "sourceSupplier");
    }

    protected LongEnumPipeline(@NotNull LongEnumPipeline upstream) {
        super(upstream);
        this.upstream = NullCheck.requireNonNull(upstream, "upstream");
        this.sourceSupplier = null;
    }

    protected abstract boolean opIsStateful();

    protected abstract @NotNull LongEnumerator opWrapEnumerator(
        @NotNull LongEnumerator upstream
    );

    @Override
    public final @NotNull LongEnumerator enumerator() {
        if (sourceSupplier != null) {
            return NullCheck.requireNonNull(
                sourceSupplier.get(),
                "sourceSupplier returned null"
            );
        }

        LongEnumPipeline upstream = this.upstream;

        if (upstream == null) {
            throw new IllegalStateException(
                "Non-source pipeline stage does not have an upstream stage."
            );
        }

        return opWrapEnumerator(upstream.enumerator());
    }

    @Override
    public final long aggregateToResult(
        long seed,
        @NotNull LongBinaryOperator aggregator
    ) {
        NullCheck.requireNonNull(aggregator, "aggregator");
        long accumulator = seed;

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                accumulator =
                    aggregator.applyAsLong(accumulator, enumerator.current());
            }
        }

        return accumulator;
    }

    @Override
    public final boolean all(@NotNull LongPredicate predicate) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (LongEnumerator enumerator = enumerator()) {
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
        try (LongEnumerator enumerator = enumerator()) {
            return enumerator.moveNext();
        }
    }

    @Override
    public final boolean any(@NotNull LongPredicate predicate) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (predicate.test(enumerator.current())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public final boolean contains(long value) {
        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                if (enumerator.current() == value) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public final @NotNull LongEnumerable append(long element) {
        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
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
    public final @NotNull LongEnumerable prepend(long element) {
        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
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
    public final @NotNull LongEnumerable concat(
        @NotNull LongEnumerable after
    ) {
        NullCheck.requireNonNull(after, "after");

        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private @Nullable LongEnumerator second;
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

                        LongEnumerator second = this.second;

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
    public final @NotNull LongEnumerable defaultIfEmpty(long defaultValue) {
        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
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
    public final long elementAt(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(
                "Index cannot be negative: " + index
            );
        }

        try (LongEnumerator enumerator = enumerator()) {
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
    public final @NotNull OptionalLong elementAtOrEmpty(int index) {
        if (index < 0) {
            return OptionalLong.empty();
        }

        try (LongEnumerator enumerator = enumerator()) {
            int currentIndex = 0;

            while (enumerator.moveNext()) {
                if (currentIndex == index) {
                    return OptionalLong.of(enumerator.current());
                }

                currentIndex++;
            }
        }

        return OptionalLong.empty();
    }

    @Override
    public final long elementAtOrDefault(int index, long defaultValue) {
        if (index < 0) {
            return defaultValue;
        }

        try (LongEnumerator enumerator = enumerator()) {
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
    public final long first() {
        try (LongEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            return enumerator.current();
        }
    }

    @Override
    public final long first(@NotNull LongPredicate predicate) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                long value = enumerator.current();

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
    public final @NotNull OptionalLong firstOrEmpty() {
        try (LongEnumerator enumerator = enumerator()) {
            if (enumerator.moveNext()) {
                return OptionalLong.of(enumerator.current());
            }
        }

        return OptionalLong.empty();
    }

    @Override
    public final @NotNull OptionalLong firstOrEmpty(
        @NotNull LongPredicate predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                long value = enumerator.current();

                if (predicate.test(value)) {
                    return OptionalLong.of(value);
                }
            }
        }

        return OptionalLong.empty();
    }

    @Override
    public final long count() {
        long count = 0L;

        try (LongEnumerator enumerator = enumerator()) {
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
    public final long count(@NotNull LongPredicate predicate) {
        NullCheck.requireNonNull(predicate, "predicate");
        long count = 0L;

        try (LongEnumerator enumerator = enumerator()) {
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
    public final long sum() {
        long sum = 0L;

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                sum = Math.addExact(sum, enumerator.current());
            }
        }

        return sum;
    }

    @Override
    public final long min() {
        try (LongEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            long minimum = enumerator.current();

            while (enumerator.moveNext()) {
                long value = enumerator.current();

                if (value < minimum) {
                    minimum = value;
                }
            }

            return minimum;
        }
    }

    @Override
    public final long max() {
        try (LongEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            long maximum = enumerator.current();

            while (enumerator.moveNext()) {
                long value = enumerator.current();

                if (value > maximum) {
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

        try (LongEnumerator enumerator = enumerator()) {
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
    public final @NotNull LongEnumerable where(
        @NotNull LongPredicate predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            long value = upstream.current();

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
    public final @NotNull LongEnumerable select(
        @NotNull LongUnaryOperator selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    @Override
                    protected boolean moveNextCore() {
                        if (!upstream.moveNext()) {
                            return false;
                        }

                        setCurrent(selector.applyAsLong(upstream.current()));
                        return true;
                    }
                };
            }
        };
    }

    @Override
    public final @NotNull IntEnumerable selectToInt(
        @NotNull LongToIntFunction selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new IntEnumPipeline.Head(
            () -> new LongToIntBridgeEnumerator(enumerator(), selector),
            isParallel()
        );
    }

    @Override
    public final @NotNull DoubleEnumerable selectToDouble(
        @NotNull LongToDoubleFunction selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new DoubleEnumPipeline.Head(
            () -> new LongToDoubleBridgeEnumerator(enumerator(), selector),
            isParallel()
        );
    }

    @Override
    public final <R> @NotNull Enumerable<R> selectToObj(
        @NotNull LongFunction<? extends R> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return Linq.fromEnumerator(
            () -> new ReferenceBridgeEnumerator<>(enumerator(), selector)
        );
    }

    @Override
    public final @NotNull LongEnumerable skip(int count) {
        final int skipCount = Math.max(0, count);

        if (skipCount == 0) {
            return this;
        }

        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
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
    public final @NotNull LongEnumerable take(int count) {
        final int takeCount = Math.max(0, count);

        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
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
    public final @NotNull LongEnumerable skipWhile(
        @NotNull LongPredicate predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private boolean skipping = true;

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            long value = upstream.current();

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
    public final @NotNull LongEnumerable takeWhile(
        @NotNull LongPredicate predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        return new StatelessOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private boolean taking = true;

                    @Override
                    protected boolean moveNextCore() {
                        if (!taking || !upstream.moveNext()) {
                            return false;
                        }

                        long value = upstream.current();

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
    public final @NotNull LongEnumerable skipLast(int count) {
        final int skipCount = Math.max(0, count);

        if (skipCount == 0) {
            return this;
        }

        return new StatefulOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private final long[] queue = new long[skipCount];
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

                        long result = queue[head];
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
    public final @NotNull LongEnumerable takeLast(int count) {
        final int takeCount = Math.max(0, count);

        return new StatefulOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private long[] values;
                    private int size;
                    private int index;
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        if (takeCount == 0) {
                            values = new long[0];
                            return;
                        }

                        long[] ring = new long[takeCount];
                        int total = 0;

                        while (upstream.moveNext()) {
                            ring[total % takeCount] = upstream.current();
                            total++;
                        }

                        size = Math.min(total, takeCount);
                        values = new long[size];

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
    public final @NotNull LongEnumerable reverse() {
        return new StatefulOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private long[] values;
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
    public final @NotNull LongEnumerable shuffle() {
        return new StatefulOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private long[] values;
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
                            long temporary = values[i];
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
    public final boolean sequenceEqual(@NotNull LongEnumerable other) {
        NullCheck.requireNonNull(other, "other");

        try (
            LongEnumerator first = enumerator();
            LongEnumerator second = other.enumerator()
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

                if (first.current() != second.current()) {
                    return false;
                }
            }
        }
    }

    @Override
    public final @NotNull LongEnumerable distinct() {
        return new StatefulOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private final LongHashSet seen = new LongHashSet();

                    @Override
                    protected boolean moveNextCore() {
                        while (upstream.moveNext()) {
                            long value = upstream.current();

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
    public final @NotNull LongEnumerable except(
        @NotNull LongEnumerable other
    ) {
        NullCheck.requireNonNull(other, "other");

        return new StatefulOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private final LongHashSet seen = new LongHashSet();
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        try (LongEnumerator enumerator = other.enumerator()) {
                            while (enumerator.moveNext()) {
                                seen.add(enumerator.current());
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            long value = upstream.current();

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
    public final @NotNull LongEnumerable intersect(
        @NotNull LongEnumerable other
    ) {
        NullCheck.requireNonNull(other, "other");

        return new StatefulOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private final LongHashSet remaining = new LongHashSet();
                    private boolean initialized;

                    private void initialize() {
                        if (initialized) {
                            return;
                        }

                        initialized = true;

                        try (LongEnumerator enumerator = other.enumerator()) {
                            while (enumerator.moveNext()) {
                                remaining.add(enumerator.current());
                            }
                        }
                    }

                    @Override
                    protected boolean moveNextCore() {
                        initialize();

                        while (upstream.moveNext()) {
                            long value = upstream.current();

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
    public final @NotNull LongEnumerable union(
        @NotNull LongEnumerable other
    ) {
        NullCheck.requireNonNull(other, "other");

        return new StatefulOp(this) {
            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {
                    private final LongHashSet seen = new LongHashSet();
                    private @Nullable LongEnumerator second;
                    private boolean firstCompleted;

                    @Override
                    protected boolean moveNextCore() {
                        if (!firstCompleted) {
                            while (upstream.moveNext()) {
                                long value = upstream.current();

                                if (seen.add(value)) {
                                    setCurrent(value);
                                    return true;
                                }
                            }

                            firstCompleted = true;
                            second = other.enumerator();
                        }

                        LongEnumerator second = this.second;

                        while (second != null && second.moveNext()) {
                            long value = second.current();

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
    public final long last() {
        try (LongEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            long result = enumerator.current();

            while (enumerator.moveNext()) {
                result = enumerator.current();
            }

            return result;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final long last(
        @NotNull LongPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        long result = 0L;

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                long value = enumerator.current();

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
    public final @NotNull OptionalLong lastOrEmpty() {
        boolean found = false;
        long result = 0L;

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                result = enumerator.current();
                found = true;
            }
        }

        return found
            ? OptionalLong.of(result)
            : OptionalLong.empty();
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalLong lastOrEmpty(
        @NotNull LongPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        long result = 0L;

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                long value = enumerator.current();

                if (predicate.test(value)) {
                    result = value;
                    found = true;
                }
            }
        }

        return found
            ? OptionalLong.of(result)
            : OptionalLong.empty();
    }


// ---------------------------------------------------------------------
// Single
// ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final long single() {
        try (LongEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException(
                    "Sequence contains no elements."
                );
            }

            long result = enumerator.current();

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
    public final long single(
        @NotNull LongPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        long result = 0L;

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                long value = enumerator.current();

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
    public final @NotNull OptionalLong singleOrEmpty() {
        try (LongEnumerator enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                return OptionalLong.empty();
            }

            long result = enumerator.current();

            if (enumerator.moveNext()) {
                throw new IllegalStateException(
                    "Sequence contains more than one element."
                );
            }

            return OptionalLong.of(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final @NotNull OptionalLong singleOrEmpty(
        @NotNull LongPredicate predicate
    ) {
        NullCheck.requireNonNull(
            predicate,
            "predicate"
        );

        boolean found = false;
        long result = 0L;

        try (LongEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                long value = enumerator.current();

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
            ? OptionalLong.of(result)
            : OptionalLong.empty();
    }


// ---------------------------------------------------------------------
// Ordering
// ---------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final @NotNull LongEnumerable order() {
        return new StatefulOp(this) {

            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {

                    private long[] values;
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
    public final @NotNull LongEnumerable orderDescending() {
        return new StatefulOp(this) {

            @Override
            protected @NotNull LongEnumerator opWrapEnumerator(
                @NotNull LongEnumerator upstream
            ) {
                return new LongPipelineEnumerator(upstream) {

                    private long[] values;
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
    public final long @NotNull [] toArray() {
        try (LongEnumerator enumerator = enumerator()) {
            return collectToArray(enumerator);
        }
    }

    @Override
    public final @NotNull Enumerable<Long> boxed() {
        return Linq.fromEnumerator(
            () -> new ReferenceBridgeEnumerator<>(
                enumerator(),
                Long::valueOf
            )
        );
    }

    private static long @NotNull [] collectToArray(
        @NotNull LongEnumerator enumerator
    ) {
        long[] values = new long[16];
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

    static final class Head extends LongEnumPipeline {

        Head(
            @NotNull Supplier<? extends LongEnumerator> sourceSupplier,
            boolean parallel
        ) {
            super(sourceSupplier, parallel);
        }

        Head(@NotNull Supplier<? extends LongEnumerator> sourceSupplier) {
            this(sourceSupplier, false);
        }

        @Override
        protected boolean opIsStateful() {
            throw new UnsupportedOperationException(
                "The source stage does not represent an operation."
            );
        }

        @Override
        protected @NotNull LongEnumerator opWrapEnumerator(
            @NotNull LongEnumerator upstream
        ) {
            throw new UnsupportedOperationException(
                "The source stage has no upstream enumerator."
            );
        }
    }

    abstract static class StatelessOp extends LongEnumPipeline {

        protected StatelessOp(@NotNull LongEnumPipeline upstream) {
            super(upstream);
        }

        @Override
        protected final boolean opIsStateful() {
            return false;
        }
    }

    abstract static class StatefulOp extends LongEnumPipeline {

        protected StatefulOp(@NotNull LongEnumPipeline upstream) {
            super(upstream);
        }

        @Override
        protected final boolean opIsStateful() {
            return true;
        }
    }

    private static final class ReferenceBridgeEnumerator<R>
        implements Enumerator<R> {

        private final LongEnumerator upstream;
        private final LongFunction<? extends R> selector;

        private @Nullable R current;
        private boolean hasCurrent;
        private boolean buffered;
        private boolean finished;
        private boolean closed;

        private ReferenceBridgeEnumerator(
            @NotNull LongEnumerator upstream,
            @NotNull LongFunction<? extends R> selector
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

    private static final class LongToIntBridgeEnumerator
        implements IntEnumerator {

        private final LongEnumerator upstream;
        private final LongToIntFunction selector;
        private int current;
        private boolean hasCurrent;

        private LongToIntBridgeEnumerator(
            LongEnumerator upstream,
            LongToIntFunction selector
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

    private static final class LongToDoubleBridgeEnumerator
        implements DoubleEnumerator {

        private final LongEnumerator upstream;
        private final LongToDoubleFunction selector;
        private double current;
        private boolean hasCurrent;

        private LongToDoubleBridgeEnumerator(
            LongEnumerator upstream,
            LongToDoubleFunction selector
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

            current = selector.applyAsDouble(upstream.current());
            hasCurrent = true;
            return true;
        }

        @Override
        public double current() {
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
 * Lightweight package-private hash set specialized for primitive
 * {@code long} values.
 */
final class LongHashSet {

    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;
    private static final byte DELETED = 2;

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.65f;

    private long[] values;
    private byte[] states;
    private int size;
    private int threshold;

    LongHashSet() {
        this(DEFAULT_CAPACITY);
    }

    LongHashSet(int expectedSize) {
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

        values = new long[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);
    }

    boolean add(long value) {
        if (size + 1 > threshold) {
            resize(values.length << 1);
        }

        int mask = values.length - 1;
        int index = mixToInt(value) & mask;
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

    boolean contains(long value) {
        return findIndex(value) >= 0;
    }

    boolean remove(long value) {
        int index = findIndex(value);

        if (index < 0) {
            return false;
        }

        states[index] = DELETED;
        size--;
        return true;
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    void clear() {
        Arrays.fill(states, EMPTY);
        size = 0;
    }

    private int findIndex(long value) {
        int mask = values.length - 1;
        int index = mixToInt(value) & mask;

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

    private void resize(int newCapacity) {
        int capacity = tableSizeFor(newCapacity);

        long[] oldValues = values;
        byte[] oldStates = states;

        values = new long[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);

        for (int i = 0; i < oldValues.length; i++) {
            if (oldStates[i] == OCCUPIED) {
                addRehashed(oldValues[i]);
            }
        }
    }

    private void addRehashed(long value) {
        int mask = values.length - 1;
        int index = mixToInt(value) & mask;

        while (states[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }

        values[index] = value;
        states[index] = OCCUPIED;
    }

    private static int threshold(int capacity) {
        return Math.max(1, (int) (capacity * LOAD_FACTOR));
    }

    private static int tableSizeFor(int capacity) {
        if (capacity <= 1) {
            return 1;
        }

        int highest = Integer.highestOneBit(capacity - 1);

        if (highest >= (1 << 30)) {
            return 1 << 30;
        }

        return highest << 1;
    }

    private static int mixToInt(long value) {
        long z = value;
        z ^= z >>> 33;
        z *= 0xff51afd7ed558ccdL;
        z ^= z >>> 33;
        z *= 0xc4ceb9fe1a85ec53L;
        z ^= z >>> 33;

        return (int) (z ^ (z >>> 32));
    }
}

