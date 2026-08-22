package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.enumerator.LongArrayEnumerator;
import io.github.piscescup.linq4j.enumerator.LongEnumerator;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.function.LongBinaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;

/**
 * Represents a sequence of primitive {@code long} values that supports
 * LINQ-style query and aggregation operations without requiring each element
 * to be boxed as a {@link Long}.
 *
 * <p>{@code LongEnumerable} is the primitive {@code long} specialization of
 * {@link Enumerable}. It provides query operations for filtering, projection,
 * aggregation, slicing, set operations, and materialization while preserving
 * primitive {@code long} values throughout a long-integer pipeline whenever
 * possible.</p>
 *
 * <p>Queries are typically expressed by chaining operations together. For
 * example, the following query filters even values, squares them, and computes
 * their sum:</p>
 *
 * <pre>{@code
 * LongEnumerable numbers = Linq.ofLongs(
 *     1L, 2L, 3L, 4L, 5L, 6L
 * );
 *
 * long sum = numbers
 *     .where(number -> number % 2L == 0L)
 *     .select(number -> number * number)
 *     .sum();
 *
 * // 2^2 + 4^2 + 6^2 = 56
 * // sum: 56
 * }</pre>
 *
 * <h2>Primitive specialization</h2>
 *
 * <p>{@code LongEnumerable} operates directly on primitive {@code long}
 * values. In contrast, representing a long sequence as
 * {@code Enumerable<Long>} requires primitive values to be represented as
 * {@link Long} objects. Operations whose input and output remain within a
 * {@code LongEnumerable} therefore avoid the per-element boxing and unboxing
 * that would otherwise be required by a reference-type pipeline.</p>
 *
 * <p>For example, the following pipeline can process its elements entirely as
 * primitive {@code long} values:</p>
 *
 * <pre>{@code
 * LongEnumerable result = Linq.ofLongs(1L, 2L, 3L, 4L, 5L)
 *     .where(value -> value > 2L)
 *     .select(value -> value * 10L);
 *
 * long[] values = result.toArray();
 *
 * // values: [30, 40, 50]
 * }</pre>
 *
 * <p>Operations that explicitly cross from a primitive pipeline into a
 * reference-type pipeline, such as {@link #boxed()} or
 * {@link #selectToObj(LongFunction)}, necessarily produce reference values.
 * Such conversions are explicit so that boxing or object creation is visible
 * at the API boundary.</p>
 *
 * <h2>Deferred execution</h2>
 *
 * <p>Most operations that return another {@code LongEnumerable} use
 * <em>deferred execution</em>. Calling an operation such as
 * {@link #where(LongPredicate)}, {@link #select(LongUnaryOperator)},
 * {@link #skip(int)}, or {@link #take(int)} constructs a new query stage but
 * does not immediately enumerate the source sequence.</p>
 *
 * <p>The query is evaluated when the resulting enumerable is traversed through
 * {@link #enumerator()}, {@link #forEach(java.util.function.LongConsumer)}, or
 * when a terminal operation such as {@link #sum()}, {@link #count()},
 * {@link #first()}, or {@link #toArray()} is invoked.</p>
 *
 * <pre>{@code
 * LongEnumerable numbers =
 *     Linq.ofLongs(1L, 2L, 3L, 4L, 5L, 6L);
 *
 * // Defines the query. The source is not enumerated here.
 * LongEnumerable query = numbers
 *     .where(number -> number % 2L == 0L)
 *     .select(number -> number * number);
 *
 * // Enumerates the source and evaluates the query.
 * long[] result = query.toArray();
 *
 * // result: [4, 16, 36]
 * }</pre>
 *
 * <h2>Enumeration</h2>
 *
 * <p>A {@code LongEnumerable} represents a sequence rather than a single-use
 * traversal. Each call to {@link #enumerator()} creates an independent
 * {@link LongEnumerator} for traversing the query.</p>
 *
 * <p>Traversal-specific state, such as indexes, buffers, counters, and other
 * operation state, belongs to the individual enumeration rather than to the
 * query description itself. Consequently, the same enumerable query can
 * normally be enumerated multiple times.</p>
 *
 * <p>Unlike the reference-type {@link Enumerable}, a
 * {@code LongEnumerable} does not expose its primitive traversal through
 * {@code Iterable<Long>}. Doing so would require every primitive value to be
 * converted to a {@link Long}. Reference-based interoperability is instead
 * provided explicitly through {@link #boxed()}.</p>
 *
 * <h2>Stateless and stateful operations</h2>
 *
 * <p>Some operations can produce output directly as elements are requested
 * from the upstream sequence. Operations such as {@code where},
 * {@code select}, {@code skip}, and {@code take} are typical stateless
 * operations.</p>
 *
 * <p>Other operations may require state associated with the current
 * enumeration. For example, reversing, shuffling, distinct-value processing,
 * and set operations may need to buffer elements or maintain auxiliary
 * collections. Such state should be created independently for each
 * enumeration.</p>
 *
 * <h2>Aggregation</h2>
 *
 * <p>Aggregation operations consume the primitive sequence and produce a
 * single result. {@link #aggregateToResult(long, LongBinaryOperator)} is the
 * general-purpose primitive {@code long} aggregation operation.</p>
 *
 * <pre>{@code
 * LongEnumerable numbers =
 *     Linq.ofLongs(1L, 2L, 3L, 4L, 5L);
 *
 * long sum = numbers.aggregateToResult(
 *     0L,
 *     Long::sum
 * );
 *
 * // sum: 15
 * }</pre>
 *
 * <p>Common numeric aggregations are also provided directly:</p>
 *
 * <pre>{@code
 * LongEnumerable numbers =
 *     Linq.ofLongs(4L, 8L, 15L, 16L, 23L, 42L);
 *
 * long sum = numbers.sum();
 * long min = numbers.min();
 * long max = numbers.max();
 * double average = numbers.average();
 * long count = numbers.count();
 * }</pre>
 *
 * <h2>Filtering with {@code where}</h2>
 *
 * <p>{@link #where(LongPredicate)} filters the primitive sequence according
 * to a specified predicate. Elements are requested from the upstream sequence
 * until an element satisfying the predicate is found.</p>
 *
 * <pre>{@code
 * LongEnumerable numbers =
 *     Linq.ofLongs(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
 *
 * LongEnumerable evenNumbers = numbers.where(
 *     number -> number % 2L == 0L
 * );
 *
 * long[] result = evenNumbers.toArray();
 *
 * // result: [2, 4, 6, 8]
 * }</pre>
 *
 * <h2>Projection</h2>
 *
 * <p>{@link #select(LongUnaryOperator)} transforms each {@code long} element
 * into another {@code long} value while preserving the primitive pipeline.</p>
 *
 * <pre>{@code
 * LongEnumerable numbers =
 *     Linq.ofLongs(1L, 2L, 3L, 4L);
 *
 * LongEnumerable squares = numbers.select(
 *     number -> number * number
 * );
 *
 * // squares: 1, 4, 9, 16
 * }</pre>
 *
 * <p>Projection may also change the shape of the pipeline. The
 * {@link #selectToInt(LongToIntFunction)} and
 * {@link #selectToDouble(LongToDoubleFunction)} operations produce another
 * primitive-specialized enumerable, while
 * {@link #selectToObj(LongFunction)} produces a reference-type
 * {@link Enumerable}.</p>
 *
 * <h2>Query composition</h2>
 *
 * <p>Primitive operations are designed to be composed in the same manner as
 * the operations of {@link Enumerable}. A query can remain primitive through
 * multiple stages and cross into another primitive or reference pipeline only
 * when an explicit projection requires it.</p>
 *
 * <pre>{@code
 * double result = Linq.ofLongs(1L, 2L, 3L, 4L, 5L, 6L)
 *     .where(value -> value % 2L == 0L)
 *     .select(value -> value * value)
 *     .selectToDouble(value -> value / 2.0)
 *     .average();
 * }</pre>
 *
 * <h2>Sequential and parallel evaluation</h2>
 *
 * <p>A {@code LongEnumerable} also carries an execution mode.
 * {@link #sequential()} and {@link #parallel()} can be used to request
 * sequential or parallel evaluation, and {@link #isParallel()} reports the
 * current mode. The execution mode is propagated through the enumerable
 * pipeline.</p>
 *
 * <p>Unless an operation explicitly documents otherwise, functions supplied
 * to query operators should not modify the enumerable source while it is being
 * enumerated. Query functions should preferably be deterministic and free of
 * externally observable side effects.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 *
 * @see Enumerable
 * @see LongEnumerator
 * @see InternalLongEnumerable
 * @see BaseEnumerable
 */
