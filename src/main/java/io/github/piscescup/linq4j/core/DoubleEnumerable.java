package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.enumerator.DoubleArrayEnumerator;
import io.github.piscescup.linq4j.enumerator.DoubleEnumerator;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;

/**
 * Represents a sequence of primitive {@code double} values that supports
 * LINQ-style query and aggregation operations without requiring each element
 * to be boxed as a {@link Double}.
 *
 * <p>{@code DoubleEnumerable} is the primitive {@code double} specialization
 * of {@link Enumerable}. It provides query operations for filtering,
 * projection, aggregation, slicing, set operations, and materialization while
 * preserving primitive {@code double} values throughout a floating-point
 * pipeline whenever possible.</p>
 *
 * <p>Queries are typically expressed by chaining operations together. For
 * example, the following query filters positive values, squares them, and
 * computes their average:</p>
 *
 * <pre>{@code
 * DoubleEnumerable numbers = Linq.ofDoubles(
 *     -2.0, 1.0, 2.0, 3.0
 * );
 *
 * double average = numbers
 *     .where(number -> number > 0.0)
 *     .select(number -> number * number)
 *     .average();
 *
 * // (1^2 + 2^2 + 3^2) / 3 = 4.666...
 * }</pre>
 *
 * <h2>Primitive specialization</h2>
 *
 * <p>{@code DoubleEnumerable} operates directly on primitive {@code double}
 * values. In contrast, representing the same sequence as
 * {@code Enumerable<Double>} requires primitive values to be represented as
 * {@link Double} objects. Operations whose input and output remain within a
 * {@code DoubleEnumerable} therefore avoid the per-element boxing and
 * unboxing required by a reference-type pipeline.</p>
 *
 * <pre>{@code
 * DoubleEnumerable result = Linq.ofDoubles(1.0, 2.0, 3.0, 4.0)
 *     .where(value -> value >= 2.0)
 *     .select(value -> value * 1.5);
 *
 * double[] values = result.toArray();
 *
 * // values: [3.0, 4.5, 6.0]
 * }</pre>
 *
 * <p>Operations that explicitly cross from a primitive pipeline into a
 * reference-type pipeline, such as {@link #boxed()} or
 * {@link #selectToObj(DoubleFunction)}, necessarily produce reference values.
 * Such conversions are explicit so that boxing or object creation is visible
 * at the API boundary.</p>
 *
 * <h2>Floating-point equality</h2>
 *
 * <p>Equality-oriented operations such as {@link #contains(double)},
 * {@link #distinct()}, {@link #except(DoubleEnumerable)},
 * {@link #intersect(DoubleEnumerable)}, {@link #union(DoubleEnumerable)}, and
 * {@link #sequenceEqual(DoubleEnumerable)} use
 * {@link Double#doubleToLongBits(double)} semantics.</p>
 *
 * <p>Consequently, all NaN representations are considered equal, while
 * positive zero ({@code 0.0}) and negative zero ({@code -0.0}) are considered
 * distinct values.</p>
 *
 * <h2>Deferred execution</h2>
 *
 * <p>Most operations that return another {@code DoubleEnumerable} use
 * <em>deferred execution</em>. Calling an operation such as
 * {@link #where(DoublePredicate)}, {@link #select(DoubleUnaryOperator)},
 * {@link #skip(int)}, or {@link #take(int)} constructs a new query stage but
 * does not immediately enumerate the source sequence.</p>
 *
 * <p>The query is evaluated when the resulting enumerable is traversed through
 * {@link #enumerator()}, {@link #forEach(java.util.function.DoubleConsumer)},
 * or when a terminal operation such as {@link #sum()}, {@link #count()},
 * {@link #first()}, or {@link #toArray()} is invoked.</p>
 *
 * <pre>{@code
 * DoubleEnumerable numbers = Linq.ofDoubles(
 *     1.0, 2.0, 3.0, 4.0
 * );
 *
 * DoubleEnumerable query = numbers
 *     .where(number -> number >= 2.0)
 *     .select(number -> number * number);
 *
 * // The source is enumerated here.
 * double[] result = query.toArray();
 *
 * // result: [4.0, 9.0, 16.0]
 * }</pre>
 *
 * <h2>Enumeration</h2>
 *
 * <p>A {@code DoubleEnumerable} represents a sequence rather than a
 * single-use traversal. Each call to {@link #enumerator()} creates an
 * independent {@link DoubleEnumerator} for traversing the query.</p>
 *
 * <p>Traversal-specific state, such as indexes, buffers, counters, and
 * distinct-value sets, belongs to the individual enumeration rather than to
 * the query description itself.</p>
 *
 * <p>Unlike the reference-type {@link Enumerable}, a
 * {@code DoubleEnumerable} does not expose its primitive traversal through
 * {@code Iterable<Double>}. Doing so would require every primitive value to be
 * boxed as a {@link Double}. Reference-based interoperability is instead
 * provided explicitly through {@link #boxed()}.</p>
 *
 * <h2>Stateless and stateful operations</h2>
 *
 * <p>Operations such as {@code where}, {@code select}, {@code skip}, and
 * {@code take} can generally produce values directly as elements are requested
 * from the upstream sequence and are therefore typically stateless.</p>
 *
 * <p>Operations such as {@code reverse}, {@code shuffle}, {@code distinct},
 * and set operations may require buffers, sets, or other state associated with
 * the current enumeration.</p>
 *
 * <h2>Aggregation</h2>
 *
 * <p>{@link #aggregateToResult(double, DoubleBinaryOperator)} is the
 * general-purpose primitive {@code double} aggregation operation.</p>
 *
 * <pre>{@code
 * DoubleEnumerable numbers =
 *     Linq.ofDoubles(1.5, 2.5, 3.5);
 *
 * double sum = numbers.aggregateToResult(
 *     0.0,
 *     Double::sum
 * );
 *
 * // sum: 7.5
 * }</pre>
 *
 * <p>Common numeric aggregations are also provided directly:</p>
 *
 * <pre>{@code
 * DoubleEnumerable numbers =
 *     Linq.ofDoubles(4.0, 8.0, 15.0, 16.0, 23.0, 42.0);
 *
 * double sum = numbers.sum();
 * double min = numbers.min();
 * double max = numbers.max();
 * double average = numbers.average();
 * long count = numbers.count();
 * }</pre>
 *
 * <h2>Projection</h2>
 *
 * <p>{@link #select(DoubleUnaryOperator)} transforms each primitive
 * {@code double} value into another primitive {@code double} value while
 * preserving the primitive pipeline.</p>
 *
 * <p>The {@link #selectToInt(DoubleToIntFunction)} and
 * {@link #selectToLong(DoubleToLongFunction)} operations transition to another
 * primitive-specialized enumerable, while
 * {@link #selectToObj(DoubleFunction)} transitions to a reference-type
 * {@link Enumerable}.</p>
 *
 * <h2>Sequential and parallel evaluation</h2>
 *
 * <p>A {@code DoubleEnumerable} also carries an execution mode.
 * {@link #sequential()} and {@link #parallel()} can be used to request
 * sequential or parallel evaluation, and {@link #isParallel()} reports the
 * current mode.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 *
 * @see Enumerable
 * @see DoubleEnumerator
 * @see InternalDoubleEnumerable
 * @see BaseEnumerable
 */
