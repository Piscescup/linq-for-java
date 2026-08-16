package io.github.piscescup.linq4j;

import io.github.piscescup.interfaces.exfunction.BinFunction;
import io.github.piscescup.interfaces.exfunction.BinPredicate;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
 * Enumeration begins only when an {@link Enumerator} is obtained and advanced.</p>
 *
 * <p>Unlike a Java {@link java.util.stream.Stream}, a reference enumerable
 * pipeline is reusable. Creating a downstream stage does not consume or
 * invalidate its upstream stage, and multiple query branches may share the
 * same upstream pipeline. Each call that begins enumeration creates an
 * independent enumerator chain.</p>
 *
 * <p>Pipeline stages describe query operations and must not contain mutable
 * state belonging to a particular enumeration. Such state, including element
 * indexes, counters, buffers, sets of previously observed values, and nested
 * enumerators, belongs to the {@link Enumerator} instances created when the
 * pipeline is evaluated.</p>
 *
 * <p>Intermediate stages are represented by {@link StatelessOp} or
 * {@link StatefulOp}. Stateless operations can process their input without
 * requiring information about the sequence as a whole, while stateful
 * operations may require information accumulated from multiple source
 * elements or may buffer elements before producing results.</p>
 *
 * @param <T_IN> the type of elements accepted from the upstream pipeline stage
 * @param <T_OUT> the type of elements produced by this pipeline stage
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
abstract class ReferenceEnumPipeline<T_IN, T_OUT>
    extends AbstractEnumPipeline<T_IN, T_OUT, Enumerable<T_OUT>>
    implements Enumerable<T_OUT> {

    /**
     * Creates the source stage of a reference enumerable pipeline.
     *
     * <p>The supplied factory is retained by the pipeline and must create an
     * independent source enumerator whenever it is invoked.</p>
     *
     * @param sourceSupplier the factory used to create source enumerators
     * @param parallel whether the pipeline is configured for parallel execution
     */
    protected ReferenceEnumPipeline(
        @NotNull Supplier<? extends Enumerator<T_OUT>> sourceSupplier,
        boolean parallel
    ) {
        super(sourceSupplier, parallel);
    }

    /**
     * Creates an intermediate pipeline stage whose input is produced by the
     * specified upstream stage.
     *
     * @param upstream the immediately preceding pipeline stage
     */
    protected ReferenceEnumPipeline(
        @NotNull AbstractEnumPipeline<?, T_IN, ?> upstream
    ) {
        super(upstream);
    }

    /**
     * Returns a sequence containing the elements of this sequence followed by
     * the specified element.
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param element the element to append
     * @return a sequence containing this sequence followed by {@code element}
     */
    @Override
    public final @NotNull Enumerable<T_OUT> append(T_OUT element) {
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return Enumerators.append(upstream, element);
            }
        };
    }

    /**
     * Casts every element of this sequence to the specified type.
     *
     * <p>The cast is performed when an element is enumerated. Consequently,
     * an incompatible element causes {@link ClassCastException} during
     * enumeration rather than when this method is invoked.</p>
     *
     * @param targetType the target element type
     * @param <R> the target element type
     * @return a sequence whose elements are cast to {@code R}
     * @throws NullPointerException if {@code targetType} is {@code null}
     */
    @Override
    public final <R> @NotNull Enumerable<R> cast(
        @NotNull Class<R> targetType
    ) {
        NullCheck.requireNonNull(targetType, "targetType");

        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return Enumerators.cast(upstream, targetType);
            }
        };
    }

    /**
     * Projects each element of this sequence into a new form.
     *
     * <p>The selector is invoked lazily as elements are requested from the
     * resulting sequence.</p>
     *
     * @param selector the transform function to apply to each element
     * @param <R> the type of the resulting elements
     * @return a sequence containing the projected elements
     * @throws NullPointerException if {@code selector} is {@code null}
     */
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
                return Enumerators.select(upstream, selector);
            }
        };
    }

    /**
     * Projects each element and its zero-based index into a new form.
     *
     * <p>The index is local to each enumeration and begins at zero whenever
     * a new enumerator is created.</p>
     *
     * @param selector the transform function receiving an element and its index
     * @param <R> the type of the resulting elements
     * @return a sequence containing the projected elements
     * @throws NullPointerException if {@code selector} is {@code null}
     */
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
                return Enumerators.selectIndexed(upstream, selector);
            }
        };
    }

    /**
     * Projects each source element to an enumerable and flattens the resulting
     * sequences into a single sequence.
     *
     * @param selector the function that produces an inner sequence for each
     *                 source element
     * @param <R> the type of elements in the resulting sequence
     * @return the flattened sequence
     * @throws NullPointerException if {@code selector} is {@code null}
     */
    @Override
    public final <R> @NotNull Enumerable<R> selectMany(
        @NotNull Function<
            ? super T_OUT,
            ? extends Enumerable<? extends R>
            > selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return Enumerators.selectMany(upstream, selector);
            }
        };
    }

    /**
     * Projects each source element and its zero-based index to an enumerable
     * and flattens the resulting sequences into a single sequence.
     *
     * @param selector the function receiving the source element and its index
     * @param <R> the type of elements in the resulting sequence
     * @return the flattened sequence
     * @throws NullPointerException if {@code selector} is {@code null}
     */
    @Override
    public final <R> @NotNull Enumerable<R> selectMany(
        @NotNull BinFunction<
            ? super T_OUT,
            Integer,
            ? extends Enumerable<? extends R>
            > selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        return new StatelessOp<T_OUT, R>(this) {
            @Override
            protected @NotNull Enumerator<R> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return Enumerators.selectManyIndexed(upstream, selector);
            }
        };
    }

    /**
     * Filters this sequence according to the specified predicate.
     *
     * <p>The predicate is evaluated lazily for each source element.</p>
     *
     * @param predicate the function used to test each element
     * @return a sequence containing the elements for which the predicate
     *         returns {@code true}
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
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
                return Enumerators.where(upstream, predicate);
            }
        };
    }

    /**
     * Filters this sequence according to a predicate that receives each
     * element and its zero-based index.
     *
     * @param predicate the function used to test each element and its index
     * @return a sequence containing the elements for which the predicate
     *         returns {@code true}
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
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
                return Enumerators.whereIndexed(upstream, predicate);
            }
        };
    }

    /**
     * Returns at most {@code count} elements from the beginning of this
     * sequence.
     *
     * @param count the maximum number of elements to return
     * @return a sequence containing at most {@code count} source elements
     */
    @Override
    public final @NotNull Enumerable<T_OUT> take(int count) {
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return Enumerators.take(upstream, count);
            }
        };
    }

    /**
     * Returns elements from the beginning of this sequence while the specified
     * predicate returns {@code true}.
     *
     * @param predicate the function used to test each element
     * @return the initial contiguous sequence of matching elements
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
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
                return Enumerators.takeWhile(upstream, predicate);
            }
        };
    }

    /**
     * Returns elements from the beginning of this sequence while the specified
     * index-aware predicate returns {@code true}.
     *
     * @param predicate the function used to test each element and its index
     * @return the initial contiguous sequence of matching elements
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
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
                return Enumerators.takeWhileIndexed(upstream, predicate);
            }
        };
    }

    /**
     * Bypasses the specified number of elements and returns the remaining
     * elements.
     *
     * @param count the number of elements to bypass
     * @return a sequence containing the remaining elements
     */
    @Override
    public final @NotNull Enumerable<T_OUT> skip(int count) {
        return new StatelessOp<T_OUT, T_OUT>(this) {
            @Override
            protected @NotNull Enumerator<T_OUT> opWrapEnumerator(
                @NotNull Enumerator<T_OUT> upstream
            ) {
                return Enumerators.skip(upstream, count);
            }
        };
    }

    /**
     * Bypasses elements while the specified predicate returns {@code true}
     * and then returns all remaining elements.
     *
     * @param predicate the function used to test each element
     * @return the sequence beginning with the first element for which
     *         {@code predicate} returns {@code false}
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
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
                return Enumerators.skipWhile(upstream, predicate);
            }
        };
    }

    /**
     * Bypasses elements while the specified index-aware predicate returns
     * {@code true} and then returns all remaining elements.
     *
     * @param predicate the function used to test each element and its index
     * @return the remaining sequence
     * @throws NullPointerException if {@code predicate} is {@code null}
     */
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
                return Enumerators.skipWhileIndexed(upstream, predicate);
            }
        };
    }

    /**
     * Source stage of a reference enumerable pipeline.
     *
     * <p>A head stage owns the source enumerator factory and does not represent
     * an intermediate query operation.</p>
     *
     * @param <T> the source element type
     */
    static final class Head<T>
        extends ReferenceEnumPipeline<T, T> {

        /**
         * Creates a source pipeline stage.
         *
         * @param sourceSupplier the factory used to create source enumerators
         * @param parallel whether the source is configured for parallel execution
         */
        Head(
            @NotNull Supplier<? extends Enumerator<T>> sourceSupplier,
            boolean parallel
        ) {
            super(sourceSupplier, parallel);
        }

        /**
         * The source stage does not represent an intermediate operation.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        protected boolean opIsStateful() {
            throw new UnsupportedOperationException(
                "The source stage does not represent an operation."
            );
        }

        /**
         * The source stage cannot wrap an upstream enumerator because it has
         * no upstream stage.
         *
         * @throws UnsupportedOperationException always
         */
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
     * Base class for stateless intermediate operations.
     *
     * <p>A stateless operation does not require information accumulated from
     * the sequence as a whole in order to transform the current element.
     * Mutable cursor or counter state belonging to a particular enumeration
     * remains stored in its enumerator.</p>
     *
     * @param <T_IN> the input element type
     * @param <T_OUT> the output element type
     */
    abstract static class StatelessOp<T_IN, T_OUT>
        extends ReferenceEnumPipeline<T_IN, T_OUT> {

        /**
         * Creates a stateless intermediate operation.
         *
         * @param upstream the immediately preceding pipeline stage
         */
        protected StatelessOp(
            @NotNull AbstractEnumPipeline<?, T_IN, ?> upstream
        ) {
            super(upstream);
        }

        /**
         * Indicates that this operation is stateless.
         *
         * @return {@code false}
         */
        @Override
        protected final boolean opIsStateful() {
            return false;
        }
    }

    /**
     * Base class for stateful intermediate operations.
     *
     * <p>A stateful operation may depend on multiple source elements or may
     * require buffering or lookup state before or while producing its output.
     * Mutable execution state must be allocated separately for every
     * enumeration.</p>
     *
     * @param <T_IN> the input element type
     * @param <T_OUT> the output element type
     */
    abstract static class StatefulOp<T_IN, T_OUT>
        extends ReferenceEnumPipeline<T_IN, T_OUT> {

        /**
         * Creates a stateful intermediate operation.
         *
         * @param upstream the immediately preceding pipeline stage
         */
        protected StatefulOp(
            @NotNull AbstractEnumPipeline<?, T_IN, ?> upstream
        ) {
            super(upstream);
        }

        /**
         * Indicates that this operation is stateful.
         *
         * @return {@code true}
         */
        @Override
        protected final boolean opIsStateful() {
            return true;
        }
    }
}