public interface LongEnumerable
    extends BaseEnumerable<Long, LongEnumerable>, InternalLongEnumerable {

    /**
     * <p>Applies an accumulator function over this sequence of primitive
     * {@code long} values. The specified seed value is used as the initial
     * accumulator value.</p>
     *
     * <p>The accumulator and sequence elements are represented as primitive
     * {@code long} values throughout the operation.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 2L, 3L, 4L, 5L);
     *
     * long sum = numbers.aggregateToResult(
     *     0L,
     *     Long::sum
     * );
     *
     * System.out.println(sum);
     *
     * // This code produces the following output:
     * //
     * // 15
     * }</pre>
     *
     * @param seed The initial accumulator value.
     * @param aggregator An accumulator function to be invoked on each element.
     * @return The final accumulated value.
     * @throws NullPointerException If {@code aggregator} is {@code null}.
     */
    long aggregateToResult(
        long seed,
        @NotNull LongBinaryOperator aggregator
    );

    /**
     * <p>Determines whether all elements of this sequence satisfy a specified
     * condition.</p>
     *
     * <p>Enumeration stops as soon as the result can be determined.
     * If the sequence is empty, {@code true} is returned.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(2L, 4L, 6L, 8L);
     *
     * boolean allEven = numbers.all(
     *     value -> value % 2L == 0L
     * );
     *
     * System.out.println(allEven);
     *
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return {@code true} if every element satisfies {@code predicate},
     *         or if the sequence is empty; otherwise, {@code false}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    boolean all(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Determines whether this sequence contains any elements.</p>
     *
     * <p>Enumeration stops as soon as the result can be determined.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 2L, 3L);
     *
     * boolean hasElements = numbers.any();
     *
     * // hasElements: true
     * }</pre>
     *
     * @return {@code true} if the sequence contains at least one element;
     *         otherwise, {@code false}.
     */
    boolean any();

    /**
     * <p>Determines whether any element of this sequence satisfies a specified
     * condition.</p>
     *
     * <p>Enumeration stops as soon as an element satisfying
     * {@code predicate} is found.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 3L, 5L, 8L);
     *
     * boolean containsEven = numbers.any(
     *     value -> value % 2L == 0L
     * );
     *
     * // containsEven: true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return {@code true} if at least one element satisfies
     *         {@code predicate}; otherwise, {@code false}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    boolean any(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Determines whether this sequence contains the specified primitive
     * {@code long} value.</p>
     *
     * <p>The search stops as soon as a matching value is encountered.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 2L, 3L, 4L);
     *
     * boolean contains =
     *     numbers.contains(3L);
     *
     * // contains: true
     * }</pre>
     *
     * @param value The value to locate in the sequence.
     * @return {@code true} if the sequence contains {@code value};
     *         otherwise, {@code false}.
     */
    boolean contains(long value);

    /**
     * <p>Appends a specified primitive {@code long} value to the end of this
     * sequence.</p>
     *
     * <p>This method does not modify the current sequence. Instead, it returns
     * a new sequence containing all elements of this sequence followed by the
     * specified value.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 2L, 3L);
     *
     * LongEnumerable result =
     *     numbers.append(4L);
     *
     * // result: 1, 2, 3, 4
     * }</pre>
     *
     * @param element The element to append to the sequence.
     * @return A new enumerable containing all elements of this sequence
     *         followed by {@code element}.
     */
    @NotNull
    LongEnumerable append(long element);

    /**
     * <p>Adds a specified primitive {@code long} value to the beginning of
     * this sequence.</p>
     *
     * <p>This method does not modify the current sequence. Instead, it returns
     * a new sequence containing the specified value followed by all elements
     * of this sequence.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(2L, 3L, 4L);
     *
     * LongEnumerable result =
     *     numbers.prepend(1L);
     *
     * // result: 1, 2, 3, 4
     * }</pre>
     *
     * @param element The element to prepend to the sequence.
     * @return A new enumerable containing {@code element} followed by all
     *         elements of this sequence.
     */
    @NotNull
    LongEnumerable prepend(long element);

    /**
     * <p>Concatenates this sequence with another primitive {@code long}
     * sequence.</p>
     *
     * <p>The resulting sequence contains all elements of this sequence
     * followed by all elements of {@code after}. The encounter order of both
     * sequences is preserved.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable first =
     *     Linq.ofLongs(1L, 2L, 3L);
     *
     * LongEnumerable second =
     *     Linq.ofLongs(4L, 5L, 6L);
     *
     * LongEnumerable result =
     *     first.concat(second);
     *
     * // result: 1, 2, 3, 4, 5, 6
     * }</pre>
     *
     * @param after The sequence to concatenate to this sequence.
     * @return A {@code LongEnumerable} containing the elements of this
     *         sequence followed by the elements of {@code after}.
     * @throws NullPointerException If {@code after} is {@code null}.
     */
    @NotNull
    LongEnumerable concat(
        @NotNull LongEnumerable after
    );

    /**
     * <p>Returns the elements of this sequence, or a singleton sequence
     * containing the specified default value if this sequence is empty.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs();
     *
     * LongEnumerable result =
     *     numbers.defaultIfEmpty(-1L);
     *
     * // result: -1
     * }</pre>
     *
     * @param defaultValue The value returned when this sequence is empty.
     * @return An enumerable containing the elements of this sequence, or a
     *         singleton sequence containing {@code defaultValue} if the
     *         sequence is empty.
     */
    @NotNull
    LongEnumerable defaultIfEmpty(long defaultValue);

    /**
     * <p>Returns the primitive {@code long} value at the specified zero-based
     * index in this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(10L, 20L, 30L, 40L);
     *
     * long value =
     *     numbers.elementAt(2);
     *
     * // value: 30
     * }</pre>
     *
     * @param index The zero-based index of the element to retrieve.
     * @return The element at the specified position.
     * @throws IndexOutOfBoundsException If {@code index} is negative or
     *         greater than or equal to the number of elements.
     */
    long elementAt(int index);

    /**
     * <p>Returns the primitive {@code long} value at the specified zero-based
     * index as an {@link OptionalLong}, or an empty {@code OptionalLong} if the
     * index is outside the bounds of this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(10L, 20L, 30L);
     *
     * OptionalLong result =
     *     numbers.elementAtOrEmpty(5);
     *
     * // result.isEmpty(): true
     * }</pre>
     *
     * @param index The zero-based index of the element to retrieve.
     * @return An {@link OptionalLong} containing the element at {@code index},
     *         or an empty {@code OptionalLong} if the index is out of range.
     */
    @NotNull
    OptionalLong elementAtOrEmpty(int index);

    /**
     * <p>Returns the primitive {@code long} value at the specified zero-based
     * index, or the specified default value if the index is outside the bounds
     * of this sequence.</p>
     *
     * @param index The zero-based index of the element to retrieve.
     * @param defaultValue The value returned if {@code index} is out of range.
     * @return The element at {@code index}, or {@code defaultValue} if the
     *         specified index is outside the bounds of the sequence.
     */
    long elementAtOrDefault(
        int index,
        long defaultValue
    );

    /**
     * <p>Returns the first primitive {@code long} value in this sequence.</p>
     *
     * @return The first element in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    long first();

    /**
     * <p>Returns the first primitive {@code long} value in this sequence that
     * satisfies the specified condition.</p>
     *
     * <p>The search stops as soon as a matching element is found.</p>
     *
     * @param predicate A function to test each element for a condition.
     * @return The first element that satisfies {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws NoSuchElementException If no element satisfies
     *         {@code predicate}.
     */
    long first(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Returns the first primitive {@code long} value in this sequence as an
     * {@link OptionalLong}, or an empty {@code OptionalLong} if the sequence
     * contains no elements.</p>
     *
     * @return An {@link OptionalLong} containing the first element, or an empty
     *         {@code OptionalLong}.
     */
    @NotNull
    OptionalLong firstOrEmpty();

    /**
     * <p>Returns the first primitive {@code long} value satisfying the
     * specified condition as an {@link OptionalLong}, or an empty
     * {@code OptionalLong} if no matching element exists.</p>
     *
     * @param predicate A function to test each element for a condition.
     * @return An {@link OptionalLong} containing the first matching element,
     *         or an empty {@code OptionalLong}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    OptionalLong firstOrEmpty(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Returns the number of elements in this sequence.</p>
     *
     * @return The number of elements in this sequence.
     */
    long count();

    /**
     * <p>Returns the number of elements in this sequence that satisfy the
     * specified condition.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 2L, 3L, 4L, 5L);
     *
     * long count =
     *     numbers.count(value -> value > 2L);
     *
     * // count: 3
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The number of elements that satisfy {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    long count(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Computes the sum of the primitive {@code long} values in this
     * sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 2L, 3L, 4L, 5L);
     *
     * long sum = numbers.sum();
     *
     * // sum: 15
     * }</pre>
     *
     * @return The sum of the elements in this sequence, or {@code 0L} if the
     *         sequence contains no elements.
     * @throws ArithmeticException If the sum exceeds the range of a
     *         primitive {@code long}.
     */
    long sum();

    /**
     * <p>Returns the minimum primitive {@code long} value in this sequence.</p>
     *
     * @return The minimum value in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    long min();

    /**
     * <p>Returns the maximum primitive {@code long} value in this sequence.</p>
     *
     * @return The maximum value in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    long max();

    /**
     * <p>Computes the average of the primitive {@code long} values in this
     * sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(2L, 4L, 6L, 8L);
     *
     * double average = numbers.average();
     *
     * // average: 5.0
     * }</pre>
     *
     * @return The arithmetic mean of the values in this sequence.
     * @throws ArithmeticException If the sequence contains no elements.
     */
    double average();

    /**
     * <p>Returns an enumerable containing only the elements of this sequence
     * that satisfy the specified condition.</p>
     *
     * <p>This operation uses deferred execution and processes values directly
     * as primitive {@code long}s.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 2L, 3L, 4L, 5L, 6L);
     *
     * LongEnumerable evenNumbers =
     *     numbers.where(value -> value % 2L == 0L);
     *
     * // evenNumbers: 2, 4, 6
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return A {@code LongEnumerable} containing the elements that satisfy
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    LongEnumerable where(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Projects each primitive {@code long} element of this sequence into
     * another primitive {@code long} value.</p>
     *
     * <p>This operation preserves the primitive long pipeline and therefore
     * does not require the projected values to be boxed as {@link Long}
     * objects.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 2L, 3L, 4L);
     *
     * LongEnumerable squares =
     *     numbers.select(number -> number * number);
     *
     * // squares: 1, 4, 9, 16
     * }</pre>
     *
     * @param selector A transform function to apply to each element.
     * @return A {@code LongEnumerable} whose elements are the result of
     *         invoking {@code selector} on each element of this sequence.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    LongEnumerable select(
        @NotNull LongUnaryOperator selector
    );

    /**
     * <p>Projects each primitive {@code long} element of this sequence into a
     * primitive {@code int} value.</p>
     *
     * <p>The returned sequence is a primitive-specialized
     * {@link IntEnumerable}, so the projected values remain unboxed.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable values =
     *     Linq.ofLongs(10L, 20L, 30L)
     *         .selectToInt(value -> (int) value);
     *
     * // values: 10, 20, 30
     * }</pre>
     *
     * @param selector A transform function to apply to each element.
     * @return An {@link IntEnumerable} containing the projected primitive
     *         {@code int} values.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    IntEnumerable selectToInt(
        @NotNull LongToIntFunction selector
    );

    /**
     * <p>Projects each primitive {@code long} element of this sequence into a
     * primitive {@code double} value.</p>
     *
     * <p>The returned sequence is a primitive-specialized
     * {@link DoubleEnumerable}, so the projected values remain unboxed.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable halves =
     *     Linq.ofLongs(1L, 2L, 3L, 4L)
     *         .selectToDouble(value -> value / 2.0);
     *
     * // halves: 0.5, 1.0, 1.5, 2.0
     * }</pre>
     *
     * @param selector A transform function to apply to each element.
     * @return A {@link DoubleEnumerable} containing the projected primitive
     *         {@code double} values.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    DoubleEnumerable selectToDouble(
        @NotNull LongToDoubleFunction selector
    );

    /**
     * <p>Projects each primitive {@code long} element of this sequence into an
     * object and returns the resulting values as a reference-type
     * {@link Enumerable}.</p>
     *
     * <p>This operation represents an explicit transition from a primitive
     * long pipeline to a reference-type pipeline.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> labels =
     *     Linq.ofLongs(10L, 20L, 30L)
     *         .selectToObj(value -> "Value: " + value);
     *
     * // labels: ["Value: 10", "Value: 20", "Value: 30"]
     * }</pre>
     *
     * @param selector A transform function to apply to each primitive element.
     * @param <R> The type of the resulting elements.
     * @return An {@link Enumerable} whose elements are the result of invoking
     *         {@code selector} on each element of this sequence.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    <R> @NotNull Enumerable<R> selectToObj(
        @NotNull LongFunction<? extends R> selector
    );

    /**
     * <p>Bypasses a specified number of elements in this sequence and returns
     * the remaining elements.</p>
     *
     * <p>If {@code count} is greater than the number of elements in the
     * sequence, an empty enumerable is returned. A non-positive value skips no
     * elements.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param count The number of elements to skip.
     * @return A {@code LongEnumerable} containing the elements occurring
     *         after the specified number of elements.
     */
    @NotNull
    LongEnumerable skip(int count);

    /**
     * <p>Returns a specified number of contiguous elements from the beginning
     * of this sequence.</p>
     *
     * <p>If {@code count} is greater than the number of elements in the
     * sequence, all elements are returned. A non-positive value produces an
     * empty sequence.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param count The number of elements to return.
     * @return A {@code LongEnumerable} containing at most the specified
     *         number of elements.
     */
    @NotNull
    LongEnumerable take(int count);

    /**
     * <p>Returns a sequence containing all elements of this sequence except
     * for the last specified number of elements.</p>
     *
     * <p>If {@code count} is greater than or equal to the number of elements,
     * an empty sequence is produced. A non-positive value skips no elements.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param count The number of elements to omit from the end of the sequence.
     * @return An enumerable containing all elements except the final
     *         {@code count} elements.
     */
    @NotNull
    LongEnumerable skipLast(int count);

    /**
     * <p>Returns the specified number of contiguous elements from the end of
     * this sequence.</p>
     *
     * <p>If {@code count} is greater than or equal to the number of elements,
     * the entire sequence is returned. A non-positive value produces an empty
     * sequence.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param count The number of elements to return from the end.
     * @return An enumerable containing at most the final {@code count}
     *         elements.
     */
    @NotNull
    LongEnumerable takeLast(int count);

    /**
     * <p>Bypasses elements in this sequence as long as the specified condition
     * is satisfied and then returns the remaining elements.</p>
     *
     * <p>After the predicate first returns {@code false}, no further elements
     * are tested by the predicate.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param predicate A function to test each element for a condition.
     * @return An enumerable beginning with the first element that does not
     *         satisfy {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    LongEnumerable skipWhile(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Returns elements from the beginning of this sequence as long as the
     * specified condition is satisfied.</p>
     *
     * <p>Enumeration stops when the predicate first returns {@code false}.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param predicate A function to test each element for a condition.
     * @return An enumerable containing the elements occurring before the first
     *         element that does not satisfy {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    LongEnumerable takeWhile(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Returns the elements of this sequence in reverse order.</p>
     *
     * <p>The source sequence is buffered when the returned enumerable is
     * traversed. Primitive values are stored directly without boxing.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @return A {@code LongEnumerable} containing the elements of this
     *         sequence in reverse order.
     */
    @NotNull
    LongEnumerable reverse();

    /**
     * <p>Returns the elements of this sequence in randomized order.</p>
     *
     * <p>The source sequence is buffered before the elements are shuffled.
     * Primitive values are stored directly without boxing.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @return A {@code LongEnumerable} containing the elements of this
     *         sequence in randomized order.
     */
    @NotNull
    LongEnumerable shuffle();

    /**
     * <p>Determines whether this sequence and another primitive
     * {@code long} sequence contain equal values in the same order.</p>
     *
     * <p>Enumeration stops as soon as a difference is detected. Two empty
     * sequences are considered equal.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable first =
     *     Linq.ofLongs(1L, 2L, 3L);
     *
     * LongEnumerable second =
     *     Linq.ofLongs(1L, 2L, 3L);
     *
     * boolean equal =
     *     first.sequenceEqual(second);
     *
     * // equal: true
     * }</pre>
     *
     * @param other The sequence to compare with this sequence.
     * @return {@code true} if both sequences contain the same values in the
     *         same order; otherwise, {@code false}.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    boolean sequenceEqual(
        @NotNull LongEnumerable other
    );

    /**
     * <p>Returns distinct primitive {@code long} values from this sequence.</p>
     *
     * <p>Only the first occurrence of each value is returned, and the
     * encounter order of those first occurrences is preserved.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @return A {@code LongEnumerable} containing the distinct values from
     *         this sequence.
     */
    @NotNull
    LongEnumerable distinct();

    /**
     * <p>Produces the set difference of this sequence and another primitive
     * {@code long} sequence.</p>
     *
     * <p>The returned sequence contains distinct values from this sequence
     * that do not occur in {@code other}. The order of the first occurrence
     * of each returned value is preserved.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param other The sequence whose values are excluded from this sequence.
     * @return An enumerable containing the set difference of this sequence and
     *         {@code other}.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    @NotNull
    LongEnumerable except(
        @NotNull LongEnumerable other
    );

    /**
     * <p>Produces the set intersection of this sequence and another primitive
     * {@code long} sequence.</p>
     *
     * <p>The returned sequence contains each value at most once. Values are
     * produced according to their first occurrence in this sequence.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param other The sequence whose values are compared with this sequence.
     * @return An enumerable containing the distinct values that occur in both
     *         sequences.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    @NotNull
    LongEnumerable intersect(
        @NotNull LongEnumerable other
    );

    /**
     * <p>Produces the set union of this sequence and another primitive
     * {@code long} sequence.</p>
     *
     * <p>The resulting sequence contains each value at most once. Values from
     * this sequence are returned first, followed by previously unseen values
     * from {@code other}.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param other The sequence whose values are combined with this sequence.
     * @return An enumerable containing the distinct values from both
     *         sequences.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    @NotNull
    LongEnumerable union(
        @NotNull LongEnumerable other
    );

    // ---------------------------------------------------------------------
// Last
// ---------------------------------------------------------------------

    /**
     * <p>Returns the last primitive {@code long} value in this sequence.</p>
     *
     * <p>The sequence is enumerated until its end in order to determine the last
     * element.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs(
     *     10L, 20L, 30L, 40L
     * );
     *
     * long last = numbers.last();
     *
     * System.out.println(last);
     *
     * // This code produces the following output:
     * //
     * // 40
     * }</pre>
     *
     * @return The last element in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    long last();

    /**
     * <p>Returns the last primitive {@code long} value in this sequence that
     * satisfies the specified condition.</p>
     *
     * <p>The entire sequence is enumerated because a later element may also
     * satisfy {@code predicate}.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs(
     *     1L, 2L, 3L, 4L, 5L, 6L, 7L
     * );
     *
     * long lastEven = numbers.last(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(lastEven);
     *
     * // This code produces the following output:
     * //
     * // 6
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The last element that satisfies {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws NoSuchElementException If no element satisfies
     *         {@code predicate}.
     */
    long last(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Returns the last primitive {@code long} value in this sequence as an
     * {@link OptionalLong}, or an empty {@code OptionalLong} if the sequence
     * contains no elements.</p>
     *
     * <p>The sequence is enumerated until its end in order to determine the last
     * element.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs();
     *
     * OptionalLong last = numbers.lastOrEmpty();
     *
     * System.out.println(last.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @return An {@link OptionalLong} containing the last element, or an empty
     *         {@code OptionalLong} if the sequence contains no elements.
     */
    @NotNull
    OptionalLong lastOrEmpty();

    /**
     * <p>Returns the last primitive {@code long} value that satisfies the
     * specified condition as an {@link OptionalLong}, or an empty
     * {@code OptionalLong} if no matching element exists.</p>
     *
     * <p>The entire sequence is enumerated because a later element may also
     * satisfy {@code predicate}.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs(
     *     1L, 3L, 5L, 7L
     * );
     *
     * OptionalLong lastEven = numbers.lastOrEmpty(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(lastEven.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An {@link OptionalLong} containing the last matching element,
     *         or an empty {@code OptionalLong} if no element satisfies
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    OptionalLong lastOrEmpty(
        @NotNull LongPredicate predicate
    );


    /**
     * <p>Returns the only primitive {@code long} value in this sequence.</p>
     *
     * <p>This operation succeeds only when the sequence contains exactly one
     * element. An empty sequence and a sequence containing multiple elements are
     * considered invalid for this operation.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs(42L);
     *
     * long value = numbers.single();
     *
     * System.out.println(value);
     *
     * // This code produces the following output:
     * //
     * // 42
     * }</pre>
     *
     * @return The single element in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     * @throws IllegalStateException If the sequence contains more than one
     *         element.
     */
    long single();

    /**
     * <p>Returns the only primitive {@code long} value in this sequence that
     * satisfies the specified condition.</p>
     *
     * <p>This operation succeeds only when exactly one element satisfies
     * {@code predicate}. If no element satisfies the condition, or if more than
     * one element satisfies it, the operation fails.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs(
     *     1L, 3L, 4L, 5L, 7L
     * );
     *
     * long even = numbers.single(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(even);
     *
     * // This code produces the following output:
     * //
     * // 4
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The single element that satisfies {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws NoSuchElementException If no element satisfies
     *         {@code predicate}.
     * @throws IllegalStateException If more than one element satisfies
     *         {@code predicate}.
     */
    long single(
        @NotNull LongPredicate predicate
    );

    /**
     * <p>Returns the only primitive {@code long} value in this sequence as an
     * {@link OptionalLong}, or an empty {@code OptionalLong} if the sequence
     * contains no elements.</p>
     *
     * <p>If the sequence contains more than one element, this operation throws an
     * exception rather than selecting one of the elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs();
     *
     * OptionalLong value = numbers.singleOrEmpty();
     *
     * System.out.println(value.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @return An {@link OptionalLong} containing the single element, or an empty
     *         {@code OptionalLong} if the sequence contains no elements.
     * @throws IllegalStateException If the sequence contains more than one
     *         element.
     */
    @NotNull
    OptionalLong singleOrEmpty();

    /**
     * <p>Returns the only primitive {@code long} value that satisfies the
     * specified condition as an {@link OptionalLong}.</p>
     *
     * <p>If no element satisfies {@code predicate}, an empty
     * {@code OptionalLong} is returned. If more than one element satisfies the
     * predicate, this operation throws an exception.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs(
     *     1L, 3L, 4L, 5L, 7L
     * );
     *
     * OptionalLong even = numbers.singleOrEmpty(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(even.getAsLong());
     *
     * // This code produces the following output:
     * //
     * // 4
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An {@link OptionalLong} containing the single matching element,
     *         or an empty {@code OptionalLong} if no element satisfies
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws IllegalStateException If more than one element satisfies
     *         {@code predicate}.
     */
    @NotNull
    OptionalLong singleOrEmpty(
        @NotNull LongPredicate predicate
    );


    /**
     * <p>Returns the elements of this sequence ordered in ascending numerical
     * order.</p>
     *
     * <p>The source sequence is buffered using primitive {@code long} storage
     * when the returned enumerable is traversed. No {@link Long} objects are
     * required for sorting.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs(
     *     5L, 2L, 8L, 1L, 4L
     * );
     *
     * LongEnumerable ordered = numbers.order();
     *
     * ordered.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1
     * // 2
     * // 4
     * // 5
     * // 8
     * }</pre>
     *
     * @return A {@code LongEnumerable} containing the elements of this sequence
     *         in ascending numerical order.
     */
    @NotNull
    LongEnumerable order();

    /**
     * <p>Returns the elements of this sequence ordered in descending numerical
     * order.</p>
     *
     * <p>The source sequence is buffered using primitive {@code long} storage
     * when the returned enumerable is traversed. No {@link Long} objects are
     * required for sorting.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers = Linq.ofLongs(
     *     5L, 2L, 8L, 1L, 4L
     * );
     *
     * LongEnumerable ordered =
     *     numbers.orderDescending();
     *
     * ordered.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 8
     * // 5
     * // 4
     * // 2
     * // 1
     * }</pre>
     *
     * @return A {@code LongEnumerable} containing the elements of this sequence
     *         in descending numerical order.
     */
    @NotNull
    LongEnumerable orderDescending();

    /**
     * <p>Creates an array containing all primitive {@code long} values in this
     * sequence.</p>
     *
     * <p>This is a terminal operation and causes the sequence to be
     * enumerated.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * long[] values = Linq.ofLongs(1L, 2L, 3L, 4L)
     *     .where(value -> value % 2L == 0L)
     *     .toArray();
     *
     * // values: [2, 4]
     * }</pre>
     *
     * @return A new primitive {@code long} array containing the elements of
     *         this sequence in enumeration order.
     */
    long @NotNull [] toArray();


    /**
     * <p>Creates an unmodifiable {@link List List<Long>} from an enumerable sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * List<Long> nums = Linq.ofLongs(1.0, 2.0, 2.0, 3.0, 4.0, 5.0)
     *     .distinct()
     *     .toList();
     *
     * System.out.println(nums.toString);
     *
     * // This code produces the following output:
     * //
     * // [1.0, 2.0, 3.0, 4.0, 5.0]
     * }</pre>
     *
     * @return A {@link List} that contains elements from the input sequence.
     * @see #toUnmodifiableList()
     */
    default List<Long> toList() {
        final List<Long> result = new ArrayList<>();
        this.forEach(result::add);
        return result;
    }

    /**
     * <p>Creates a {@link List List<Long>} from an enumerable sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * List<Long> nums = Linq.ofLongs(1.0, 2.0, 2.0, 3.0, 4.0, 5.0)
     *     .distinct()
     *     .toList();
     *
     * System.out.println(nums.toString);
     *
     * // This code produces the following output:
     * //
     * // [1.0, 2.0, 3.0, 4.0, 5.0]
     * }</pre>
     *
     * @return A {@link List} that contains elements from the input sequence.
     * @see #toList()
     */
    default List<Long> toUnmodifiableList() {
        final List<Long> result = new ArrayList<>();
        this.forEach(result::add);
        return result;
    }

    /**
     * <p>Returns a reference-type enumerable whose elements are the values of
     * this primitive sequence boxed as {@link Long} objects.</p>
     *
     * <p>This operation represents an explicit transition from a primitive
     * {@code long} pipeline to a reference-type pipeline. Each primitive value
     * is boxed when it crosses that boundary.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable numbers =
     *     Linq.ofLongs(1L, 2L, 3L);
     *
     * Enumerable<Long> boxed =
     *     numbers.boxed();
     *
     * boxed.forEach(System.out::println);
     * }</pre>
     *
     * @return An {@link Enumerable} containing the elements of this sequence
     *         boxed as {@link Long} values.
     */
    @NotNull
    Enumerable<Long> boxed();

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
     * LongEnumerable numbers = Linq.ofLongs(
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
    static LongEnumerable ofLongs(long @NotNull ... longs) {
        NullCheck.requireNonNull(longs);

        return new LongEnumPipeline.Head(() -> new LongArrayEnumerator(longs));
    }
}