public interface DoubleEnumerable
    extends BaseEnumerable<Double, DoubleEnumerable>,
    InternalDoubleEnumerable {

    /**
     * <p>Applies an accumulator function over this sequence of primitive
     * {@code double} values. The specified seed value is used as the initial
     * accumulator value.</p>
     *
     * <p>The accumulator and sequence elements remain primitive
     * {@code double} values throughout the operation.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.5, 2.5, 3.5);
     *
     * double sum = numbers.aggregateToResult(
     *     0.0,
     *     Double::sum
     * );
     *
     * System.out.println(sum);
     *
     * // This code produces the following output:
     * //
     * // 7.5
     * }</pre>
     *
     * @param seed The initial accumulator value.
     * @param aggregator An accumulator function to invoke on each element.
     * @return The final accumulated value.
     * @throws NullPointerException If {@code aggregator} is {@code null}.
     */
    double aggregateToResult(
        double seed,
        @NotNull DoubleBinaryOperator aggregator
    );

    /**
     * <p>Determines whether all elements of this sequence satisfy a specified
     * condition.</p>
     *
     * <p>Enumeration stops as soon as the result can be determined. If the
     * sequence is empty, {@code true} is returned.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(2.0, 4.0, 6.0, 8.0);
     *
     * boolean allPositive =
     *     numbers.all(value -> value > 0.0);
     *
     * System.out.println(allPositive);
     *
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return {@code true} if every element satisfies {@code predicate}, or
     *         if the sequence is empty; otherwise {@code false}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    boolean all(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Determines whether this sequence contains any elements.</p>
     *
     * <p>Enumeration stops as soon as the result can be determined.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.0, 2.0, 3.0);
     *
     * boolean hasElements = numbers.any();
     *
     * System.out.println(hasElements);
     *
     * // true
     * }</pre>
     *
     * @return {@code true} if the sequence contains at least one element;
     *         otherwise {@code false}.
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
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.0, 2.5, 4.5);
     *
     * boolean result =
     *     numbers.any(value -> value > 4.0);
     *
     * System.out.println(result);
     *
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return {@code true} if at least one element satisfies
     *         {@code predicate}; otherwise {@code false}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    boolean any(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Appends a specified primitive {@code double} value to the end of this
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
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.0, 2.0, 3.0);
     *
     * DoubleEnumerable result =
     *     numbers.append(4.0);
     *
     * result.forEach(System.out::println);
     *
     * // 1.0
     * // 2.0
     * // 3.0
     * // 4.0
     * }</pre>
     *
     * @param element The element to append to the sequence.
     * @return A new enumerable containing all elements of this sequence
     *         followed by {@code element}.
     */
    @NotNull
    DoubleEnumerable append(double element);

    /**
     * <p>Computes the average of the primitive {@code double} values in this
     * sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(2.0, 4.0, 6.0, 8.0);
     *
     * double average = numbers.average();
     *
     * System.out.println(average);
     *
     * // 5.0
     * }</pre>
     *
     * @return The arithmetic mean of the values in this sequence.
     * @throws ArithmeticException If the sequence contains no elements.
     */
    double average();

    /**
     * <p>Concatenates this sequence with another primitive {@code double}
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
     * DoubleEnumerable first =
     *     Linq.ofDoubles(1.0, 2.0);
     *
     * DoubleEnumerable second =
     *     Linq.ofDoubles(3.0, 4.0);
     *
     * DoubleEnumerable result =
     *     first.concat(second);
     *
     * // result: 1.0, 2.0, 3.0, 4.0
     * }</pre>
     *
     * @param after The sequence to concatenate to this sequence.
     * @return A {@code DoubleEnumerable} containing this sequence followed by
     *         {@code after}.
     * @throws NullPointerException If {@code after} is {@code null}.
     */
    @NotNull
    DoubleEnumerable concat(
        @NotNull DoubleEnumerable after
    );

    /**
     * <p>Determines whether this sequence contains the specified primitive
     * {@code double} value.</p>
     *
     * <p>Equality uses {@link Double#doubleToLongBits(double)} semantics.
     * Consequently, all NaN values compare as equal, while {@code 0.0} and
     * {@code -0.0} are distinct.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.0, 2.0, 3.0);
     *
     * boolean contains =
     *     numbers.contains(2.0);
     *
     * System.out.println(contains);
     *
     * // true
     * }</pre>
     *
     * @param value The value to locate in the sequence.
     * @return {@code true} if the sequence contains {@code value};
     *         otherwise {@code false}.
     */
    boolean contains(double value);

    /**
     * <p>Returns the number of elements in this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(10.0, 20.0, 30.0, 40.0);
     *
     * long count = numbers.count();
     *
     * System.out.println(count);
     *
     * // 4
     * }</pre>
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
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.0, 2.0, 3.0, 4.0);
     *
     * long count =
     *     numbers.count(value -> value >= 3.0);
     *
     * System.out.println(count);
     *
     * // 2
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The number of elements satisfying {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    long count(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Returns the elements of this sequence, or a singleton sequence
     * containing the specified default value if this sequence is empty.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles();
     *
     * DoubleEnumerable result =
     *     numbers.defaultIfEmpty(-1.0);
     *
     * result.forEach(System.out::println);
     *
     * // -1.0
     * }</pre>
     *
     * @param defaultValue The value returned when this sequence is empty.
     * @return An enumerable containing this sequence, or a singleton sequence
     *         containing {@code defaultValue} if it is empty.
     */
    @NotNull
    DoubleEnumerable defaultIfEmpty(double defaultValue);

    /**
     * <p>Returns distinct primitive {@code double} values from this
     * sequence.</p>
     *
     * <p>Only the first occurrence of each value is returned, and encounter
     * order is preserved. Equality uses
     * {@link Double#doubleToLongBits(double)} semantics.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.0, 2.0, 2.0, 3.0, 1.0);
     *
     * DoubleEnumerable result =
     *     numbers.distinct();
     *
     * // result: 1.0, 2.0, 3.0
     * }</pre>
     *
     * @return A {@code DoubleEnumerable} containing the distinct values from
     *         this sequence.
     */
    @NotNull
    DoubleEnumerable distinct();

    /**
     * <p>Returns the primitive {@code double} value at the specified
     * zero-based index.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(10.5, 20.5, 30.5);
     *
     * double value =
     *     numbers.elementAt(1);
     *
     * System.out.println(value);
     *
     * // 20.5
     * }</pre>
     *
     * @param index The zero-based index of the element to retrieve.
     * @return The element at the specified position.
     * @throws IndexOutOfBoundsException If {@code index} is outside the
     *         bounds of this sequence.
     */
    double elementAt(int index);

    /**
     * <p>Returns the primitive {@code double} value at the specified
     * zero-based index as an {@link OptionalDouble}, or an empty
     * {@code OptionalDouble} if the index is outside this sequence.</p>
     *
     * @param index The zero-based index of the element to retrieve.
     * @return An {@link OptionalDouble} containing the element at
     *         {@code index}, or an empty {@code OptionalDouble}.
     */
    @NotNull
    OptionalDouble elementAtOrEmpty(int index);

    /**
     * <p>Returns the primitive {@code double} value at the specified
     * zero-based index, or the specified default value if the index is outside
     * this sequence.</p>
     *
     * @param index The zero-based index of the element to retrieve.
     * @param defaultValue The value returned if {@code index} is out of range.
     * @return The element at {@code index}, or {@code defaultValue}.
     */
    double elementAtOrDefault(
        int index,
        double defaultValue
    );

    /**
     * <p>Produces the set difference of this sequence and another primitive
     * {@code double} sequence.</p>
     *
     * <p>The returned sequence contains distinct values from this sequence
     * that do not occur in {@code other}. Equality uses
     * {@link Double#doubleToLongBits(double)} semantics.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param other The sequence whose values are excluded from this sequence.
     * @return An enumerable containing the set difference of this sequence and
     *         {@code other}.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    @NotNull
    DoubleEnumerable except(
        @NotNull DoubleEnumerable other
    );

    /**
     * <p>Returns the first primitive {@code double} value in this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(10.5, 20.5, 30.5);
     *
     * double first = numbers.first();
     *
     * System.out.println(first);
     *
     * // 10.5
     * }</pre>
     *
     * @return The first element in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    double first();

    /**
     * <p>Returns the first primitive {@code double} value in this sequence
     * that satisfies the specified condition.</p>
     *
     * <p>The search stops as soon as a matching element is found.</p>
     *
     * @param predicate A function to test each element for a condition.
     * @return The first element satisfying {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws NoSuchElementException If no element satisfies the predicate.
     */
    double first(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Returns the first primitive {@code double} value as an
     * {@link OptionalDouble}, or an empty {@code OptionalDouble} if the
     * sequence contains no elements.</p>
     *
     * @return An {@link OptionalDouble} containing the first element, or an
     *         empty {@code OptionalDouble}.
     */
    @NotNull
    OptionalDouble firstOrEmpty();

    /**
     * <p>Returns the first primitive {@code double} value satisfying the
     * specified condition as an {@link OptionalDouble}, or an empty
     * {@code OptionalDouble} if no matching element exists.</p>
     *
     * @param predicate A function to test each element for a condition.
     * @return An {@link OptionalDouble} containing the first matching element,
     *         or an empty {@code OptionalDouble}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    OptionalDouble firstOrEmpty(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Produces the set intersection of this sequence and another primitive
     * {@code double} sequence.</p>
     *
     * <p>The returned sequence contains each value at most once. Equality uses
     * {@link Double#doubleToLongBits(double)} semantics.</p>
     *
     * @param other The sequence whose values are compared with this sequence.
     * @return An enumerable containing distinct values that occur in both
     *         sequences.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    @NotNull
    DoubleEnumerable intersect(
        @NotNull DoubleEnumerable other
    );

    /**
     * <p>Returns the maximum primitive {@code double} value in this
     * sequence.</p>
     *
     * <p>Values are compared using
     * {@link Double#compare(double, double)} semantics.</p>
     *
     * @return The maximum value in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    double max();

    /**
     * <p>Returns the minimum primitive {@code double} value in this
     * sequence.</p>
     *
     * <p>Values are compared using
     * {@link Double#compare(double, double)} semantics.</p>
     *
     * @return The minimum value in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    double min();

    /**
     * <p>Adds a specified primitive {@code double} value to the beginning of
     * this sequence.</p>
     *
     * <p>This method does not modify the current sequence.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param element The element to prepend to the sequence.
     * @return A new enumerable containing {@code element} followed by all
     *         elements of this sequence.
     */
    @NotNull
    DoubleEnumerable prepend(double element);

    /**
     * <p>Returns the elements of this sequence in reverse order.</p>
     *
     * <p>The source sequence is buffered using primitive {@code double}
     * storage when the returned enumerable is traversed.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @return A {@code DoubleEnumerable} containing the elements of this
     *         sequence in reverse order.
     */
    @NotNull
    DoubleEnumerable reverse();

    /**
     * <p>Returns the elements of this sequence in randomized order.</p>
     *
     * <p>The source sequence is buffered using primitive {@code double}
     * storage before the elements are shuffled.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @return A {@code DoubleEnumerable} containing the elements of this
     *         sequence in randomized order.
     */
    @NotNull
    DoubleEnumerable shuffle();

    /**
     * <p>Determines whether this sequence and another primitive
     * {@code double} sequence contain equal values in the same order.</p>
     *
     * <p>Equality uses {@link Double#doubleToLongBits(double)} semantics.
     * Enumeration stops as soon as a difference is detected.</p>
     *
     * @param other The sequence to compare with this sequence.
     * @return {@code true} if both sequences contain equal values in the same
     *         order; otherwise {@code false}.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    boolean sequenceEqual(
        @NotNull DoubleEnumerable other
    );

    /**
     * <p>Projects each primitive {@code double} element into another primitive
     * {@code double} value.</p>
     *
     * <p>This operation preserves the primitive {@code double} pipeline and
     * therefore does not require projected values to be boxed as
     * {@link Double} objects.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.0, 2.0, 3.0);
     *
     * DoubleEnumerable squares =
     *     numbers.select(value -> value * value);
     *
     * // squares: 1.0, 4.0, 9.0
     * }</pre>
     *
     * @param selector A transform function to apply to each element.
     * @return A {@code DoubleEnumerable} containing the projected values.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    DoubleEnumerable select(
        @NotNull DoubleUnaryOperator selector
    );

    /**
     * <p>Projects each primitive {@code double} element into a primitive
     * {@code int} value.</p>
     *
     * <p>The returned sequence remains primitive-specialized.</p>
     *
     * @param selector A transform function to apply to each element.
     * @return An {@link IntEnumerable} containing the projected primitive
     *         {@code int} values.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    IntEnumerable selectToInt(
        @NotNull DoubleToIntFunction selector
    );

    /**
     * <p>Projects each primitive {@code double} element into a primitive
     * {@code long} value.</p>
     *
     * <p>The returned sequence remains primitive-specialized.</p>
     *
     * @param selector A transform function to apply to each element.
     * @return A {@link LongEnumerable} containing the projected primitive
     *         {@code long} values.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    LongEnumerable selectToLong(
        @NotNull DoubleToLongFunction selector
    );

    /**
     * <p>Projects each primitive {@code double} element into an object and
     * returns the results as a reference-type {@link Enumerable}.</p>
     *
     * <p>This operation represents an explicit transition from a primitive
     * pipeline to a reference-type pipeline.</p>
     *
     * @param selector A transform function to apply to each primitive element.
     * @param <R> The type of the resulting elements.
     * @return An {@link Enumerable} containing the projected reference values.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    <R> @NotNull Enumerable<R> selectToObj(
        @NotNull DoubleFunction<? extends R> selector
    );

    /**
     * <p>Computes the sum of the primitive {@code double} values in this
     * sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.5, 2.5, 3.0);
     *
     * double sum = numbers.sum();
     *
     * System.out.println(sum);
     *
     * // 7.0
     * }</pre>
     *
     * @return The sum of the elements in this sequence, or {@code 0.0} if the
     *         sequence contains no elements.
     */
    double sum();

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
     * @return A {@code DoubleEnumerable} containing the remaining elements.
     */
    @NotNull
    DoubleEnumerable skip(int count);

    /**
     * <p>Returns a sequence containing all elements of this sequence except
     * for the last specified number of elements.</p>
     *
     * <p>If {@code count} is greater than or equal to the number of elements,
     * an empty sequence is produced. A non-positive value skips no
     * elements.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @param count The number of elements to omit from the end.
     * @return An enumerable containing all elements except the final
     *         {@code count} elements.
     */
    @NotNull
    DoubleEnumerable skipLast(int count);

    /**
     * <p>Bypasses elements in this sequence as long as the specified condition
     * is satisfied and then returns the remaining elements.</p>
     *
     * <p>After the predicate first returns {@code false}, no later elements
     * are tested by the predicate.</p>
     *
     * @param predicate A function to test each element for a condition.
     * @return A sequence beginning with the first value that does not satisfy
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    DoubleEnumerable skipWhile(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Returns a specified number of contiguous elements from the beginning
     * of this sequence.</p>
     *
     * <p>If {@code count} is greater than the number of elements, all elements
     * are returned. A non-positive value produces an empty sequence.</p>
     *
     * @param count The number of elements to return.
     * @return A {@code DoubleEnumerable} containing at most the specified
     *         number of elements.
     */
    @NotNull
    DoubleEnumerable take(int count);

    /**
     * <p>Returns the specified number of contiguous elements from the end of
     * this sequence.</p>
     *
     * <p>If {@code count} is greater than or equal to the number of elements,
     * the entire sequence is returned. A non-positive value produces an empty
     * sequence.</p>
     *
     * @param count The number of elements to return from the end.
     * @return An enumerable containing at most the last {@code count}
     *         elements.
     */
    @NotNull
    DoubleEnumerable takeLast(int count);

    /**
     * <p>Returns elements from the beginning of this sequence as long as the
     * specified condition is satisfied.</p>
     *
     * <p>Enumeration stops when the predicate first returns
     * {@code false}.</p>
     *
     * @param predicate A function to test each element for a condition.
     * @return An enumerable containing the initial elements satisfying
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    DoubleEnumerable takeWhile(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Produces the set union of this sequence and another primitive
     * {@code double} sequence.</p>
     *
     * <p>The resulting sequence contains each value at most once. Values from
     * this sequence are returned first, followed by previously unseen values
     * from {@code other}. Equality uses
     * {@link Double#doubleToLongBits(double)} semantics.</p>
     *
     * @param other The sequence whose values are combined with this sequence.
     * @return An enumerable containing the distinct values from both
     *         sequences.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    @NotNull
    DoubleEnumerable union(
        @NotNull DoubleEnumerable other
    );

    /**
     * <p>Returns an enumerable containing only the elements of this sequence
     * that satisfy the specified condition.</p>
     *
     * <p>This operation uses deferred execution and processes values directly
     * as primitive {@code double}s.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.0, 2.5, 3.0, 4.5);
     *
     * DoubleEnumerable result =
     *     numbers.where(value -> value >= 3.0);
     *
     * // result: 3.0, 4.5
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return A {@code DoubleEnumerable} containing the elements satisfying
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    DoubleEnumerable where(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Creates an array containing all primitive {@code double} values in
     * this sequence.</p>
     *
     * <p>This is a terminal operation and causes the sequence to be
     * enumerated.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * double[] values = Linq.ofDoubles(1.0, 2.0, 3.0, 4.0)
     *     .where(value -> value >= 2.0)
     *     .toArray();
     *
     * // values: [2.0, 3.0, 4.0]
     * }</pre>
     *
     * @return A new primitive {@code double} array containing the elements of
     *         this sequence in enumeration order.
     */
    double @NotNull [] toArray();

    // ---------------------------------------------------------------------
// Last
// ---------------------------------------------------------------------

    /**
     * <p>Returns the last primitive {@code double} value in this sequence.</p>
     *
     * <p>The sequence is enumerated until its end in order to determine the last
     * element.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles(
     *     10.5, 20.5, 30.5, 40.5
     * );
     *
     * double last = numbers.last();
     *
     * System.out.println(last);
     *
     * // This code produces the following output:
     * //
     * // 40.5
     * }</pre>
     *
     * @return The last element in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    double last();

    /**
     * <p>Returns the last primitive {@code double} value in this sequence that
     * satisfies the specified condition.</p>
     *
     * <p>The entire sequence is enumerated because a later element may also
     * satisfy {@code predicate}.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles(
     *     1.0, 2.5, 3.0, 4.5, 5.0
     * );
     *
     * double last = numbers.last(
     *     value -> value < 5.0
     * );
     *
     * System.out.println(last);
     *
     * // This code produces the following output:
     * //
     * // 4.5
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The last element that satisfies {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws NoSuchElementException If no element satisfies
     *         {@code predicate}.
     */
    double last(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Returns the last primitive {@code double} value in this sequence as an
     * {@link OptionalDouble}, or an empty {@code OptionalDouble} if the sequence
     * contains no elements.</p>
     *
     * <p>The sequence is enumerated until its end in order to determine the last
     * element.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles();
     *
     * OptionalDouble last = numbers.lastOrEmpty();
     *
     * System.out.println(last.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @return An {@link OptionalDouble} containing the last element, or an empty
     *         {@code OptionalDouble} if the sequence contains no elements.
     */
    @NotNull
    OptionalDouble lastOrEmpty();

    /**
     * <p>Returns the last primitive {@code double} value that satisfies the
     * specified condition as an {@link OptionalDouble}, or an empty
     * {@code OptionalDouble} if no matching element exists.</p>
     *
     * <p>The entire sequence is enumerated because a later element may also
     * satisfy {@code predicate}.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles(
     *     1.0, 2.0, 3.0
     * );
     *
     * OptionalDouble last = numbers.lastOrEmpty(
     *     value -> value > 10.0
     * );
     *
     * System.out.println(last.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An {@link OptionalDouble} containing the last matching element,
     *         or an empty {@code OptionalDouble} if no element satisfies
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    OptionalDouble lastOrEmpty(
        @NotNull DoublePredicate predicate
    );


    /**
     * <p>Returns the only primitive {@code double} value in this sequence.</p>
     *
     * <p>This operation succeeds only when the sequence contains exactly one
     * element. An empty sequence and a sequence containing multiple elements are
     * considered invalid for this operation.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles(42.5);
     *
     * double value = numbers.single();
     *
     * System.out.println(value);
     *
     * // This code produces the following output:
     * //
     * // 42.5
     * }</pre>
     *
     * @return The single element in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     * @throws IllegalStateException If the sequence contains more than one
     *         element.
     */
    double single();

    /**
     * <p>Returns the only primitive {@code double} value in this sequence that
     * satisfies the specified condition.</p>
     *
     * <p>This operation succeeds only when exactly one element satisfies
     * {@code predicate}. If no element satisfies the condition, or if more than
     * one element satisfies it, the operation fails.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles(
     *     1.0, 2.5, 4.0, 7.5
     * );
     *
     * double value = numbers.single(
     *     number -> number > 3.0 && number < 5.0
     * );
     *
     * System.out.println(value);
     *
     * // This code produces the following output:
     * //
     * // 4.0
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
    double single(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Returns the only primitive {@code double} value in this sequence as an
     * {@link OptionalDouble}, or an empty {@code OptionalDouble} if the sequence
     * contains no elements.</p>
     *
     * <p>If the sequence contains more than one element, this operation throws an
     * exception rather than selecting one of the elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles();
     *
     * OptionalDouble value = numbers.singleOrEmpty();
     *
     * System.out.println(value.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @return An {@link OptionalDouble} containing the single element, or an empty
     *         {@code OptionalDouble} if the sequence contains no elements.
     * @throws IllegalStateException If the sequence contains more than one
     *         element.
     */
    @NotNull
    OptionalDouble singleOrEmpty();

    /**
     * <p>Returns the only primitive {@code double} value that satisfies the
     * specified condition as an {@link OptionalDouble}.</p>
     *
     * <p>If no element satisfies {@code predicate}, an empty
     * {@code OptionalDouble} is returned. If more than one element satisfies the
     * predicate, this operation throws an exception.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles(
     *     1.0, 2.5, 4.0, 7.5
     * );
     *
     * OptionalDouble value = numbers.singleOrEmpty(
     *     number -> number > 3.0 && number < 5.0
     * );
     *
     * System.out.println(value.getAsDouble());
     *
     * // This code produces the following output:
     * //
     * // 4.0
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An {@link OptionalDouble} containing the single matching element,
     *         or an empty {@code OptionalDouble} if no element satisfies
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws IllegalStateException If more than one element satisfies
     *         {@code predicate}.
     */
    @NotNull
    OptionalDouble singleOrEmpty(
        @NotNull DoublePredicate predicate
    );

    /**
     * <p>Returns the elements of this sequence ordered in ascending numerical
     * order.</p>
     *
     * <p>The source sequence is buffered using primitive {@code double} storage
     * when the returned enumerable is traversed. No {@link Double} objects are
     * required for sorting.</p>
     *
     * <p>The ordering is consistent with
     * {@link Double#compare(double, double)}. In particular, negative zero
     * ({@code -0.0}) is ordered before positive zero ({@code 0.0}), and NaN
     * values are ordered after all other {@code double} values.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles(
     *     5.5, 2.0, 8.25, 1.5, 4.0
     * );
     *
     * DoubleEnumerable ordered = numbers.order();
     *
     * ordered.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1.5
     * // 2.0
     * // 4.0
     * // 5.5
     * // 8.25
     * }</pre>
     *
     * @return A {@code DoubleEnumerable} containing the elements of this sequence
     *         in ascending numerical order.
     */
    @NotNull
    DoubleEnumerable order();

    /**
     * <p>Returns the elements of this sequence ordered in descending numerical
     * order.</p>
     *
     * <p>The source sequence is buffered using primitive {@code double} storage
     * when the returned enumerable is traversed. No {@link Double} objects are
     * required for sorting.</p>
     *
     * <p>The ordering is the reverse of the ordering defined by
     * {@link Double#compare(double, double)}. Consequently, NaN values are ordered
     * before all other {@code double} values in the resulting sequence.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers = Linq.ofDoubles(
     *     5.5, 2.0, 8.25, 1.5, 4.0
     * );
     *
     * DoubleEnumerable ordered =
     *     numbers.orderDescending();
     *
     * ordered.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 8.25
     * // 5.5
     * // 4.0
     * // 2.0
     * // 1.5
     * }</pre>
     *
     * @return A {@code DoubleEnumerable} containing the elements of this sequence
     *         in descending numerical order.
     */
    @NotNull
    DoubleEnumerable orderDescending();

    /**
     * <p>Returns a reference-type enumerable whose elements are the values of
     * this primitive sequence boxed as {@link Double} objects.</p>
     *
     * <p>This operation represents an explicit transition from a primitive
     * {@code double} pipeline to a reference-type pipeline.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable numbers =
     *     Linq.ofDoubles(1.0, 2.0, 3.0);
     *
     * Enumerable<Double> boxed =
     *     numbers.boxed();
     *
     * boxed.forEach(System.out::println);
     * }</pre>
     *
     * @return An {@link Enumerable} containing the elements of this sequence
     *         boxed as {@link Double} values.
     */
    @NotNull
    Enumerable<Double> boxed();

    /**
     * <p>Creates a {@link List List<Double>} from an enumerable sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * List<Double> nums = Linq.ofDoubles(1L, 2L, 2L, 3L, 4L, 5L)
     *     .distinct()
     *     .toList();
     *
     * System.out.println(nums.toString);
     *
     * // This code produces the following output:
     * //
     * // [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @return A {@link List} that contains elements from the input sequence.
     * @see #toUnmodifiableList()
     */
    default List<Double> toList() {
        final List<Double> result = new ArrayList<>();
        this.forEach(result::add);
        return result;
    }

    /**
     * <p>Creates an unmodifiable {@link List List<Double>} from an enumerable sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * List<Double> nums = Linq.ofDoubles(1L, 2L, 2L, 3L, 4L, 5L)
     *     .distinct()
     *     .toList();
     *
     * System.out.println(nums.toString);
     *
     * // This code produces the following output:
     * //
     * // [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @return A {@link List} that contains elements from the input sequence.
     * @see #toUnmodifiableList()
     */
    default List<Double> toUnmodifiableList() {
        final List<Double> result = new ArrayList<>();
        this.forEach(result::add);
        return result;
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
     * DoubleEnumerable numbers = Linq.ofDoubles(
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
    static DoubleEnumerable ofDoubles(double @NotNull ... doubles) {
        NullCheck.requireNonNull(doubles);

        return new DoubleEnumPipeline.Head(() -> new DoubleArrayEnumerator(doubles));
    }
}