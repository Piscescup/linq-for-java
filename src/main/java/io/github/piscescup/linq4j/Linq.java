package io.github.piscescup.linq4j;


import io.github.piscescup.linq4j.core.*;
import io.github.piscescup.linq4j.enumerator.DoubleRangeEnumerator;
import io.github.piscescup.linq4j.enumerator.Enumerator;
import io.github.piscescup.linq4j.enumerator.LongRangeEnumerator;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * The factory class for the LINQ in java.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class Linq {
    private Linq() {}

    /**
     * <p>Creates an {@link IntEnumerable} from the specified primitive
     * {@code int} values.</p>
     *
     * <p>The returned enumerable traverses the supplied array directly and does
     * not require boxing the primitive values into {@link Integer} objects.</p>
     *
     * <p>The supplied array is not copied. Changes made to the array after this
     * method returns may therefore be visible when the enumerable is traversed.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.of(
     *     1, 2, 3, 4, 5
     * );
     *
     * numbers
     *     .where(value -> value % 2 != 0)
     *     .select(value -> value * 10)
     *     .forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 10
     * // 30
     * // 50
     * }</pre>
     *
     * @param ints The primitive {@code int} values used as the source sequence.
     * @return An {@link IntEnumerable} that enumerates the specified values.
     * @throws NullPointerException If {@code ints} is {@code null}.
     */
    @NotNull
    @Contract("_ -> new")
    public static IntEnumerable ofInts(int... ints) {
        return IntEnumerable.ofInts(ints);
    }


    /**
     * <p>Creates an {@link IntEnumerable} containing a sequence of evenly
     * spaced primitive {@code int} values.</p>
     *
     * <p>The sequence begins with {@code startInclusive} and repeatedly adds
     * {@code step} until {@code endExclusive} is reached. The
     * {@code endExclusive} value is never included in the resulting sequence.</p>
     *
     * <p>A positive {@code step} produces an ascending sequence, while a
     * negative {@code step} produces a descending sequence. If the direction
     * of {@code step} cannot reach the specified end value, an empty enumerable
     * is returned.</p>
     *
     * <p>The values are generated lazily during enumeration and are not stored
     * in an intermediate array.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable ascending = Linq.rangeInts(
     *     1, 10, 2
     * );
     *
     * ascending.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1
     * // 3
     * // 5
     * // 7
     * // 9
     *
     * IntEnumerable descending = Linq.rangeInts(
     *     10, 1, -3
     * );
     *
     * descending.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 10
     * // 7
     * // 4
     * }</pre>
     *
     * @param startInclusive The first value in the sequence.
     * @param endExclusive The exclusive end bound of the sequence.
     * @param step The difference between two consecutive values; must not be
     *             zero.
     * @return A new {@link IntEnumerable} representing the specified range,
     *         or an empty enumerable if the end cannot be reached in the
     *         direction of {@code step}.
     * @throws IllegalArgumentException If {@code step} is zero.
     */
    @NotNull
    @Contract("_ , _, _ -> new")
    public static IntEnumerable rangeInts(int startInclusive, int endExclusive, int step) {
        return IntEnumerable.rangeInts(startInclusive, endExclusive, step);
    }

    /**
     * <p>Creates an {@link IntEnumerable} containing consecutive primitive
     * {@code int} values within the specified range.</p>
     *
     * <p>The sequence begins with {@code startInclusive} and increments by
     * {@code 1} until {@code endExclusive} is reached. The
     * {@code endExclusive} value is not included.</p>
     *
     * <p>If {@code startInclusive} is greater than or equal to
     * {@code endExclusive}, an empty enumerable is returned.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.rangeInts(1, 5);
     *
     * numbers.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1
     * // 2
     * // 3
     * // 4
     * }</pre>
     *
     * @param startInclusive The first value in the sequence.
     * @param endExclusive The exclusive upper bound of the sequence.
     * @return A new {@link IntEnumerable} representing the specified range.
     *
     * @see #rangeInts(int, int, int)
     */
    @NotNull
    @Contract("_, _ -> new")
    public static IntEnumerable rangeInts(int startInclusive, int endExclusive) {
        return rangeInts(startInclusive, endExclusive, 1);
    }

    /**
     * <p>Creates a {@link DoubleEnumerable} from the specified primitive
     * {@code double} values.</p>
     *
     * <p>The returned enumerable traverses the supplied array directly and does
     * not require boxing the primitive values into {@link Double} objects.</p>
     *
     * <p>The supplied array is not copied. Changes made to the array after this
     * method returns may therefore be visible when the enumerable is traversed.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.of(
     *     1.5, 2.0, 3.5, 4.0, 5.5
     * );
     *
     * numbers
     *     .where(value -> value >= 3.0)
     *     .select(value -> value * 2.0)
     *     .forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 7.0
     * // 8.0
     * // 11.0
     * }</pre>
     *
     * @param doubles The primitive {@code double} values used as the source
     *                sequence.
     * @return A {@link DoubleEnumerable} that enumerates the specified values.
     * @throws NullPointerException If {@code doubles} is {@code null}.
     */
    @NotNull
    @Contract("_ -> new")
    public static DoubleEnumerable ofDoubles(double @NotNull ... doubles) {
        return DoubleEnumerable.ofDoubles(doubles);
    }

    /**
     * <p>Creates a {@link DoubleEnumerable} containing a fixed number of
     * evenly spaced primitive {@code double} values.</p>
     *
     * <p>The sequence contains exactly {@code count} values. The value at
     * zero-based index {@code index} is calculated as follows:</p>
     *
     * <pre>{@code
     * start + index * step
     * }</pre>
     *
     * <p>The sequence length is determined by {@code count} rather than an
     * end boundary. This avoids relying on potentially unreliable equality
     * or boundary comparisons between floating-point values.</p>
     *
     * <p>Each value is calculated independently from its index during
     * enumeration, reducing the accumulation of floating-point rounding
     * errors. However, values such as {@code 0.1} may still not be represented
     * exactly because of the limitations of IEEE 754 floating-point
     * arithmetic.</p>
     *
     * <p>The values are generated lazily during enumeration and are not stored
     * in an intermediate array.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable values = Linq.rangeDoubles(
     *     0.0, 5L, 0.25
     * );
     *
     * values.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 0.0
     * // 0.25
     * // 0.5
     * // 0.75
     * // 1.0
     *
     * DoubleEnumerable descending = Linq.rangeDoubles(
     *     1.0, 4L, -0.25
     * );
     *
     * descending.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1.0
     * // 0.75
     * // 0.5
     * // 0.25
     * }</pre>
     *
     * @param start The first value in the sequence.
     * @param count The number of values to generate; must be non-negative.
     * @param step The difference between two consecutive values; must be
     *             finite and non-zero.
     * @return A new {@link DoubleEnumerable} containing {@code count}
     *         evenly spaced values.
     * @throws IllegalArgumentException If {@code start} is not finite,
     *         {@code count} is negative, or {@code step} is zero or not
     *         finite.
     */
    @NotNull
    @Contract("_, _, _ -> new")
    public static DoubleEnumerable rangeDoubles(double start, long count, double step) {
        return DoubleEnumerable.rangeDoubles(start, count, step);
    }


    /**
     * <p>Creates a {@link DoubleEnumerable} containing {@code count}
     * consecutive primitive {@code double} values.</p>
     *
     * <p>The sequence begins with {@code start} and increments by {@code 1.0}
     * for each subsequent value. The value at zero-based index {@code index}
     * is calculated as follows:</p>
     *
     * <pre>{@code
     * start + index
     * }</pre>
     *
     * <p>If {@code count} is zero, an empty enumerable is returned.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.rangeDoubles(
     *     1.5, 4L
     * );
     *
     * numbers.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1.5
     * // 2.5
     * // 3.5
     * // 4.5
     * }</pre>
     *
     * @param start The first value in the sequence.
     * @param count The number of values to generate; must be non-negative.
     * @return A new {@link DoubleEnumerable} containing {@code count}
     *         consecutive values.
     * @throws IllegalArgumentException If {@code start} is not finite or
     *         {@code count} is negative.
     *
     * @see #rangeDoubles(double, long, double)
     */
    @NotNull
    @Contract("_, _ -> new")
    public static DoubleEnumerable rangeDoubles(
        double start,
        long count
    ) {
        return rangeDoubles(start, count, 1.0);
    }

    /**
     * <p>Creates a {@link LongEnumerable} from the specified primitive
     * {@code long} values.</p>
     *
     * <p>The returned enumerable traverses the supplied array directly and does
     * not require boxing the primitive values into {@link Long} objects.</p>
     *
     * <p>The supplied array is not copied. Changes made to the array after this
     * method returns may therefore be visible when the enumerable is traversed.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.of(
     *     1L, 2L, 3L, 4L, 5L
     * );
     *
     * numbers
     *     .where(value -> value % 2 != 0)
     *     .select(value -> value * 10)
     *     .forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 10
     * // 30
     * // 50
     * }</pre>
     *
     * @param longs The primitive {@code long} values used as the source sequence.
     * @return A {@link LongEnumerable} that enumerates the specified values.
     * @throws NullPointerException If {@code longs} is {@code null}.
     */
    @NotNull
    @Contract("_ -> new")
    public static LongEnumerable ofLongs(long @NotNull ... longs) {
        return LongEnumerable.ofLongs(longs);
    }

    /**
     * <p>Creates a {@link LongEnumerable} containing a sequence of evenly
     * spaced primitive {@code long} values.</p>
     *
     * <p>The sequence begins with {@code startInclusive} and repeatedly adds
     * {@code step} until {@code endExclusive} is reached. The
     * {@code endExclusive} value is never included in the resulting sequence.</p>
     *
     * <p>A positive {@code step} produces an ascending sequence, while a
     * negative {@code step} produces a descending sequence. If the direction
     * of {@code step} cannot reach the specified end value, an empty enumerable
     * is returned.</p>
     *
     * <p>The values are generated lazily during enumeration and are not stored
     * in an intermediate array.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable ascending = Linq.rangeLongs(
     *     1L, 10L, 2L
     * );
     *
     * ascending.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1
     * // 3
     * // 5
     * // 7
     * // 9
     *
     * LongEnumerable descending = Linq.rangeLongs(
     *     10L, 1L, -3L
     * );
     *
     * descending.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 10
     * // 7
     * // 4
     * }</pre>
     *
     * @param startInclusive The first value in the sequence.
     * @param endExclusive The exclusive end bound of the sequence.
     * @param step The difference between two consecutive values; must not be
     *             zero.
     * @return A new {@link LongEnumerable} representing the specified range,
     *         or an empty enumerable if the end cannot be reached in the
     *         direction of {@code step}.
     * @throws IllegalArgumentException If {@code step} is zero.
     */
    @NotNull
    @Contract("_, _, _ -> new")
    public static LongEnumerable rangeLongs(long startInclusive, long endExclusive, long step) {
        if (step == 0L) {
            throw new IllegalArgumentException(
                "The step must not be zero."
            );
        }

        return LongEnumerable.rangeLongs(startInclusive, endExclusive, step);
    }

    /**
     * <p>Creates a {@link LongEnumerable} containing consecutive primitive
     * {@code long} values within the specified range.</p>
     *
     * <p>The sequence begins with {@code startInclusive} and increments by
     * {@code 1} until {@code endExclusive} is reached. The
     * {@code endExclusive} value is not included.</p>
     *
     * <p>If {@code startInclusive} is greater than or equal to
     * {@code endExclusive}, an empty enumerable is returned.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.rangeLongs(1L, 5L);
     *
     * numbers.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1
     * // 2
     * // 3
     * // 4
     * }</pre>
     *
     * @param startInclusive The first value in the sequence.
     * @param endExclusive The exclusive upper bound of the sequence.
     * @return A new {@link LongEnumerable} representing the specified range.
     *
     * @see #rangeLongs(long, long, long)
     */
    @NotNull
    @Contract("_, _ -> new")
    public static LongEnumerable rangeLongs(long startInclusive, long endExclusive) {
        return rangeLongs(startInclusive, endExclusive, 1L);
    }

    /**
     * Creates an {@link Enumerable} sequence from the specified elements.
     *
     * <p>The elements are enumerated in the same order in which they are supplied.
     * The returned enumerable does not copy the specified array; enumeration is
     * performed over the supplied array.</p>
     *
     * <p>All elements in {@code elements} must be non-null.</p>
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(1, 2, 3, 4, 5);
     *
     * Enumerable<Integer> result = numbers
     *     .where(n -> n % 2 == 0)
     *     .select(n -> n * 10);
     *
     * // result: 20, 40
     * }</pre>
     *
     * @param elements the elements used to create the enumerable sequence
     * @param <T> the type of the elements in the sequence
     * @return an {@link Enumerable} that enumerates the specified elements
     * @throws NullPointerException if {@code elements} or any element contained
     *                              in {@code elements} is {@code null}
     */
    @SafeVarargs
    @NotNull
    public static <T> Enumerable<T> of(@NotNull T... elements) {
        NullCheck.requireAllNonNull(elements);

        return Enumerable.of(elements);
    }

    /**
     * Creates an {@link Enumerable} sequence from the specified collection.
     *
     * <p>The elements are enumerated according to the iteration order defined by
     * the supplied {@link Collection}.</p>
     *
     * <p>The collection is not copied when this method is called. The returned
     * enumerable creates an enumerator over the supplied collection when the
     * sequence is enumerated. Consequently, changes made to the collection before
     * enumeration may be reflected in the resulting sequence.</p>
     *
     * <p>All elements in {@code elements} must be non-null.</p>
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * List<String> names = List.of("Alice", "Bob", "Charlie");
     *
     * Enumerable<String> enumerable = Linq.of(names);
     *
     * Enumerable<String> result = enumerable
     *     .where(name -> name.length() > 3)
     *     .order();
     *
     * // result: Alice, Charlie
     * }</pre>
     *
     * @param elements the collection whose elements form the enumerable sequence
     * @param <T> the type of the elements in the sequence
     * @return an {@link Enumerable} that enumerates the elements of the specified collection
     * @throws NullPointerException if {@code elements} or any element contained
     *                              in {@code elements} is {@code null}
     */
    @NotNull
    public static <T> Enumerable<T> of(@NotNull Collection<T> elements) {
        NullCheck.requireAllNonNull(elements);

        return Enumerable.of(elements);
    }

    /**
     * Creates an {@link Enumerable} whose elements are obtained from an
     * {@link Iterator} supplied by the specified supplier.
     *
     * <p>The {@code iteratorSupplier} is invoked each time a new enumeration of
     * the returned sequence begins. Therefore, the supplier should normally return
     * a new and independent {@link Iterator} for each invocation.</p>
     *
     * <p>This method is useful for adapting iterator-based APIs to the
     * {@link Enumerable} abstraction.</p>
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * List<Integer> numbers = List.of(1, 2, 3, 4, 5);
     *
     * Enumerable<Integer> enumerable =
     *     Linq.fromIterator(numbers::iterator);
     *
     * int sum = enumerable
     *     .where(n -> n > 2)
     *     .aggregate(0, Integer::sum);
     *
     * // sum == 12
     * }</pre>
     *
     * @param iteratorSupplier a supplier that provides an iterator when enumeration begins
     * @param <T> the type of the elements in the sequence
     * @return an {@link Enumerable} backed by iterators produced by the specified supplier
     * @throws NullPointerException if {@code iteratorSupplier} is {@code null}
     */
    @NotNull
    public static <T> Enumerable<T> fromIterator(
        @NotNull Supplier<? extends Iterator<? extends T>> iteratorSupplier
    ) {
        NullCheck.requireNonNull(iteratorSupplier);

        return Enumerable.fromIterator(iteratorSupplier);
    }

    /**
     * Creates an {@link Enumerable} whose elements are obtained from an
     * {@link Enumerator} supplied by the specified supplier.
     *
     * <p>The {@code enumeratorSupplier} is invoked each time a new enumeration of
     * the returned sequence begins. Therefore, the supplier should normally create
     * a new and independent {@link Enumerator} for each invocation.</p>
     *
     * <p>This method provides the lowest-level factory for integrating a custom
     * {@link Enumerator} implementation directly with the enumerable pipeline.</p>
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * Integer[] numbers = {1, 2, 3, 4, 5};
     *
     * Enumerable<Integer> enumerable = Linq.fromEnumerator(
     *     () -> new ArrayEnumerator<>(numbers)
     * );
     *
     * List<Integer> result = enumerable
     *     .where(n -> n % 2 != 0)
     *     .select(n -> n * n)
     *     .toList();
     *
     * // result: [1, 9, 25]
     * }</pre>
     *
     * @param enumeratorSupplier a supplier that provides an enumerator when enumeration begins
     * @param <T> the type of the elements in the sequence
     * @return an {@link Enumerable} backed by enumerators produced by the specified supplier
     * @throws NullPointerException if {@code enumeratorSupplier} is {@code null}
     */
    @NotNull
    public static <T> Enumerable<T> fromEnumerator(
        @NotNull Supplier<? extends Enumerator<T>> enumeratorSupplier
    ) {
        NullCheck.requireNonNull(enumeratorSupplier);

        return Enumerable.fromEnumerator(enumeratorSupplier);
    }
}
