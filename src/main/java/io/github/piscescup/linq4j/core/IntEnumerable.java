package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.enumerator.IntArrayEnumerator;
import io.github.piscescup.linq4j.enumerator.IntEnumerator;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;

/**
 * Represents a sequence of primitive {@code int} values that supports
 * LINQ-style query and aggregation operations without requiring each element
 * to be boxed as an {@link Integer}.
 *
 * <p>{@code IntEnumerable} is the primitive {@code int} specialization of
 * {@link Enumerable}. It provides query operations for filtering, projection,
 * aggregation, slicing, and materialization while preserving primitive
 * {@code int} values throughout an integer pipeline whenever possible.</p>
 *
 * <p>Queries are typically expressed by chaining operations together. For
 * example, the following query filters the even values of a sequence, squares
 * them, and computes their sum:</p>
 *
 * <pre>{@code
 * IntEnumerable numbers = Linq.ofInts(
 *     1, 2, 3, 4, 5, 6
 * );
 *
 * int sum = numbers
 *     .where(number -> number % 2 == 0)
 *     .select(number -> number * number)
 *     .sum();
 *
 * // 2^2 + 4^2 + 6^2 = 56
 * // sum: 56
 * }</pre>
 *
 * <h2>Primitive specialization</h2>
 *
 * <p>{@code IntEnumerable} operates directly on primitive {@code int} values.
 * In contrast, representing an integer sequence as
 * {@code Enumerable<Integer>} requires primitive values to be represented as
 * {@link Integer} objects. Operations whose input and output remain within an
 * {@code IntEnumerable} therefore avoid the per-element boxing and unboxing
 * that would otherwise be required by a reference-type pipeline.</p>
 *
 * <p>For example, the following pipeline can process its elements entirely as
 * primitive {@code int} values:</p>
 *
 * <pre>{@code
 * IntEnumerable result = Linq.ofInts(1, 2, 3, 4, 5)
 *     .where(value -> value > 2)
 *     .select(value -> value * 10);
 *
 * int[] values = result.toArray();
 *
 * // values: [30, 40, 50]
 * }</pre>
 *
 * <p>Operations that explicitly cross from a primitive pipeline into a
 * reference-type pipeline, such as {@link #boxed()} or
 * {@link #selectToObj(IntFunction)}, necessarily produce reference values.
 * Such conversions are explicit so that boxing or object creation is visible
 * at the API boundary.</p>
 *
 * <h2>Deferred execution</h2>
 *
 * <p>Most operations that return another {@code IntEnumerable} use
 * <em>deferred execution</em>. Calling an operation such as
 * {@link #where(IntPredicate)}, {@link #select(IntUnaryOperator)},
 * {@link #skip(int)}, or {@link #take(int)} constructs a new query stage but
 * does not immediately enumerate the source sequence.</p>
 *
 * <p>The query is evaluated when the resulting enumerable is traversed through
 * {@link #enumerator()}, {@link #forEach(java.util.function.IntConsumer)}, or
 * when a terminal operation such as {@link #sum()}, {@link #count()},
 * {@link #first()}, or {@link #toArray()} is invoked.</p>
 *
 * <pre>{@code
 * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4, 5, 6);
 *
 * // Defines the query. The source is not enumerated here.
 * IntEnumerable query = numbers
 *     .where(number -> number % 2 == 0)
 *     .select(number -> number * number);
 *
 * // Enumerates the source and evaluates the query.
 * int[] result = query.toArray();
 *
 * // result: [4, 16, 36]
 * }</pre>
 *
 * <h2>Enumeration</h2>
 *
 * <p>An {@code IntEnumerable} represents a sequence rather than a single-use
 * traversal. Each call to {@link #enumerator()} creates an independent
 * {@link IntEnumerator} for traversing the query.</p>
 *
 * <p>Traversal-specific state, such as indexes, buffers, counters, and other
 * operation state, belongs to the individual enumeration rather than to the
 * query description itself. Consequently, the same enumerable query can
 * normally be enumerated multiple times.</p>
 *
 * <p>Unlike the reference-type {@link Enumerable}, an
 * {@code IntEnumerable} does not expose its primitive traversal through
 * {@code Iterable<Integer>}. Doing so would require every primitive value to
 * be converted to an {@link Integer}. Reference-based interoperability is
 * instead provided explicitly through {@link #boxed()}.</p>
 *
 * <h2>Stateless and stateful operations</h2>
 *
 * <p>Some operations can produce output directly as elements are requested
 * from the upstream sequence. Operations such as {@code where},
 * {@code select}, {@code skip}, and {@code take} are typical stateless
 * operations.</p>
 *
 * <p>Other operations may require state associated with the current
 * enumeration. For example, ordering, reversing, distinct-value processing,
 * and set operations may need to buffer elements or maintain auxiliary
 * collections. Such state should be created independently for each
 * enumeration.</p>
 *
 * <h2>Aggregation</h2>
 *
 * <p>Aggregation operations consume the primitive sequence and produce a
 * single result. {@link #aggregateToResult(int, IntBinaryOperator)} is the
 * general-purpose primitive integer aggregation operation.</p>
 *
 * <pre>{@code
 * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4, 5);
 *
 * int sum = numbers.aggregateToResult(
 *     0,
 *     Integer::sum
 * );
 *
 * // sum: 15
 * }</pre>
 *
 * <p>Common numeric aggregations are also provided directly:</p>
 *
 * <pre>{@code
 * IntEnumerable numbers = Linq.ofInts(4, 8, 15, 16, 23, 42);
 *
 * int sum = numbers.sum();
 * int min = numbers.min();
 * int max = numbers.max();
 * double average = numbers.average();
 * long count = numbers.count();
 * }</pre>
 *
 * <h2>Filtering with {@code where}</h2>
 *
 * <p>{@link #where(IntPredicate)} filters the primitive sequence according to
 * a specified predicate. Elements are requested from the upstream sequence
 * until an element satisfying the predicate is found.</p>
 *
 * <pre>{@code
 * IntEnumerable numbers =
 *     Linq.ofInts(1, 2, 3, 4, 5, 6, 7, 8);
 *
 * IntEnumerable evenNumbers = numbers.where(
 *     number -> number % 2 == 0
 * );
 *
 * int[] result = evenNumbers.toArray();
 *
 * // result: [2, 4, 6, 8]
 * }</pre>
 *
 * <h2>Projection</h2>
 *
 * <p>{@link #select(IntUnaryOperator)} transforms each {@code int} element
 * into another {@code int} value while preserving the primitive pipeline.</p>
 *
 * <pre>{@code
 * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4);
 *
 * IntEnumerable squares = numbers.select(
 *     number -> number * number
 * );
 *
 * // squares: 1, 4, 9, 16
 * }</pre>
 *
 * <p>Projection may also change the shape of the pipeline. The
 * {@link #selectToLong(IntToLongFunction)} and
 * {@link #selectToDouble(IntToDoubleFunction)} operations produce another
 * primitive-specialized enumerable, while
 * {@link #selectToObj(IntFunction)} produces a reference-type
 * {@link Enumerable}.</p>
 *
 * <pre>{@code
 * Enumerable<String> labels = Linq.ofInts(10, 20, 30)
 *     .selectToObj(value -> "Value: " + value);
 *
 * // labels: ["Value: 10", "Value: 20", "Value: 30"]
 * }</pre>
 *
 * <h2>Query composition</h2>
 *
 * <p>Primitive operations are designed to be composed in the same manner as
 * the operations of {@link Enumerable}. A query can remain primitive through
 * multiple stages and cross into another primitive or reference pipeline only
 * when an explicit projection requires it.</p>
 *
 * <pre>{@code
 * double result = Linq.ofInts(1, 2, 3, 4, 5, 6)
 *     .where(value -> value % 2 == 0)
 *     .select(value -> value * value)
 *     .selectToDouble(value -> value / 2.0)
 *     .average();
 * }</pre>
 *
 * <h2>Sequential and parallel evaluation</h2>
 *
 * <p>An {@code IntEnumerable} also carries an execution mode.
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
 * @see IntEnumerator
 * @see InternalIntEnumerable
 * @see BaseEnumerable
 */
