package io.github.piscescup.linq4j;


import io.github.piscescup.linq4j.primitive.*;
import io.github.piscescup.util.validation.NullCheck;
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
    public static IntEnumerable ofInts(int... ints) {
        return IntEnumerable.of(ints);
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
    public static DoubleEnumerable ofDoubles(double @NotNull ... doubles) {
        return DoubleEnumerable.of(doubles);
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
    public static LongEnumerable ofLongs(long @NotNull ... longs) {
        return LongEnumerable.of(longs);
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

        return new ReferenceEnumPipeline.Head<>(() -> new ArrayEnumerator<>(elements));
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

        return new ReferenceEnumPipeline.Head<>(() -> new CollectionEnumerator<>(elements));
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

        return new ReferenceEnumPipeline.Head<>(
            () -> new IteratorEnumerator<>(iteratorSupplier.get())
        );
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

        return new ReferenceEnumPipeline.Head<>(enumeratorSupplier);
    }
}