public interface IntEnumerable
    extends BaseEnumerable<Integer, IntEnumerable>, InternalIntEnumerable {

    /**
     * <p>Applies an accumulator function over this sequence of primitive
     * {@code int} values. The specified seed value is used as the initial
     * accumulator value.</p>
     *
     * <p>The accumulator and sequence elements are represented as primitive
     * {@code int} values throughout the operation.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4, 5);
     *
     * int sum = numbers.aggregateToResult(
     *     0,
     *     Integer::sum
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
    int aggregateToResult(
        int seed,
        @NotNull IntBinaryOperator aggregator
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
     * IntEnumerable numbers = Linq.ofInts(2, 4, 6, 8);
     *
     * boolean allEven = numbers.all(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(allEven);
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return {@code true} if every element satisfies {@code predicate},
     *         or if the sequence is empty; otherwise, {@code false}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    boolean all(
        @NotNull IntPredicate predicate
    );

    /**
     * <p>Determines whether this sequence contains any elements.</p>
     *
     * <p>Enumeration stops as soon as the result can be determined.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3);
     *
     * boolean hasElements = numbers.any();
     *
     * System.out.println(hasElements);
     *
     * // This code produces the following output:
     * //
     * // true
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
     * IntEnumerable numbers = Linq.ofInts(1, 3, 5, 8);
     *
     * boolean containsEven = numbers.any(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(containsEven);
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return {@code true} if at least one element satisfies
     *         {@code predicate}; otherwise, {@code false}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    boolean any(
        @NotNull IntPredicate predicate
    );


// ---------------------------------------------------------------------
// Concatenation
// ---------------------------------------------------------------------

    /**
     * <p>Appends a specified primitive {@code int} value to the end of this
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
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3);
     *
     * IntEnumerable result = numbers.append(4);
     *
     * result.forEach(System.out::println);
     *
     * // 1
     * // 2
     * // 3
     * // 4
     * }</pre>
     *
     * @param element The element to append to the sequence.
     * @return A new enumerable containing all elements of this sequence followed
     *         by {@code element}.
     */
    @NotNull
    IntEnumerable append(int element);

    /**
     * <p>Computes the average of the primitive {@code int} values in this
     * sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(2, 4, 6, 8);
     *
     * double average = numbers.average();
     *
     * System.out.println(average);
     *
     * // This code produces the following output:
     * //
     * // 5.0
     * }</pre>
     *
     * @return The arithmetic mean of the values in this sequence.
     * @throws ArithmeticException If the sequence contains no elements.
     */
    double average();


    /**
     * <p>Concatenates this sequence with another primitive integer sequence.</p>
     *
     * <p>The resulting sequence contains all elements of this sequence followed
     * by all elements of {@code after}. The encounter order of both sequences
     * is preserved.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable first = Linq.ofInts(1, 2, 3);
     * IntEnumerable second = Linq.ofInts(4, 5, 6);
     *
     * IntEnumerable result = first.concat(second);
     *
     * result.forEach(System.out::println);
     *
     * // 1
     * // 2
     * // 3
     * // 4
     * // 5
     * // 6
     * }</pre>
     *
     * @param after The sequence to concatenate to this sequence.
     * @return An {@code IntEnumerable} containing the elements of this sequence
     *         followed by the elements of {@code after}.
     * @throws NullPointerException If {@code after} is {@code null}.
     */
    @NotNull
    IntEnumerable concat(
        @NotNull IntEnumerable after
    );

    /**
     * <p>Determines whether this sequence contains the specified primitive
     * {@code int} value.</p>
     *
     * <p>The search stops as soon as a matching value is encountered.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4);
     *
     * boolean contains = numbers.contains(3);
     *
     * System.out.println(contains);
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param value The value to locate in the sequence.
     * @return {@code true} if the sequence contains {@code value};
     *         otherwise, {@code false}.
     */
    boolean contains(int value);

    /**
     * <p>Returns the number of elements in this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(10, 20, 30, 40);
     *
     * long count = numbers.count();
     *
     * System.out.println(count);
     *
     * // This code produces the following output:
     * //
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
     * <p>Each element is tested using {@code predicate}. Only elements for which
     * the predicate returns {@code true} are included in the count.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(
     *     1, 2, 3, 4, 5, 6
     * );
     *
     * long count = numbers.count(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(count);
     *
     * // This code produces the following output:
     * //
     * // 3
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The number of elements in this sequence that satisfy
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws ArithmeticException If the number of matching elements exceeds
     *         {@link Long#MAX_VALUE}.
     */
    long count(
        @NotNull IntPredicate predicate
    );

    /**
     * <p>Returns the elements of this sequence, or a singleton sequence
     * containing the specified default value if this sequence is empty.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts();
     *
     * IntEnumerable result = numbers.defaultIfEmpty(-1);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // -1
     * }</pre>
     *
     * @param defaultValue The value returned when this sequence is empty.
     * @return An enumerable containing the elements of this sequence, or a
     *         singleton sequence containing {@code defaultValue} if the
     *         sequence is empty.
     */
    @NotNull
    IntEnumerable defaultIfEmpty(int defaultValue);

    /**
     * <p>Returns distinct primitive {@code int} values from this sequence.</p>
     *
     * <p>Only the first occurrence of each value is returned, and the encounter
     * order of those first occurrences is preserved.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(
     *     1, 2, 2, 3, 1, 4
     * );
     *
     * IntEnumerable distinct = numbers.distinct();
     *
     * distinct.forEach(System.out::println);
     *
     * // 1
     * // 2
     * // 3
     * // 4
     * }</pre>
     *
     * @return An {@code IntEnumerable} containing the distinct values from this
     *         sequence.
     */
    @NotNull
    IntEnumerable distinct();

    /**
     * <p>Returns the primitive {@code int} value at the specified zero-based
     * index in this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(10, 20, 30, 40);
     *
     * int value = numbers.elementAt(2);
     *
     * System.out.println(value);
     *
     * // This code produces the following output:
     * //
     * // 30
     * }</pre>
     *
     * @param index The zero-based index of the element to retrieve.
     * @return The element at the specified position.
     * @throws IndexOutOfBoundsException If {@code index} is negative or greater
     *         than or equal to the number of elements in the sequence.
     */
    int elementAt(int index);

    /**
     * <p>Returns the primitive {@code int} value at the specified zero-based
     * index as an {@link OptionalInt}, or an empty {@code OptionalInt} if the
     * index is outside the bounds of this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(10, 20, 30);
     *
     * OptionalInt result = numbers.elementAtOrEmpty(5);
     *
     * System.out.println(result.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param index The zero-based index of the element to retrieve.
     * @return An {@link OptionalInt} containing the element at {@code index},
     *         or an empty {@code OptionalInt} if the index is out of range.
     */
    @NotNull
    OptionalInt elementAtOrEmpty(int index);

    /**
     * <p>Returns the primitive {@code int} value at the specified zero-based
     * index, or the specified default value if the index is outside the bounds
     * of this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(10, 20, 30);
     *
     * int value = numbers.elementAtOrDefault(5, -1);
     *
     * System.out.println(value);
     *
     * // This code produces the following output:
     * //
     * // -1
     * }</pre>
     *
     * @param index The zero-based index of the element to retrieve.
     * @param defaultValue The value returned if {@code index} is out of range.
     * @return The element at {@code index}, or {@code defaultValue} if the
     *         specified index is outside the bounds of the sequence.
     */
    int elementAtOrDefault(
        int index,
        int defaultValue
    );

    /**
     * <p>Produces the set difference of this sequence and another primitive
     * integer sequence.</p>
     *
     * <p>The returned sequence contains distinct values from this sequence that
     * do not occur in {@code other}. The order of the first occurrence of each
     * returned value is preserved.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable first = Linq.ofInts(1, 2, 2, 3, 4);
     * IntEnumerable second = Linq.ofInts(2, 4);
     *
     * IntEnumerable result = first.except(second);
     *
     * result.forEach(System.out::println);
     *
     * // 1
     * // 3
     * }</pre>
     *
     * @param other The sequence whose values are excluded from this sequence.
     * @return An enumerable containing the set difference of this sequence and
     *         {@code other}.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    @NotNull
    IntEnumerable except(
        @NotNull IntEnumerable other
    );

    /**
     * <p>Returns the first primitive {@code int} value in this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(10, 20, 30);
     *
     * int first = numbers.first();
     *
     * System.out.println(first);
     *
     * // This code produces the following output:
     * //
     * // 10
     * }</pre>
     *
     * @return The first element in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    int first();

    /**
     * <p>Returns the first primitive {@code int} value in this sequence that
     * satisfies the specified condition.</p>
     *
     * <p>The search stops as soon as a matching element is found.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 3, 6, 8, 10);
     *
     * int firstEven = numbers.first(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(firstEven);
     *
     * // This code produces the following output:
     * //
     * // 6
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The first element that satisfies {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws NoSuchElementException If no element satisfies
     *         {@code predicate}.
     */
    int first(
        @NotNull IntPredicate predicate
    );

    /**
     * <p>Returns the first primitive {@code int} value in this sequence as an
     * {@link OptionalInt}, or an empty {@code OptionalInt} if the sequence
     * contains no elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts();
     *
     * OptionalInt first = numbers.firstOrEmpty();
     *
     * System.out.println(first.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @return An {@link OptionalInt} containing the first element, or an empty
     *         {@code OptionalInt} if the sequence contains no elements.
     */
    @NotNull
    OptionalInt firstOrEmpty();

    /**
     * <p>Returns the first primitive {@code int} value that satisfies the
     * specified condition as an {@link OptionalInt}, or an empty
     * {@code OptionalInt} if no matching element exists.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 3, 5, 7);
     *
     * OptionalInt firstEven = numbers.firstOrEmpty(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(firstEven.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An {@link OptionalInt} containing the first matching element,
     *         or an empty {@code OptionalInt} if no element satisfies
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    OptionalInt firstOrEmpty(
        @NotNull IntPredicate predicate
    );

    /**
     * <p>Returns the last primitive {@code int} value in this sequence.</p>
     *
     * <p>The sequence is enumerated until its end in order to determine the last
     * element.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(10, 20, 30, 40);
     *
     * int last = numbers.last();
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
    int last();

    /**
     * <p>Returns the last primitive {@code int} value in this sequence that
     * satisfies the specified condition.</p>
     *
     * <p>The entire sequence is enumerated because a later element may also
     * satisfy {@code predicate}.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(
     *     1, 2, 3, 4, 5, 6, 7
     * );
     *
     * int lastEven = numbers.last(
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
    int last(
        @NotNull IntPredicate predicate
    );

    /**
     * <p>Returns the last primitive {@code int} value in this sequence as an
     * {@link OptionalInt}, or an empty {@code OptionalInt} if the sequence
     * contains no elements.</p>
     *
     * <p>The sequence is enumerated until its end in order to determine the last
     * element.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts();
     *
     * OptionalInt last = numbers.lastOrEmpty();
     *
     * System.out.println(last.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @return An {@link OptionalInt} containing the last element, or an empty
     *         {@code OptionalInt} if the sequence contains no elements.
     */
    @NotNull
    OptionalInt lastOrEmpty();

    /**
     * <p>Returns the last primitive {@code int} value that satisfies the
     * specified condition as an {@link OptionalInt}, or an empty
     * {@code OptionalInt} if no matching element exists.</p>
     *
     * <p>The entire sequence is enumerated because a later element may also
     * satisfy {@code predicate}.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 3, 5, 7);
     *
     * OptionalInt lastEven = numbers.lastOrEmpty(
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
     * @return An {@link OptionalInt} containing the last matching element,
     *         or an empty {@code OptionalInt} if no element satisfies
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    OptionalInt lastOrEmpty(
        @NotNull IntPredicate predicate
    );

    /**
     * <p>Produces the set intersection of this sequence and another primitive
     * integer sequence.</p>
     *
     * <p>The returned sequence contains each value at most once. Values are
     * produced according to their first occurrence in this sequence.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable first = Linq.ofInts(1, 2, 2, 3, 4);
     * IntEnumerable second = Linq.ofInts(2, 4, 5);
     *
     * IntEnumerable result = first.intersect(second);
     *
     * result.forEach(System.out::println);
     *
     * // 2
     * // 4
     * }</pre>
     *
     * @param other The sequence whose values are compared with this sequence.
     * @return An enumerable containing the distinct values that occur in both
     *         sequences.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    @NotNull
    IntEnumerable intersect(
        @NotNull IntEnumerable other
    );

    /**
     * <p>Returns the maximum primitive {@code int} value in this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(9, 4, 7, 2, 8);
     *
     * int maximum = numbers.max();
     *
     * System.out.println(maximum);
     *
     * // This code produces the following output:
     * //
     * // 9
     * }</pre>
     *
     * @return The maximum value in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    int max();

    /**
     * <p>Returns the minimum primitive {@code int} value in this sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(9, 4, 7, 2, 8);
     *
     * int minimum = numbers.min();
     *
     * System.out.println(minimum);
     *
     * // This code produces the following output:
     * //
     * // 2
     * }</pre>
     *
     * @return The minimum value in this sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    int min();


    /**
     * <p>Returns the elements of this sequence ordered in ascending numerical
     * order.</p>
     *
     * <p>The source sequence is buffered using primitive {@code int} storage when
     * the returned enumerable is traversed. No {@link Integer} objects are
     * required for sorting.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(
     *     5, 2, 8, 1, 4
     * );
     *
     * IntEnumerable ordered = numbers.order();
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
     * @return An {@code IntEnumerable} containing the elements of this sequence
     *         in ascending numerical order.
     */
    @NotNull
    IntEnumerable order();

    /**
     * <p>Returns the elements of this sequence ordered in descending numerical
     * order.</p>
     *
     * <p>The source sequence is buffered using primitive {@code int} storage when
     * the returned enumerable is traversed. No {@link Integer} objects are
     * required for sorting.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(
     *     5, 2, 8, 1, 4
     * );
     *
     * IntEnumerable ordered =
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
     * @return An {@code IntEnumerable} containing the elements of this sequence
     *         in descending numerical order.
     */
    @NotNull
    IntEnumerable orderDescending();

    /**
     * <p>Adds a specified primitive {@code int} value to the beginning of this
     * sequence.</p>
     *
     * <p>This method does not modify the current sequence. Instead, it returns
     * a new sequence containing the specified value followed by all elements of
     * this sequence.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(2, 3, 4);
     *
     * IntEnumerable result = numbers.prepend(1);
     *
     * result.forEach(System.out::println);
     *
     * // 1
     * // 2
     * // 3
     * // 4
     * }</pre>
     *
     * @param element The element to prepend to the sequence.
     * @return A new enumerable containing {@code element} followed by all
     *         elements of this sequence.
     */
    @NotNull
    IntEnumerable prepend(int element);

    /**
     * <p>Returns the elements of this sequence in reverse order.</p>
     *
     * <p>The source sequence is buffered when the returned enumerable is
     * traversed. Primitive values are stored directly without boxing.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4);
     *
     * IntEnumerable reversed = numbers.reverse();
     *
     * reversed.forEach(System.out::println);
     *
     * // 4
     * // 3
     * // 2
     * // 1
     * }</pre>
     *
     * @return An {@code IntEnumerable} containing the elements of this sequence
     *         in reverse order.
     */
    @NotNull
    IntEnumerable reverse();

    /**
     * <p>Returns the elements of this sequence in a randomized order.</p>
     *
     * <p>The source sequence is buffered before the elements are shuffled.
     * Primitive values are stored directly without boxing.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * @return An {@code IntEnumerable} containing the elements of this sequence
     *         in a randomized order.
     */
    @NotNull
    IntEnumerable shuffle();

    /**
     * <p>Returns the only primitive {@code int} value in this sequence.</p>
     *
     * <p>This operation succeeds only when the sequence contains exactly one
     * element. An empty sequence and a sequence containing multiple elements are
     * considered invalid for this operation.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(42);
     *
     * int value = numbers.single();
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
    int single();

    /**
     * <p>Returns the only primitive {@code int} value in this sequence that
     * satisfies the specified condition.</p>
     *
     * <p>This operation succeeds only when exactly one element satisfies
     * {@code predicate}. If no element satisfies the condition, or if more than
     * one element satisfies it, the operation fails.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(
     *     1, 3, 4, 5, 7
     * );
     *
     * int even = numbers.single(
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
    int single(
        @NotNull IntPredicate predicate
    );

    /**
     * <p>Returns the only primitive {@code int} value in this sequence as an
     * {@link OptionalInt}, or an empty {@code OptionalInt} if the sequence
     * contains no elements.</p>
     *
     * <p>If the sequence contains more than one element, this operation throws an
     * exception rather than selecting one of the elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts();
     *
     * OptionalInt value = numbers.singleOrEmpty();
     *
     * System.out.println(value.isEmpty());
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @return An {@link OptionalInt} containing the single element, or an empty
     *         {@code OptionalInt} if the sequence contains no elements.
     * @throws IllegalStateException If the sequence contains more than one
     *         element.
     */
    @NotNull
    OptionalInt singleOrEmpty();

    /**
     * <p>Returns the only primitive {@code int} value that satisfies the
     * specified condition as an {@link OptionalInt}.</p>
     *
     * <p>If no element satisfies {@code predicate}, an empty
     * {@code OptionalInt} is returned. If more than one element satisfies the
     * predicate, this operation throws an exception.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(
     *     1, 3, 4, 5, 7
     * );
     *
     * OptionalInt even = numbers.singleOrEmpty(
     *     value -> value % 2 == 0
     * );
     *
     * System.out.println(even.getAsInt());
     *
     * // This code produces the following output:
     * //
     * // 4
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An {@link OptionalInt} containing the single matching element,
     *         or an empty {@code OptionalInt} if no element satisfies
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws IllegalStateException If more than one element satisfies
     *         {@code predicate}.
     */
    @NotNull
    OptionalInt singleOrEmpty(
        @NotNull IntPredicate predicate
    );


    /**
     * <p>Returns a sequence containing all elements of this sequence except for
     * the last specified number of elements.</p>
     *
     * <p>If {@code count} is greater than or equal to the number of elements,
     * an empty sequence is produced. A non-positive value skips no elements.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4, 5);
     *
     * IntEnumerable result = numbers.skipLast(2);
     *
     * result.forEach(System.out::println);
     *
     * // 1
     * // 2
     * // 3
     * }</pre>
     *
     * @param count The number of elements to omit from the end of the sequence.
     * @return An enumerable containing all elements except the last
     *         {@code count} elements.
     */
    @NotNull
    IntEnumerable skipLast(int count);

    /**
     * <p>Bypasses elements in this sequence as long as the specified condition
     * is satisfied and then returns the remaining elements.</p>
     *
     * <p>After the predicate first returns {@code false}, no further elements
     * are tested by the predicate.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 6, 4, 2);
     *
     * IntEnumerable result = numbers.skipWhile(
     *     value -> value < 5
     * );
     *
     * result.forEach(System.out::println);
     *
     * // 6
     * // 4
     * // 2
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An enumerable containing the elements beginning with the first
     *         element that does not satisfy {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    IntEnumerable skipWhile(
        @NotNull IntPredicate predicate
    );


    /**
     * <p>Determines whether this sequence and another primitive integer sequence
     * contain equal values in the same order.</p>
     *
     * <p>Enumeration stops as soon as a difference is detected. Two empty
     * sequences are considered equal.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable first = Linq.ofInts(1, 2, 3);
     * IntEnumerable second = Linq.ofInts(1, 2, 3);
     *
     * boolean equal = first.sequenceEqual(second);
     *
     * System.out.println(equal);
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param other The sequence to compare with this sequence.
     * @return {@code true} if both sequences contain the same values in the
     *         same order; otherwise, {@code false}.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    boolean sequenceEqual(
        @NotNull IntEnumerable other
    );


    /**
     * <p>Projects each primitive {@code int} element of this sequence into
     * another primitive {@code int} value.</p>
     *
     * <p>This operation preserves the primitive integer pipeline and therefore
     * does not require the projected values to be boxed as {@link Integer}
     * objects.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4);
     *
     * IntEnumerable squares =
     *     numbers.select(number -> number * number);
     *
     * squares.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1
     * // 4
     * // 9
     * // 16
     * }</pre>
     *
     * @param selector A transform function to apply to each element.
     * @return An {@code IntEnumerable} whose elements are the result of
     *         invoking {@code selector} on each element of this sequence.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    IntEnumerable select(
        @NotNull IntUnaryOperator selector
    );

    /**
     * <p>Projects each primitive {@code int} element of this sequence into a
     * primitive {@code long} value.</p>
     *
     * <p>The returned sequence is a primitive-specialized
     * {@link LongEnumerable}, so the projected values do not need to be boxed
     * as {@link Long} objects.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * LongEnumerable values = Linq.ofInts(1, 2, 3)
     *     .selectToLong(value -> (long) value * 1_000_000_000L);
     *
     * values.forEach(System.out::println);
     * }</pre>
     *
     * @param selector A transform function to apply to each element.
     * @return A {@link LongEnumerable} whose elements are the projected
     *         primitive {@code long} values.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    LongEnumerable selectToLong(
        @NotNull IntToLongFunction selector
    );

    /**
     * <p>Projects each primitive {@code int} element of this sequence into a
     * primitive {@code double} value.</p>
     *
     * <p>The returned sequence is a primitive-specialized
     * {@link DoubleEnumerable}, so the projected values do not need to be
     * boxed as {@link Double} objects.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * DoubleEnumerable halves = Linq.ofInts(1, 2, 3, 4)
     *     .selectToDouble(value -> value / 2.0);
     *
     * halves.forEach(System.out::println);
     *
     * // 0.5
     * // 1.0
     * // 1.5
     * // 2.0
     * }</pre>
     *
     * @param selector A transform function to apply to each element.
     * @return A {@link DoubleEnumerable} whose elements are the projected
     *         primitive {@code double} values.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    DoubleEnumerable selectToDouble(
        @NotNull IntToDoubleFunction selector
    );

    /**
     * <p>Projects each primitive {@code int} element of this sequence into an
     * object and returns the resulting values as a reference-type
     * {@link Enumerable}.</p>
     *
     * <p>This operation represents an explicit transition from a primitive
     * integer pipeline to a reference-type pipeline.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> values = Linq.ofInts(10, 20, 30)
     *     .selectToObj(value -> "Value: " + value);
     *
     * values.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // Value: 10
     * // Value: 20
     * // Value: 30
     * }</pre>
     *
     * @param selector A transform function to apply to each primitive element.
     * @param <R> The type of the resulting elements.
     * @return An {@link Enumerable} whose elements are the result of invoking
     *         {@code selector} on each element of this sequence.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    <R> @NotNull Enumerable<R> selectToObj(
        @NotNull IntFunction<? extends R> selector
    );

    /**
     * <p>Computes the sum of the primitive {@code int} values in this
     * sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4, 5);
     *
     * int sum = numbers.sum();
     *
     * System.out.println(sum);
     *
     * // This code produces the following output:
     * //
     * // 15
     * }</pre>
     *
     * @return The sum of the elements in this sequence, or {@code 0} if the
     *         sequence contains no elements.
     * @throws ArithmeticException If the sum overflows the range of an
     *         {@code int}, if overflow checking is enabled by the
     *         implementation.
     */
    int sum();


    /**
     * <p>Bypasses a specified number of elements in this sequence and returns
     * the remaining elements.</p>
     *
     * <p>If {@code count} is greater than the number of elements in the
     * sequence, an empty enumerable is returned. A non-positive value does not
     * skip any elements.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(
     *     1, 2, 3, 4, 5
     * );
     *
     * IntEnumerable result = numbers.skip(2);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 3
     * // 4
     * // 5
     * }</pre>
     *
     * @param count The number of elements to skip.
     * @return An {@code IntEnumerable} containing the elements that occur
     *         after the specified number of elements.
     */
    @NotNull
    IntEnumerable skip(int count);



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
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(
     *     1, 2, 3, 4, 5
     * );
     *
     * IntEnumerable result = numbers.take(3);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1
     * // 2
     * // 3
     * }</pre>
     *
     * @param count The number of elements to return.
     * @return An {@code IntEnumerable} containing at most the specified
     *         number of elements from the beginning of this sequence.
     */
    @NotNull
    IntEnumerable take(int count);

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
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 4, 5);
     *
     * IntEnumerable result = numbers.takeLast(2);
     *
     * result.forEach(System.out::println);
     *
     * // 4
     * // 5
     * }</pre>
     *
     * @param count The number of elements to return from the end of the sequence.
     * @return An enumerable containing at most the last {@code count} elements.
     */
    @NotNull
    IntEnumerable takeLast(int count);

    /**
     * <p>Returns elements from the beginning of this sequence as long as the
     * specified condition is satisfied.</p>
     *
     * <p>Enumeration stops when the predicate first returns {@code false}.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3, 6, 4, 2);
     *
     * IntEnumerable result = numbers.takeWhile(
     *     value -> value < 5
     * );
     *
     * result.forEach(System.out::println);
     *
     * // 1
     * // 2
     * // 3
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An enumerable containing the elements that occur before the first
     *         element that does not satisfy {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    IntEnumerable takeWhile(
        @NotNull IntPredicate predicate
    );


    /**
     * <p>Produces the set union of this sequence and another primitive integer
     * sequence.</p>
     *
     * <p>The resulting sequence contains each value at most once. Values from
     * this sequence are returned first, followed by previously unseen values
     * from {@code other}.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable first = Linq.ofInts(1, 2, 3);
     * IntEnumerable second = Linq.ofInts(3, 4, 5);
     *
     * IntEnumerable result = first.union(second);
     *
     * result.forEach(System.out::println);
     *
     * // 1
     * // 2
     * // 3
     * // 4
     * // 5
     * }</pre>
     *
     * @param other The sequence whose values are combined with this sequence.
     * @return An enumerable containing the distinct values from both sequences.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    @NotNull
    IntEnumerable union(
        @NotNull IntEnumerable other
    );

    /**
     * <p>Returns an enumerable containing only the elements of this sequence
     * that satisfy the specified condition.</p>
     *
     * <p>This operation uses deferred execution and processes values directly
     * as primitive {@code int}s.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers =
     *     Linq.ofInts(1, 2, 3, 4, 5, 6);
     *
     * IntEnumerable evenNumbers =
     *     numbers.where(number -> number % 2 == 0);
     *
     * evenNumbers.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 2
     * // 4
     * // 6
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An {@code IntEnumerable} containing the elements that satisfy
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @NotNull
    IntEnumerable where(
        @NotNull IntPredicate predicate
    );


    /**
     * <p>Creates an array containing all primitive {@code int} values in this
     * sequence.</p>
     *
     * <p>This is a terminal operation and causes the sequence to be
     * enumerated.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * int[] values = Linq.ofInts(1, 2, 3, 4)
     *     .where(value -> value % 2 == 0)
     *     .toArray();
     *
     * // values: [2, 4]
     * }</pre>
     *
     * @return A new primitive {@code int} array containing the elements of
     *         this sequence in enumeration order.
     */
    int @NotNull [] toArray();

    /**
     * <p>Returns a reference-type enumerable whose elements are the values of
     * this primitive sequence boxed as {@link Integer} objects.</p>
     *
     * <p>This operation represents an explicit transition from a primitive
     * integer pipeline to a reference-type pipeline. Each primitive value is
     * boxed when it crosses that boundary.</p>
     *
     * <p>This operation uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * IntEnumerable numbers = Linq.ofInts(1, 2, 3);
     *
     * Enumerable<Integer> boxed = numbers.boxed();
     *
     * boxed.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 1
     * // 2
     * // 3
     * }</pre>
     *
     * @return An {@link Enumerable} containing the elements of this sequence
     *         boxed as {@link Integer} values.
     */
    @NotNull
    Enumerable<Integer> boxed();

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
     * IntEnumerable numbers = Linq.ofInts(
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
    static IntEnumerable ofInts(int... ints) {
        NullCheck.requireNonNull(ints);
        return new IntEnumPipeline.Head(() -> new IntArrayEnumerator(ints));
    }
}