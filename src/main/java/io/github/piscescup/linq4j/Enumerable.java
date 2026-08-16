package io.github.piscescup.linq4j;

import io.github.piscescup.interfaces.Equalable;
import io.github.piscescup.interfaces.Equalator;
import io.github.piscescup.interfaces.Pair;
import io.github.piscescup.interfaces.exfunction.BinFunction;
import io.github.piscescup.interfaces.exfunction.BinPredicate;
import io.github.piscescup.linq4j.base.Groupable;
import io.github.piscescup.linq4j.exceptions.OverflowEnumerableException;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.*;

/**
 * A sequence of elements that supports aggregate operations in a declarative
 * style, analogous to the .NET {@code IEnumerable<T>} interface and its
 * Language Integrated Query (LINQ) extensions.  The following example
 * illustrates a typical aggregate operation using an {@code Enumerable}:
 *
 * <pre>{@code
 * int sum = Linq.fromIterable(widgets)
 *     .where(w -> w.getColor() == RED)
 *     .select(w -> w.getWeight())
 *     .sum();
 * }</pre>
 *
 * In this example, {@code widgets} is a {@code Collection<Widget>}.  We obtain
 * an {@code Enumerable<Widget>} via {@code asEnumerable()} (or a similar factory
 * method), filter it to retain only the red widgets using {@code where}, and
 * then transform each remaining widget into an {@code int} representing its
 * weight via {@code select}. Finally, the terminal operation {@code sum} computes
 * the total weight of all red widgets.
 *
 * <p>An {@code Enumerable} can be viewed as a <em>query</em> over its source
 * data.  The query is expressed as a <em>pipeline</em> of operations, which
 * consists of a source (e.g., a collection, an array, a generator function, or
 * an I/O channel), zero or more <em>intermediate operations</em> (such as
 * {@code where}, {@code select}, or {@code orderBy}) that transform one
 * {@code Enumerable} into another, and a <em>terminal operation</em> (such as
 * {@code sum}, {@code count}, or {@code forEach}) that produces a result or
 * performs a side-effect.  The pipeline is <em>lazy</em>; computation on the
 * source data is deferred until the terminal operation is invoked, and elements
 * are consumed only as needed.
 *
 * <p>Implementations are allowed significant freedom in optimizing the execution
 * of the pipeline.  For example, an implementation may elide intermediate
 * operations entirely if it can prove that doing so does not affect the final
 * result.  Consequently, behavioral parameters (such as predicates or mapping
 * functions) may not be invoked in all cases, and any side-effects they might
 * have are not guaranteed to occur—unless specifically documented by the
 * terminal operation (e.g., {@code forEach} is guaranteed to execute its action
 * for each element).  For more details, see the discussion on
 * <a href="package-summary.html#SideEffects">side-effects</a>.
 *
 * <p>While collections and enumerables share some superficial similarities,
 * their purposes differ.  Collections are primarily concerned with the efficient
 * storage, management, and direct access to their elements.  In contrast, an
 * {@code Enumerable} does not provide direct access to its elements; instead,
 * it is a <em>query</em> that declaratively describes the source and the
 * operations to be performed upon it.  If the built-in operations are insufficient,
 * you may obtain a traditional iterator via {@link #iterator()} or
 * {@link #spliterator()} to perform manual traversal.
 *
 * <p>Most operations accept user-specified behavioral parameters (typically
 * lambda expressions or method references) that must be <em>non-interfering</em>
 * (they do not modify the stream source) and, in most cases, <em>stateless</em>
 * (their result must not depend on any state that might change during pipeline
 * execution).  These parameters must also be <em>non-null</em> unless otherwise
 * specified.
 *
 * <p>An {@code Enumerable} should be operated on (i.e., intermediate or terminal
 * operations invoked) only once.  Reusing the same enumerable—for example, by
 * attempting to traverse it multiple times or by "forking" it into multiple
 * pipelines—is not supported and may lead to {@link IllegalStateException} if
 * detected.  Some operations may return the same instance rather than a new
 * object, making detection of reuse not always possible, but clients should
 * avoid such patterns.
 *
 * <p>An {@code Enumerable} may implement {@link AutoCloseable} and provide a
 * {@link #close()} method.  Once closed, any further operation will throw
 * {@link IllegalStateException}.  Most enumerable instances do not require
 * explicit closing, as they are backed by in-memory collections or generating
 * functions that hold no external resources.  However, if the source is an I/O
 * channel (e.g., lines read from a file), the enumerable <em>must</em> be used
 * within a try-with-resources block or equivalent to ensure timely release of
 * resources.
 *
 * <p>Execution may be sequential or parallel; this is a property of the
 * enumerable instance.  The initial execution mode is determined by the factory
 * method used to create it (e.g., a sequential or parallel factory).  The mode
 * can be changed using {@link #sequential()} or {@link #parallel()}, and the
 * current mode can be queried with {@link #isParallel()}.
 *
 * @param <T> the type of the elements in this enumerable
 * @since 1.0.0
 * @see BaseEnumerable
 * @see <a href="https://learn.microsoft.com/en-us/dotnet/api/system.collections.ienumerable?view=net-10.0">
 *     IEnumerable in .NET</a>
 * @see <a href="https://learn.microsoft.com/en-us/dotnet/csharp/linq/get-started/introduction-to-linq-queries">
 *     Language Integrated Query (LINQ)</a>
 */
public interface Enumerable<T>
    extends BaseEnumerable<T, Enumerable<T>>
{
    /**
     * <p>Applies an accumulator function over a sequence.
     * The specified seed value is used as the initial accumulator value,
     * and the specified function is used to select the result value.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "mango", "orange", "passionfruit", "grape");
     *
     * // Determine whether any string in the array is longer than "banana".
     * string longestName =
     *     fruits.aggregate("banana",
     *                     (longest, next) ->
     *                         next.Length > longest.Length ? next : longest,
     *                     // Return the final result as an upper case string.
     *                     fruit -> fruit.ToUpper());
     *
     * System.out.printf("The fruit with the longest name is %s.\n ", longestName)
     *
     * // This code produces the following output:
     * //
     * // The fruit with the longest name is PASSIONFRUIT.
     *
     * }</pre>
     *
     * @param seed The initial accumulator value.
     * @param aggregator An accumulator function to be invoked on each element.
     * @param resultSelector A function to transform the final accumulator value into the result value.
     * @return The transformed final accumulator value.
     * @param <A> The type of the accumulator value.
     * @param <R> The type of the resulting value.
     */
    <A, R> R aggregateToResult(
        A seed,
        @NotNull BinFunction<? super A, ? super T, ? extends A> aggregator,
        @NotNull Function<? super A, ? extends R> resultSelector
    );

    /**
     * <p>Applies an accumulator function over a sequence.
     * The specified seed value is used as the initial accumulator value.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> ints = Linq.of(4, 8, 8, 3, 9, 0, 7, 8, 2);
     *
     * // Count the even numbers in the array, using a seed value of 0.
     * int numEven = ints.aggregate(0, (total, next) ->
     *                                     next % 2 == 0 ? total + 1 : total);
     *
     * System.out.printf("The number of even integers is: %s", numEven);
     *
     * // This code produces the following output:
     * //
     * // The number of even integers is: 6
     * }</pre>
     *
     * @param seed The initial accumulator value.
     * @param aggregator An accumulator function to be invoked on each element.
     * @return The transformed final accumulator value.
     * @param <A> The type of the accumulator value.
     * @param <R> The type of the resulting value.
     */
    <A> A aggregateToResult(
        @NotNull A seed, @NotNull BinFunction<? super A, ? super T, ? extends A> aggregator
    );

    /**
     * <p>Applies an accumulator function over a sequence and get the accumulation result.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * String sentence = "the quick brown fox jumps over the lazy dog";
     *
     * // Split the string into individual words.
     * String[] words = sentence.Split(' ');
     *
     * // Prepend each word to the beginning of the
     * // new sentence to reverse the word order.
     * string reversed = Linq.fromArray(words)
     *     .Aggregate(
     *         (workingSentence, next) -> next + " " + workingSentence
     *     );
     *
     * System.out.println(reversed);
     *
     * // This code produces the following output:
     * //
     * // dog lazy the over jumps fox brown quick the
     * }</pre>
     *
     * @param aggregator An accumulator function to be invoked on each element.
     * @return The transformed final accumulator value.
     */
    T aggregateToResult(@NotNull BinFunction<? super T, ? super T, ? extends T> aggregator);

    /**
     * <p>
     * Applies an accumulator function over a sequence, grouping the results by key.
     * The specified seed value is used as the initial accumulator value for each key.
     * </p>
     *
     * <p>
     * Each resulting {@link Pair} contains the grouping key as its left element
     * and the accumulated value as its right element.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Employee> employees = Linq.of(
     *     new Employee("Alice", "HR", 45000),
     *     new Employee("Bob", "Technology", 50000),
     *     new Employee("Charlie", "Sales", 75000),
     *     new Employee("David", "Technology", 65000),
     *     new Employee("Eve", "HR", 40000)
     * );
     *
     * Enumerable<Pair<String, Integer>> result =
     *     employees.aggregateBy(
     *         Employee::department,
     *         0,
     *         (total, employee) -> total + employee.salary()
     *     );
     * }</pre>
     *
     * @param keySelector A function to extract the key for each element.
     * @param seed The initial accumulator value for each key.
     * @param aggregator An accumulator function to be invoked on each element.
     * @return An enumerable containing the aggregates corresponding to each key
     *         derived from the sequence.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <A> The type of the accumulator value.
     */
    <K, A> Enumerable<Pair<K, A>> aggregateBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull A seed,
        @NotNull BinFunction<? super A, ? super T, ? extends A> aggregator
    );

    /**
     * <p>
     * Applies an accumulator function over a sequence, grouping the results by key.
     * The specified seed value is used as the initial accumulator value for each key.
     * </p>
     *
     * <p>
     * The specified {@link Equalator} is used to determine whether two keys
     * are considered equal.
     * </p>
     *
     * <p>
     * Each resulting {@link Pair} contains the grouping key as its left element
     * and the accumulated value as its right element.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> words = Linq.of(
     *     "apple", "APPLE", "banana", "BANANA", "apple"
     * );
     *
     * Enumerable<Pair<String, Integer>> result =
     *     words.aggregateBy(
     *         String::toLowerCase,
     *         0,
     *         (count, word) -> count + 1,
     *         Equalator.defaultEqualator()
     *     );
     * }</pre>
     *
     * @param keySelector A function to extract the key for each element.
     * @param seed The initial accumulator value for each key.
     * @param aggregator An accumulator function to be invoked on each element.
     * @param keyEqualator An equalator used to compare keys for equality.
     * @return An enumerable containing the aggregates corresponding to each key
     *         derived from the sequence.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <A> The type of the accumulator value.
     */
    <K, A> Enumerable<Pair<K, A>> aggregateBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull A seed,
        @NotNull BinFunction<? super A, ? super T, ? extends A> aggregator,
        Equalator<? super K> keyEqualator
    );

    /**
     * <p>
     * Applies an accumulator function over a sequence, grouping the results by key.
     * A seed selector is used to create the initial accumulator value for each key.
     * </p>
     *
     * <p>
     * Each resulting {@link Pair} contains the grouping key as its left element
     * and the accumulated value as its right element.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Employee> employees = Linq.of(
     *     new Employee("Alice", "HR", 45000),
     *     new Employee("Bob", "Technology", 50000),
     *     new Employee("Charlie", "Sales", 75000)
     * );
     *
     * Enumerable<Pair<String, Integer>> result =
     *     employees.aggregateBy(
     *         Employee::department,
     *         department -> 0,
     *         (total, employee) -> total + employee.salary()
     *     );
     * }</pre>
     *
     * @param keySelector A function to extract the key for each element.
     * @param seedSelector A function used to create the initial accumulator value
     *                     for each key.
     * @param aggregator An accumulator function to be invoked on each element.
     * @return An enumerable containing the aggregates corresponding to each key
     *         derived from the sequence.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <A> The type of the accumulator value.
     */
    <K, A> Enumerable<Pair<K, A>> aggregateBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super K, ? extends A> seedSelector,
        @NotNull BinFunction<? super A, ? super T, ? extends A> aggregator
    );

    /**
     * <p>
     * Applies an accumulator function over a sequence, grouping the results by key.
     * A seed selector is used to create the initial accumulator value for each key.
     * </p>
     *
     * <p>
     * The specified {@link Equalator} is used to determine whether two keys
     * are considered equal.
     * </p>
     *
     * <p>
     * Each resulting {@link Pair} contains the grouping key as its left element
     * and the accumulated value as its right element.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Employee> employees = Linq.of(
     *     new Employee("Alice", "HR", 45000),
     *     new Employee("Bob", "Technology", 50000),
     *     new Employee("Charlie", "Sales", 75000)
     * );
     *
     * Enumerable<Pair<String, Integer>> result =
     *     employees.aggregateBy(
     *         Employee::department,
     *         department -> 0,
     *         (total, employee) -> total + employee.salary(),
     *         Equalator.defaultEqualator()
     *     );
     * }</pre>
     *
     * @param keySelector A function to extract the key for each element.
     * @param seedSelector A function used to create the initial accumulator value
     *                     for each key.
     * @param aggregator An accumulator function to be invoked on each element.
     * @param keyEqualator An equalator used to compare keys for equality.
     * @return An enumerable containing the aggregates corresponding to each key
     *         derived from the sequence.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <A> The type of the accumulator value.
     */
    <K, A> Enumerable<Pair<K, A>> aggregateBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super K, ? extends A> seedSelector,
        @NotNull BinFunction<? super A, ? super T, ? extends A> aggregator,
        @Nullable Equalator<? super K> keyEqualator
    );

    /**
     * <p>
     * Determines whether all elements of a sequence satisfy a specified condition.
     * </p>
     *
     * <p>
     * Enumeration stops as soon as the result can be determined.
     * If the sequence is empty, {@code true} is returned.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "orange"
     * );
     *
     * boolean allLongEnough =
     *     fruits.all(fruit -> fruit.length() >= 5);
     *
     * // This code produces:
     * //
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return {@code true} if every element of the sequence satisfies the
     *         specified condition, or if the sequence is empty;
     *         otherwise, {@code false}.
     */
    boolean all(Predicate<? super T> predicate);

    /**
     * <p>
     * Determines whether a sequence contains any elements.
     * </p>
     *
     * <p>
     * Enumeration stops as soon as the result can be determined.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "orange"
     * );
     *
     * boolean hasElements = fruits.any();
     *
     * // This code produces:
     * //
     * // true
     * }</pre>
     *
     * @return {@code true} if the sequence contains at least one element;
     *         otherwise, {@code false}.
     */
    default boolean any() {
        return any(null);
    }

    /**
     * <p>
     * Determines whether any element of a sequence satisfies a specified condition.
     * </p>
     *
     * <p>
     * Enumeration stops as soon as an element satisfies the specified condition.
     * If no element satisfies the condition, the entire sequence is enumerated.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "orange"
     * );
     *
     * boolean containsLongName =
     *     fruits.any(fruit -> fruit.length() > 6);
     *
     * // This code produces:
     * //
     * // true
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return {@code true} if the sequence is not empty and at least one element
     *         satisfies the specified condition; otherwise, {@code false}.
     */
    default boolean any(@Nullable Predicate<? super T> predicate) {
        try (Enumerator<T> e = enumerator()) {
            if (predicate==null) return e.moveNext();

            while (e.moveNext()) {
                if (predicate.test(e.current())) {
                    return true;
                }
            }
            return false;
        }
    }


    /**
     * <p>
     * Appends a specified element to the end of the sequence.
     * </p>
     *
     * <p>
     * This method does not modify the current sequence. Instead, it returns
     * a new sequence that contains all elements of this sequence followed
     * by the specified element.
     * </p>
     *
     * <p>
     * This method uses deferred execution.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(1, 2, 3, 4);
     *
     * Enumerable<Integer> result = numbers.append(5);
     *
     * // numbers: 1, 2, 3, 4
     * // result:  1, 2, 3, 4, 5
     * }</pre>
     *
     * @param element The element to append to the sequence.
     * @return A new enumerable that contains all elements of this sequence
     *         followed by {@code element}.
     */
    Enumerable<T> append(T element);

    /**
     * <p>
     * Computes the average of a sequence of {@code double} values that are
     * obtained by invoking a transform function on each element of the input
     * sequence.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "mango",
     *     "orange",
     *     "passionfruit",
     *     "grape"
     * );
     *
     * double average = fruits.averageToDouble(String::length);
     *
     * System.out.printf("The average string length is %s.%n", average);
     *
     * // This code produces the following output:
     * //
     * // The average string length is 6.5.
     * }</pre>
     *
     * @param selector A transform function to apply to each element.
     * @return The average of the sequence of values.
     * @throws NullPointerException if {@code selector} is {@code null}.
     * @throws ArithmeticException if the sequence contains no elements.
     * @param <R> The type of the result produced by the selector.
     */
    default double averageToDouble(@NotNull ToDoubleFunction<? super T> selector) {
        NullCheck.requireNonNull(selector, "selector");
        try (Enumerator<T> e = enumerator()) {
            double sum = 0.0;
            long count = 0;

            while (e.moveNext()) {
                sum += selector.applyAsDouble(e.current());
                count++;
            }

            if (count == 0) {
                throw new ArithmeticException("Cannot compute average of an empty sequence.");
            }

            return sum / count;
        }
    }

    /**
     * <p>
     * Computes the average of a sequence of {@link BigDecimal} values that are
     * obtained by invoking a transform function on each element of the input
     * sequence, rounding the result according to the specified
     * {@link RoundingMode}.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Product> products = Linq.of(
     *     new Product("Apple", new BigDecimal("1.20")),
     *     new Product("Mango", new BigDecimal("2.50")),
     *     new Product("Orange", new BigDecimal("1.80"))
     * );
     *
     * BigDecimal average = products.averageToDecimal(Product::price, RoundingMode.HALF_UP);
     *
     * System.out.printf("The average price is %s.%n", average);
     *
     * // This code produces the following output:
     * //
     * // The average price is 1.83.
     * }</pre>
     *
     * @param selector A transform function to apply to each element.
     * @param roundingMode The rounding mode to apply when the exact quotient
     *                     cannot be represented with the scale of the sum.
     * @return The average of the sequence of values, rounded according to
     *         {@code roundingMode}.
     * @throws NullPointerException if {@code selector} or {@code roundingMode} is {@code null}.
     * @throws ArithmeticException if the sequence contains no elements.
     */
    default BigDecimal averageToDecimal(
        @NotNull Function<? super T, ? extends BigDecimal> selector,
        @NotNull RoundingMode roundingMode
    ) {
        NullCheck.requireNonNull(selector, "selector");
        NullCheck.requireNonNull(roundingMode, "roundingMode");

        try (Enumerator<T> e = enumerator()) {
            BigDecimal sum = BigDecimal.ZERO;
            long count = 0;

            while (e.moveNext()) {
                sum = sum.add(selector.apply(e.current()));
                count++;
            }

            if (count == 0) {
                throw new ArithmeticException("Cannot compute average of an empty sequence.");
            }

            return sum.divide(BigDecimal.valueOf(count), roundingMode);
        }
    }

    /**
     * <p>
     * Casts the elements of this sequence to the specified type.
     * </p>
     *
     * <p>
     * If an element cannot be cast to the specified type, a
     * {@link ClassCastException} is thrown when the element is accessed.
     * This operation uses deferred execution.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Object> fruits = Linq.of(
     *     "mango",
     *     "apple",
     *     "lemon"
     * );
     *
     * Enumerable<String> strings = fruits.cast(String.class);
     *
     * strings.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // mango
     * // apple
     * // lemon
     * }</pre>
     *
     * @param targetType The type to cast the elements of this sequence to.
     * @param <R> The target type of the elements.
     * @return An {@code Enumerable<R>} that contains each element of this
     *     sequence cast to the specified type.
     * @throws NullPointerException if {@code targetType} is {@code null}.
     * @throws ClassCastException if an element cannot be cast to {@code R}.
     */
    <R> Enumerable<R> cast(@NotNull Class<R> targetType);

    /**
     * <p>
     * Splits the elements of this sequence into chunks of the specified size.
     * </p>
     *
     * <p>
     * Each chunk, except the last one, contains exactly {@code size} elements.
     * The last chunk contains the remaining elements and may contain fewer than
     * {@code size} elements.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(
     *     1, 2, 3, 4, 5, 6, 7, 8, 9, 10
     * );
     *
     * Enumerable<Enumerable<Integer>> chunks = numbers.chunk(3);
     *
     * chunks.forEach(chunk -> {
     *     System.out.println(chunk.toList());
     * });
     *
     * // This code produces the following output:
     * //
     * // [1, 2, 3]
     * // [4, 5, 6]
     * // [7, 8, 9]
     * // [10]
     * }</pre>
     *
     * @param size The maximum number of elements in each chunk.
     * @return An {@code Enumerable} containing the elements of this sequence
     *     split into chunks of at most {@code size} elements.
     * @throws IllegalArgumentException if {@code size} is less than {@code 1}.
     */
    Enumerable<Enumerable<T>> chunk(int size);

    /**
     * <p>
     * Concatenates this sequence with another sequence.
     * </p>
     *
     * <p>
     * The resulting sequence contains all the elements of this sequence,
     * followed by all the elements of the specified sequence. The original
     * order of the elements in both sequences is preserved.
     * </p>
     *
     * <p>
     * This operation uses deferred execution.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> cats = Linq.of(
     *     "Barley",
     *     "Boots",
     *     "Whiskers"
     * );
     *
     * Enumerable<String> dogs = Linq.of(
     *     "Bounder",
     *     "Snoopy",
     *     "Fido"
     * );
     *
     * Enumerable<String> animals = cats.concat(dogs);
     *
     * animals.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // Barley
     * // Boots
     * // Whiskers
     * // Bounder
     * // Snoopy
     * // Fido
     * }</pre>
     *
     * @param after The sequence to concatenate to this sequence.
     * @return An {@code Enumerable<T>} that contains the elements of this
     *     sequence followed by the elements of {@code after}.
     * @throws NullPointerException if {@code after} is {@code null}.
     */
    Enumerable<T> concat(@NotNull Enumerable<? extends T> after);

    /**
     * <p>
     * Determines whether this sequence contains a specified element by using
     * the default equality semantics.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "mango",
     *     "orange",
     *     "passionfruit",
     *     "grape"
     * );
     *
     * boolean contains = fruits.contains("mango");
     *
     * System.out.printf("The sequence %s contain 'mango'.%n",
     *     contains ? "does" : "does not");
     *
     * // This code produces the following output:
     * //
     * // The sequence does contain 'mango'.
     * }</pre>
     *
     * @param value The value to locate in the sequence.
     * @return {@code true} if the sequence contains an element that is equal to
     *     the specified value; otherwise, {@code false}.
     */
    default boolean contains(T value) {
        return contains(value, null);
    }

    /**
     * <p>
     * Determines whether this sequence contains a specified element by using
     * the specified equality function.
     * </p>
     *
     * <p>
     * If {@code equalator} is {@code null}, the default equality semantics are
     * used.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, int age) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", 20),
     *     new Person("Bob", 25),
     *     new Person("Charlie", 30)
     * );
     *
     * Person person = new Person("Alice", 20);
     *
     * boolean contains = people.contains(
     *     person,
     *     Equalator.comparing(Person::name)
     * );
     *
     * System.out.println("Contains Alice? " + contains);
     *
     * // This code produces the following output:
     * //
     * // Contains Alice? true
     * }</pre>
     *
     * @param value The value to locate in the sequence.
     * @param equalator The equality function used to compare elements with
     *     the specified value.
     * @return {@code true} if the sequence contains an element that is
     *     considered equal to the specified value by {@code equalator};
     *     otherwise, {@code false}.
     * @see Equalator
     * @see Equalable
     */
    default boolean contains(T value, @Nullable Equalator<? super T> equalator) {
        final Equalator<? super T> effectiveEqualator = equalator != null
            ? equalator
            : Equalator.defaultEqualator();

        try (Enumerator<T> e = enumerator()) {
            while (e.moveNext()) {
                if (effectiveEqualator.equals(e.current(), value)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * <p>
     * Returns the number of elements in this sequence.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "mango",
     *     "orange",
     *     "passionfruit",
     *     "grape"
     * );
     *
     * long count = fruits.count();
     *
     * System.out.printf("The sequence contains %d elements.%n", count);
     *
     * // This code produces the following output:
     * //
     * // The sequence contains 6 elements.
     * }</pre>
     *
     * @return The number of elements in the sequence.
     * @throws OverflowEnumerableException if the number of elements exceeds
     *     {@link Long#MAX_VALUE}.
     */
    default long count() {
        try (Enumerator<T> e = enumerator()) {
            long count = 0;
            while (e.moveNext()) {
                count++;
            }
            return count;
        }
    }

    /**
     * <p>
     * Returns the number of elements in this sequence that satisfy the
     * specified condition.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "mango",
     *     "orange",
     *     "passionfruit",
     *     "grape"
     * );
     *
     * long count = fruits.count(fruit -> fruit.length() > 5);
     *
     * System.out.printf(
     *     "There are %d fruits whose names contain more than five characters.%n",
     *     count
     * );
     *
     * // This code produces the following output:
     * //
     * // There are 3 fruits whose names contain more than five characters.
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The number of elements in the sequence that satisfy the condition
     *     specified by {@code predicate}.
     * @throws NullPointerException if {@code predicate} is {@code null}.
     * @throws OverflowEnumerableException if the number of matching elements
     *     exceeds {@link Long#MAX_VALUE}.
     */
    default long count(@NotNull Predicate<? super T> predicate) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (Enumerator<T> e = enumerator()) {
            long count = 0;
            while (e.moveNext()) {
                if (predicate.test(e.current())) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
     * <p>
     * Returns the number of elements in this sequence grouped by the key
     * returned by the specified key selector.
     * </p>
     *
     * <p>
     * Each element in the resulting sequence contains a key and the number
     * of times that key occurs in this sequence. The keys are returned in
     * the order in which they are first encountered in the source sequence.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Employee(String name, String department) {}
     *
     * Enumerable<Employee> employees = Linq.of(
     *     new Employee("Saly", "IT"),
     *     new Employee("David", "Sales"),
     *     new Employee("Mahmoud", "IT"),
     *     new Employee("Qamar", "HR"),
     *     new Employee("Sara", "IT"),
     *     new Employee("John", "HR"),
     *     new Employee("Jaffar", "Sales")
     * );
     *
     * Enumerable<Map.Entry<String, Integer>> countPerDepartment =
     *     employees.countBy(Employee::department);
     *
     * countPerDepartment.forEach(item ->
     *     System.out.printf(
     *         "Department: %s - Employees Count: %d%n",
     *         item.getKey(),
     *         item.getValue()
     *     )
     * );
     *
     * // This code produces the following output:
     * //
     * // Department: IT - Employees Count: 3
     * // Department: Sales - Employees Count: 2
     * // Department: HR - Employees Count: 2
     * }</pre>
     *
     * @param keySelector A function to extract the key for each element.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An {@code Enumerable} containing the frequency of each key
     *     occurrence in this sequence.
     * @throws NullPointerException if {@code keySelector} is {@code null}.
     * @see Pair
     */
    <K> Enumerable<Pair<K, Integer>> countBy(@NotNull Function<? super T, ? extends K> keySelector);

    /**
     * <p>
     * Returns the number of elements in this sequence grouped by the key
     * returned by the specified key selector, using the specified equality
     * function to compare keys.
     * </p>
     *
     * <p>
     * Each element in the resulting sequence contains a key and the number
     * of times that key occurs in this sequence. The keys are returned in
     * the order in which they are first encountered in the source sequence.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, String department) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", "IT"),
     *     new Person("Bob", "it"),
     *     new Person("Charlie", "HR"),
     *     new Person("David", "IT")
     * );
     *
     * Enumerable<Map.Entry<String, Integer>> counts =
     *     people.countBy(
     *         Person::department,
     *         Equalator.comparing(String::toUpperCase)
     *     );
     *
     * // The departments "IT" and "it" are considered equal.
     * }</pre>
     *
     * @param keySelector A function to extract the key for each element.
     * @param keyEqualator An equality function used to compare keys.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An {@code Enumerable} containing the frequency of each key
     *     occurrence in this sequence.
     * @throws NullPointerException if {@code keySelector} is {@code null}.
     * @throws NullPointerException if {@code keyEqualator} is {@code null}.
     * @see Pair
     */
    <K> Enumerable<Pair<K, Integer>> countBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @Nullable Equalator<? super K> keyEqualator
    );

    /**
     * <p>
     * Returns the elements of this sequence, or a singleton sequence containing
     * the specified default value if the sequence is empty.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of();
     *
     * Enumerable<String> result = fruits.defaultIfEmpty("unknown");
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // unknown
     * }</pre>
     *
     * @param defaultValue The value to return if this sequence is empty.
     * @return An {@code Enumerable<T>} containing the elements of this sequence,
     *     or a singleton sequence containing {@code defaultValue} if this
     *     sequence is empty.
     */
    Enumerable<T> defaultIfEmpty(T defaultValue);

    /**
     * <p>
     * Returns distinct elements from this sequence by using the default
     * equality function to compare values.
     * </p>
     *
     * <p>
     * The resulting sequence contains no duplicate elements. This operation
     * uses deferred execution.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(
     *     21, 46, 46, 55, 17, 21, 55, 55
     * );
     *
     * Enumerable<Integer> distinctNumbers = numbers.distinct();
     *
     * distinctNumbers.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 21
     * // 46
     * // 55
     * // 17
     * }</pre>
     *
     * @return An {@code Enumerable<T>} that contains the distinct elements
     *     from this sequence.
     */
    Enumerable<T> distinct();

    /**
     * <p>
     * Returns distinct elements from this sequence by using the specified
     * equality function (if {@code null}, use default) to compare values.
     * </p>
     *
     * <p>
     * The resulting sequence contains no duplicate elements. This operation
     * uses deferred execution.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, int code) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("apple", 9),
     *     new Product("orange", 4),
     *     new Product("apple", 9),
     *     new Product("lemon", 12)
     * );
     *
     * Enumerable<Product> distinctProducts = products.distinct(
     *     Equalator.comparing(Product::name)
     * );
     *
     * distinctProducts.forEach(System.out::println);
     *
     * // Products with the same name are considered equal.
     * }</pre>
     *
     * @param equalator The equality function used to compare elements.
     * @return An {@code Enumerable<T>} that contains the distinct elements
     *     from this sequence.
     */
    Enumerable<T> distinct(@Nullable Equalator<? super T> equalator);

    /**
     * <p>
     * Returns distinct elements from this sequence according to the specified
     * key selector function.
     * </p>
     *
     * <p>
     * The key selector is applied to each element, and elements that produce
     * equal keys are considered duplicates. Only one element is returned for
     * each distinct key.
     * </p>
     *
     * <p>
     * This operation uses deferred execution.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, String department) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", "IT"),
     *     new Person("Bob", "Sales"),
     *     new Person("Charlie", "IT"),
     *     new Person("David", "HR")
     * );
     *
     * Enumerable<Person> distinct =
     *     people.distinctBy(Person::department);
     *
     * distinct.forEach(System.out::println);
     *
     * // Each department occurs only once in the resulting sequence.
     * }</pre>
     *
     * @param keySelector A function to extract the key for each element.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An {@code Enumerable<T>} that contains distinct elements from
     *     this sequence according to their selected keys.
     * @throws NullPointerException if {@code keySelector} is {@code null}.
     */
    <K> Enumerable<T> distinctBy(@NotNull Function<? super T, ? extends K> keySelector);

    /**
     * <p>
     * Returns distinct elements from this sequence according to the specified
     * key selector function and equality function.
     * </p>
     *
     * <p>
     * The key selector is applied to each element, and elements that produce
     * equal keys according to {@code keyEqualator} are considered duplicates.
     * Only one element is returned for each distinct key.
     * </p>
     *
     * <p>
     * If {@code keyEqualator} is {@code null}, the default equality function
     * is used to compare keys.
     * </p>
     *
     * <p>
     * This operation uses deferred execution.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, String department) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", "IT"),
     *     new Person("Bob", "it"),
     *     new Person("Charlie", "HR"),
     *     new Person("David", "IT")
     * );
     *
     * Enumerable<Person> distinct =
     *     people.distinctBy(
     *         Person::department,
     *         Equalator.comparing(String::toUpperCase)
     *     );
     *
     * // "IT" and "it" are considered equal by the specified equalator.
     * }</pre>
     *
     * @param keySelector A function to extract the key for each element.
     * @param keyEqualator An equality function used to compare the selected keys.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An {@code Enumerable<T>} that contains distinct elements from
     *     this sequence according to their selected keys.
     * @throws NullPointerException if {@code keySelector} is {@code null}.
     */
    <K> Enumerable<T> distinctBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @Nullable Equalator<? super K> keyEqualator
    );


    /**
     * <p>
     * Returns the element at the specified zero-based index in this sequence.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "mango",
     *     "orange",
     *     "grape"
     * );
     *
     * String fruit = fruits.elementAt(2);
     *
     * System.out.printf("The element at index 2 is %s.%n", fruit);
     *
     * // This code produces the following output:
     * //
     * // The element at index 2 is mango.
     * }</pre>
     *
     * @param index The zero-based index of the element to retrieve.
     * @return The element at the specified position in the sequence.
     * @throws IndexOutOfBoundsException if {@code index} is less than zero or
     *     greater than or equal to the number of elements in the sequence.
     */
    default T elementAt(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("Index cannot be negative: " + index);

        try (Enumerator<T> e = enumerator()) {
            int currentIndex = 0;
            while (e.moveNext()) {
                if (currentIndex == index) {
                    return e.current();
                }
                currentIndex++;
            }
            throw new IndexOutOfBoundsException(
                "Index " + index + " is out of bounds for a sequence of length " + currentIndex
            );
        }
    }

    /**
     * <p>
     * Returns the element at the specified zero-based index in this sequence,
     * or {@code null} if the index is outside the bounds of the sequence.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "mango",
     *     "orange",
     *     "grape"
     * );
     *
     * String fruit = fruits.elementAtOrNull(10);
     *
     * System.out.printf("The element at index 10 is %s.%n", fruit);
     *
     * // This code produces the following output:
     * //
     * // The element at index 10 is null.
     * }</pre>
     *
     * @param index The zero-based index of the element to retrieve.
     * @return The element at the specified position in the sequence, or
     *     {@code null} if the index is outside the bounds of the sequence.
     */
    @Nullable
    default T elementAtOrNull(int index) {
        return elementAtOrDefault(index, null);
    }

    /**
     * <p>
     * Returns the element at the specified zero-based index in this sequence,
     * or {@code null} if the index is outside the bounds of the sequence.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple",
     *     "banana",
     *     "mango",
     *     "orange",
     *     "grape"
     * );
     *
     * String fruit = fruits.elementAtOrDefault(10, "watermelon");
     *
     * System.out.printf("The element at index 10 is %s.%n", fruit);
     *
     * // This code produces the following output:
     * //
     * // The element at index 10 is watermelon.
     * }</pre>
     *
     * @param index The zero-based index of the element to retrieve.
     * @param defaultElement The default element will to be returned if the index is out of bounds.
     * @return The element at the specified position in the sequence, or
     *     {@code null} if the index is outside the bounds of the sequence.
     */
    @Nullable
    default T elementAtOrDefault(int index, @Nullable T defaultElement) {
        if (index < 0) return defaultElement;

        try (Enumerator<T> e = enumerator()) {
            int currentIndex = 0;
            while (e.moveNext()) {
                if (currentIndex == index) {
                    return e.current();
                }
                currentIndex++;
            }
            return defaultElement;
        }
    }

    /**
     * <p>
     * Produces the set difference of two sequences by using the default
     * equality semantics to compare values.
     * </p>
     *
     * <p>
     * The returned sequence contains the unique elements from this sequence
     * that do not occur in the specified sequence.
     * </p>
     *
     * <p>
     * This method uses deferred execution. The query represented by the
     * returned sequence is not executed until the sequence is enumerated.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Double> numbers1 = Linq.of(
     *     2.0, 2.0, 2.1, 2.2, 2.3, 2.3, 2.4, 2.5
     * );
     *
     * Enumerable<Double> numbers2 = Linq.of(2.2);
     *
     * Enumerable<Double> onlyInFirstSet = numbers1.except(numbers2);
     *
     * onlyInFirstSet.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 2.0
     * // 2.1
     * // 2.3
     * // 2.4
     * // 2.5
     * }</pre>
     *
     * @param other The sequence whose elements will be excluded from this sequence.
     * @return A sequence that contains the set difference of the elements
     *         of this sequence and {@code other}.
     */
    Enumerable<T> except(@NotNull Enumerable<? extends T> other);

    /**
     * <p>
     * Produces the set difference of two sequences by using the specified
     * {@link Equalator} to compare values.
     * </p>
     *
     * <p>
     * The returned sequence contains the unique elements from this sequence
     * that do not occur in the specified sequence according to the supplied
     * equality semantics.
     * </p>
     *
     * <p>
     * If {@code equalator} is {@code null}, the default equality semantics
     * are used.
     * </p>
     *
     * <p>
     * This method uses deferred execution. The query represented by the
     * returned sequence is not executed until the sequence is enumerated.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, int code) {}
     *
     * Enumerable<Product> products1 = Linq.of(
     *     new Product("apple", 9),
     *     new Product("orange", 4),
     *     new Product("lemon", 12)
     * );
     *
     * Enumerable<Product> products2 = Linq.of(
     *     new Product("apple", 9)
     * );
     *
     * Equalator<Product> productEqualator =
     *     Equalator.comparing(
     *         Product::code,
     *         Equalator.defaultEqualator()
     *     );
     *
     * Enumerable<Product> except =
     *     products1.except(products2, productEqualator);
     *
     * except.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // Product[name=orange, code=4]
     * // Product[name=lemon, code=12]
     * }</pre>
     *
     * @param other The sequence whose elements will be excluded from this sequence.
     * @param equalator The equality function used to compare elements.
     * @return A sequence that contains the set difference of the elements
     *         of this sequence and {@code other}.
     */
    Enumerable<T> except(
        @NotNull Enumerable<? extends T> other,
        @Nullable Equalator<? super T> equalator
    );

    /**
     * <p>
     * Produces the set difference of two sequences according to a specified
     * key selector function.
     * </p>
     *
     * <p>
     * The key selector is used to extract a key from each element of this
     * sequence. Elements whose keys occur in the specified sequence of keys
     * are excluded from the result.
     * </p>
     *
     * <p>
     * Only unique elements are returned.
     * </p>
     *
     * <p>
     * This method uses deferred execution. The query represented by the
     * returned sequence is not executed until the sequence is enumerated.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, int code) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("apple", 9),
     *     new Product("orange", 4),
     *     new Product("lemon", 12)
     * );
     *
     * Enumerable<Integer> excludedCodes = Linq.of(9);
     *
     * Enumerable<Product> result =
     *     products.exceptBy(excludedCodes, Product::code);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // Product[name=orange, code=4]
     * // Product[name=lemon, code=12]
     * }</pre>
     *
     * @param <K> The type of the key.
     * @param other The sequence of keys whose corresponding elements
     *               will be excluded from this sequence.
     * @param keySelector A function used to extract the key from each element.
     * @return A sequence that contains the set difference of the elements
     *         of this sequence and the elements identified by the keys in
     *         {@code other}.
     */
    <K> Enumerable<T> exceptBy(
        @NotNull Enumerable<? extends K> other,
        @NotNull Function<? super T, ? extends K> keySelector
    );

    /**
     * <p>
     * Produces the set difference of two sequences according to a specified
     * key selector function and equality function.
     * </p>
     *
     * <p>
     * The key selector is used to extract a key from each element of this
     * sequence. The supplied {@link Equalator} is then used to determine
     * whether the extracted key occurs in the specified sequence of keys.
     * </p>
     *
     * <p>
     * Only unique elements are returned.
     * </p>
     *
     * <p>
     * This method uses deferred execution. The query represented by the
     * returned sequence is not executed until the sequence is enumerated.
     * </p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, int code) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("apple", 9),
     *     new Product("orange", 4),
     *     new Product("lemon", 12)
     * );
     *
     * Enumerable<Integer> excludedCodes = Linq.of(9);
     *
     * Enumerable<Product> result =
     *     products.exceptBy(
     *         excludedCodes,
     *         Product::code,
     *         Equalator.defaultEqualator()
     *     );
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // Product[name=orange, code=4]
     * // Product[name=lemon, code=12]
     * }</pre>
     *
     * @param <K> The type of the key.
     * @param second The sequence of keys whose corresponding elements
     *               will be excluded from this sequence.
     * @param keySelector A function used to extract the key from each element.
     * @param equalator The equality function used to compare keys.
     * @return A sequence that contains the set difference of the elements
     *         of this sequence and the elements identified by the keys in
     *         {@code second}.
     */
    <K> Enumerable<T> exceptBy(
        @NotNull Enumerable<? extends K> second,
        @NotNull Function<? super T, ? extends K> keySelector,
        @Nullable Equalator<? super K> equalator
    );

    /**
     * <p>Returns the first element in the sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple", "banana", "orange"
     * );
     *
     * String firstFruit = fruits.first();
     *
     * System.out.println(firstFruit);
     *
     * // This code produces the following output:
     * //
     * // apple
     * }</pre>
     *
     * @return The first element in the sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    default T first() {
        try (Enumerator<T> e = enumerator()) {
            if (e.moveNext()) {
                return e.current();
            }
            throw new NoSuchElementException("The sequence contains no elements.");
        }
    }

    /**
     * <p>Returns the first element in the sequence that satisfies a specified
     * condition.</p>
     *
     * <p>The search stops as soon as an element that satisfies
     * {@code predicate} is found.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple", "banana", "orange", "passionfruit", "grape"
     * );
     *
     * String firstLongFruit = fruits.first(
     *     fruit -> fruit.length() > 6
     * );
     *
     * System.out.printf(
     *     "The first fruit with more than six characters is %s.%n",
     *     firstLongFruit
     * );
     *
     * // This code produces the following output:
     * //
     * // The first fruit with more than six characters is banana.
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The first element in the sequence that passes the test in
     *         {@code predicate}.
     * @throws NoSuchElementException If no element satisfies
     *         {@code predicate}.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    default T first(@Nullable Predicate<? super T> predicate) {
        if (predicate == null) throw new NullPointerException("predicate is null");


        try (Enumerator<T> e = enumerator()) {
            while (e.moveNext()) {
                T current = e.current();
                if (predicate.test(current)) {
                    return current;
                }
            }
            throw new NoSuchElementException("No element satisfies the condition.");
        }
    }


    /**
     * <p>Returns the first element of the sequence, or {@code null} if the
     * sequence contains no elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple", "banana", "orange"
     * );
     *
     * String firstFruit = fruits.firstOrDefault();
     *
     * System.out.println(firstFruit);
     *
     * // This code produces the following output:
     * //
     * // apple
     * }</pre>
     *
     * @return The first element in the sequence, or {@code null} if the
     *         sequence contains no elements.
     */
    @Nullable
    default T firstOrNull() {
        try (Enumerator<T> e = enumerator()) {
            if (e.moveNext()) {
                return e.current();
            }
            return null;
        }
    }

    /**
     * <p>Returns the first element in the sequence that satisfies a specified
     * condition, or {@code null} if no such element is found.</p>
     *
     * <p>The search stops as soon as an element that satisfies
     * {@code predicate} is found.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple", "banana", "orange", "grape"
     * );
     *
     * String fruit = fruits.firstOrDefault(
     *     value -> value.length() > 10
     * );
     *
     * System.out.println(fruit);
     *
     * // This code produces the following output:
     * //
     * // null
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The first element that satisfies {@code predicate}, or
     *         {@code null} if no such element is found.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @Nullable
    default T firstOrNull(@NotNull Predicate<? super T> predicate) {
        NullCheck.requireNonNull(predicate, "predicate");
        try (Enumerator<T> e = enumerator()) {
            while (e.moveNext()) {
                T current = e.current();
                if (predicate.test(current)) {
                    return current;
                }
            }
            return null;
        }
    }

    /**
     * <p>Returns the first element in the sequence, or the specified default
     * value if the sequence contains no elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.empty();
     *
     * String first = fruits.firstOrDefault("unknown");
     *
     * System.out.println(first);
     *
     * // This code produces the following output:
     * //
     * // unknown
     * }</pre>
     *
     * @param defaultValue The first element in the sequence, or {@code defaultValue}
     *         if the sequence contains no elements.
     * @return The first element in the sequence, or {@code defaultValue`
     *         if the sequence contains no elements.
     */
    default T firstOrDefault(T defaultValue) {
        try (Enumerator<T> e = enumerator()) {
            if (e.moveNext()) {
                return e.current();
            }
            return defaultValue;
        }
    }

    /**
     * <p>Returns the first element in the sequence that satisfies a
     * specified condition, or the specified default value if no such
     * element is found.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of(
     *     "apple", "banana", "orange"
     * );
     *
     * String first = fruits.firstOrDefault(
     *     fruit -> fruit.length() > 10,
     *     "unknown"
     * );
     *
     * System.out.println(first);
     *
     * // This code produces the following output:
     * //
     * // unknown
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @param defaultValue The first element in the sequence, or {@code defaultValue}
     *         if the sequence contains no elements.
     * @return The first element that satisfies {@code predicate}, or
     *         {@code defaultValue} if no such element is found.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    default T firstOrDefault(
        @NotNull Predicate<? super T> predicate,
        T defaultValue
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (Enumerator<T> e = enumerator()) {
            while (e.moveNext()) {
                T current = e.current();
                if (predicate.test(current)) {
                    return current;
                }
            }
            return defaultValue;
        }
    }

    /**
     * <p>Groups the elements of the sequence according to a specified
     * key selector function.</p>
     *
     * <p>Each group contains the elements that have the same key.
     * Groups are produced according to the order in which their keys
     * first appear in the source sequence, and elements within each
     * group retain their original order.</p>
     *
     * <p>The default equality semantics are used to compare keys.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> the type of the group key
     * @param keySelector the function used to extract the key from each element
     * @return a sequence containing one {@link Groupable} for each distinct key
     * @throws NullPointerException if {@code keySelector} is {@code null}
     */
    <K> Enumerable<Groupable<K, T>> groupBy(
        @NotNull Function<? super T, ? extends K> keySelector
    );

    /**
     * <p>Groups the elements of the sequence according to a specified
     * key selector function and compares keys using the specified
     * {@link Equalator}.</p>
     *
     * <p>Groups are produced according to the order in which their keys
     * first appear in the source sequence, and elements within each
     * group retain their original order.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> the type of the group key
     * @param keySelector the function used to extract the key from each element
     * @param equalator the equality function used to compare keys
     * @return a sequence containing one {@link Groupable} for each distinct key
     * @throws NullPointerException if {@code keySelector} or {@code equalator}
     *         is {@code null}
     */
    <K> Enumerable<Groupable<K, T>> groupBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @Nullable Equalator<? super K> equalator
    );

    /**
     * <p>Groups the elements of the sequence according to a specified
     * key selector function and projects the elements of each group
     * by using a specified element selector.</p>
     *
     * <p>Each resulting group contains the projected elements rather than
     * the original source elements.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> the type of the group key
     * @param <E> the type of the grouped elements
     * @param keySelector the function used to extract the key from each element
     * @param elementSelector the function used to transform each source element
     *                        into an element of its group
     * @return a sequence containing one {@link Groupable} for each distinct key
     * @throws NullPointerException if {@code keySelector} or
     *         {@code elementSelector} is {@code null}
     */
    <K, E> Enumerable<Groupable<K, E>> groupBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends E> elementSelector
    );

    /**
     * <p>Groups the elements of the sequence according to a specified
     * key selector function, projects the elements of each group by using
     * a specified element selector, and compares keys using the specified
     * {@link Equalator}.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> the type of the group key
     * @param <E> the type of the grouped elements
     * @param keySelector the function used to extract the key from each element
     * @param elementSelector the function used to transform each source element
     *                        into an element of its group
     * @param equalator the equality function used to compare keys
     * @return a sequence containing one {@link Groupable} for each distinct key
     * @throws NullPointerException if {@code keySelector},
     *         {@code elementSelector}, or {@code equalator} is {@code null}
     */
    <K, E> Enumerable<Groupable<K, E>> groupBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends E> elementSelector,
        @Nullable Equalator<? super K> equalator
    );

    /**
     * <p>Groups the elements of the sequence according to a specified
     * key selector function and creates a result value from each group
     * and its key.</p>
     *
     * <p>The {@code resultSelector} receives the key of each group and
     * an {@link Enumerable} containing the elements of that group.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> the type of the group key
     * @param <R> the type of the result value
     * @param keySelector the function used to extract the key from each element
     * @param resultSelector the function used to create a result from
     *                       a group key and its elements
     * @return a sequence containing one result value for each group
     * @throws NullPointerException if {@code keySelector} or
     *         {@code resultSelector} is {@code null}
     */
    <K, R> Enumerable<R> groupToResult(
        @NotNull Function<? super T, ? extends K> keySelector,
        @Nullable BinFunction<? super K, ? super Enumerable<T>, ? extends R> resultSelector
    );

    /**
     * <p>Groups the elements of the sequence according to a specified
     * key selector function, compares keys using the specified
     * {@link Equalator}, and creates a result value from each group
     * and its key.</p>
     *
     * <p>The {@code resultSelector} receives the key of each group and
     * an {@link Enumerable} containing the elements of that group.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> the type of the group key
     * @param <R> the type of the result value
     * @param keySelector the function used to extract the key from each element
     * @param resultSelector the function used to create a result from
     *                       a group key and its elements
     * @param equalator the equality function used to compare keys
     * @return a sequence containing one result value for each group
     * @throws NullPointerException if {@code keySelector},
     *         {@code resultSelector}, or {@code equalator} is {@code null}
     */
    <K, R> Enumerable<R> groupToResult(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull BinFunction<? super K, ? super Enumerable<T>, ? extends R> resultSelector,
        @Nullable Equalator<? super K> equalator
    );

    /**
     * <p>Groups the elements of the sequence according to a specified
     * key selector function, projects the elements of each group by using
     * a specified element selector, and creates a result value from each
     * group and its key.</p>
     *
     * <p>The {@code resultSelector} receives the key of each group and
     * an {@link Enumerable} containing the projected elements of that group.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> the type of the group key
     * @param <E> the type of the grouped elements
     * @param <R> the type of the result value
     * @param keySelector the function used to extract the key from each element
     * @param elementSelector the function used to transform each source element
     *                        into an element of its group
     * @param resultSelector the function used to create a result from
     *                       a group key and its elements
     * @return a sequence containing one result value for each group
     * @throws NullPointerException if {@code keySelector},
     *         {@code elementSelector}, or {@code resultSelector} is {@code null}
     */
    <K, E, R> Enumerable<R> groupToResult(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends E> elementSelector,
        @NotNull BinFunction<? super K, ? super Enumerable<E>, ? extends R> resultSelector
    );

    /**
     * <p>Groups the elements of the sequence according to a specified
     * key selector function, projects the elements of each group by using
     * a specified element selector, compares keys using the specified
     * {@link Equalator}, and creates a result value from each group
     * and its key.</p>
     *
     * <p>The {@code resultSelector} receives the key of each group and
     * an {@link Enumerable} containing the projected elements of that group.</p>
     *
     * <p>Groups are produced according to the order in which their keys
     * first appear in the source sequence, and elements within each
     * group retain their original order.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> the type of the group key
     * @param <E> the type of the grouped elements
     * @param <R> the type of the result value
     * @param keySelector the function used to extract the key from each element
     * @param elementSelector the function used to transform each source element
     *                        into an element of its group
     * @param resultSelector the function used to create a result from
     *                       a group key and its elements
     * @param equalator the equality function used to compare keys
     * @return a sequence containing one result value for each group
     * @throws NullPointerException if {@code keySelector},
     *         {@code elementSelector}, {@code resultSelector}, or
     *         {@code equalator} is {@code null}
     */
    <K, E, R> Enumerable<R> groupToResult(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends E> elementSelector,
        @NotNull BinFunction<? super K, ? super Enumerable<E>, ? extends R> resultSelector,
        @Nullable Equalator<? super K> equalator
    );

    /**
     * <p>Correlates the elements of this sequence with the elements of
     * another sequence based on matching keys, and groups the matching
     * elements from the second sequence for each element of this sequence.</p>
     *
     * <p>The default equality semantics are used to compare keys.</p>
     *
     * <p>The {@code resultSelector} is invoked once for each element of
     * this sequence, including elements for which no matching element
     * exists in {@code inner}.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <I> The type of the elements in the inner sequence.
     * @param <K> The type of the keys.
     * @param <R> The type of the result elements.
     * @param inner The sequence to join with this sequence.
     * @param outerKeySelector A function used to extract the join key
     *                         from each element of this sequence.
     * @param innerKeySelector A function used to extract the join key
     *                         from each element of {@code inner}.
     * @param resultSelector A function used to create a result from an
     *                       element of this sequence and its matching
     *                       elements from {@code inner}.
     * @return A sequence containing one result element for each element
     *         in this sequence.
     * @throws NullPointerException If {@code inner}, {@code outerKeySelector},
     *         {@code innerKeySelector}, or {@code resultSelector} is {@code null}.
     */
    <K, I, R> Enumerable<R> groupJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T, ? super Enumerable<I>, ? extends R> resultSelector
    );

    /**
     * <p>Correlates the elements of this sequence with the elements of
     * another sequence based on matching keys, and groups the matching
     * elements from the second sequence for each element of this sequence.</p>
     *
     * <p>The {@code resultSelector} is invoked once for each element of
     * this sequence. It receives the current element and an
     * {@link Enumerable} containing all elements from {@code inner} whose
     * keys are equal to the key of the current element.</p>
     *
     * <p>If no elements in {@code inner} match an element in this sequence,
     * the result selector is still invoked with an empty sequence.</p>
     *
     * <p>The order of the elements in this sequence is preserved, as is
     * the order of matching elements from {@code inner} for each element.</p>
     *
     * <p>The specified {@link Equalator} is used to compare keys.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, int id) {}
     *
     * record Pet(String name, int ownerId) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", 1),
     *     new Person("Bob", 2),
     *     new Person("Charlie", 3)
     * );
     *
     * Enumerable<Pet> pets = Linq.of(
     *     new Pet("Fluffy", 2),
     *     new Pet("Mittens", 2),
     *     new Pet("Buddy", 1)
     * );
     *
     * Enumerable<String> result = people.groupJoin(
     *     pets,
     *     Person::id,
     *     Pet::ownerId,
     *     (person, personPets) ->
     *         person.name() + ": "
     *             + personPets
     *                 .select(Pet::name)
     *                 .join(", "),
     *     Equalator.defaultEqualator()
     * );
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // Alice: Buddy
     * // Bob: Fluffy, Mittens
     * // Charlie:
     * }</pre>
     *
     * @param <I> The type of the elements in the inner sequence.
     * @param <K> The type of the keys.
     * @param <R> The type of the result elements.
     * @param inner The sequence to join with this sequence.
     * @param outerKeySelector A function used to extract the join key
     *                         from each element of this sequence.
     * @param innerKeySelector A function used to extract the join key
     *                         from each element of {@code inner}.
     * @param resultSelector A function used to create a result from an
     *                       element of this sequence and its matching
     *                       elements from {@code inner}.
     * @param equalator The equality function used to compare keys.
     * @return A sequence containing one result element for each element
     *         in this sequence.
     * @throws NullPointerException If {@code inner}, {@code outerKeySelector},
     *         {@code innerKeySelector}, {@code resultSelector}, or
     *         {@code equalator} is {@code null}.
     */
    <K, I, R> Enumerable<R> groupJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T, ? super Enumerable<I>, ? extends R> resultSelector,
        @Nullable Equalator<? super K> equalator
    );

    /**
     * <p>Produces the set intersection of this sequence and another sequence
     * by using the default equality semantics to compare elements.</p>
     *
     * <p>The returned sequence contains distinct elements that occur in both
     * sequences, in the order in which they first appear in this sequence.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> first = Linq.of(1, 2, 2, 3, 4);
     * Enumerable<Integer> other = Linq.of(2, 3, 3, 5);
     *
     * Enumerable<Integer> result = first.intersect(other);
     *
     * // result: 2, 3
     * }</pre>
     *
     * @param other The sequence whose elements are also searched for
     *               in this sequence.
     * @return A sequence that contains the distinct elements that occur
     *         in both sequences.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    Enumerable<T> intersect(
        @NotNull Enumerable<? extends T> other
    );

    /**
     * <p>Produces the set intersection of this sequence and another sequence
     * by using the specified {@link Equalator} to compare elements.</p>
     *
     * <p>The returned sequence contains distinct elements that occur in both
     * sequences, in the order in which they first appear in this sequence.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> first =
     *     Linq.of("Apple", "banana", "APPLE", "orange");
     *
     * Enumerable<String> second =
     *     Linq.of("apple", "BANANA");
     *
     * Enumerable<String> result =
     *     first.intersect(
     *         second,
     *         Equalator.comparing(String::toLowerCase)
     *     );
     *
     * // result: Apple, banana
     * }</pre>
     *
     * @param second The sequence whose elements are also searched for
     *               in this sequence.
     * @param equalator The equality function used to compare elements.
     * @return A sequence that contains the distinct elements that occur
     *         in both sequences.
     * @throws NullPointerException If {@code second} or {@code equalator}
     *         is {@code null}.
     */
    Enumerable<T> intersect(
        @NotNull Enumerable<? extends T> second,
        @Nullable Equalator<? super T> equalator
    );

    /**
     * <p>Produces the set intersection of this sequence and another sequence
     * according to a specified key selector function.</p>
     *
     * <p>The elements of this sequence are compared with the elements of
     * {@code other} by using the keys produced by {@code keySelector}.</p>
     *
     * <p>The returned sequence contains distinct elements from this sequence
     * whose keys occur in {@code other}, in the order in which they first
     * appear in this sequence.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, int id) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", 1),
     *     new Person("Bob", 2),
     *     new Person("Charlie", 3)
     * );
     *
     * Enumerable<Integer> ids = Linq.of(2, 3);
     *
     * Enumerable<Person> result =
     *     people.intersectBy(ids, Person::id);
     *
     * // result:
     * // Bob
     * // Charlie
     * }</pre>
     *
     * @param <K> The type of the key.
     * @param other A sequence containing the keys to compare against.
     * @param keySelector A function used to extract the key from each
     *                    element of this sequence.
     * @return A sequence containing the distinct elements whose keys
     *         occur in {@code other}.
     * @throws NullPointerException If {@code other} or {@code keySelector}
     *         is {@code null}.
     */
    <K> Enumerable<T> intersectBy(
        @NotNull Enumerable<? extends K> other,
        @NotNull Function<? super T, ? extends K> keySelector
    );

    /**
     * <p>Produces the set intersection of this sequence and another sequence
     * according to a specified key selector function and the specified
     * {@link Equalator}.</p>
     *
     * <p>The elements of this sequence are compared with the elements of
     * {@code second} by using the keys produced by {@code keySelector} and
     * compared by {@code equalator}.</p>
     *
     * <p>The returned sequence contains distinct elements from this sequence
     * whose keys occur in {@code second}, in the order in which they first
     * appear in this sequence.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, String code) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", "A01"),
     *     new Person("Bob", "B01"),
     *     new Person("Charlie", "C01")
     * );
     *
     * Enumerable<String> codes = Linq.of("a01", "C01");
     *
     * Enumerable<Person> result =
     *     people.intersectBy(
     *         codes,
     *         Person::code,
     *         Equalator.comparing(String::toLowerCase)
     *     );
     *
     * // result:
     * // Alice
     * // Charlie
     * }</pre>
     *
     * @param <K> The type of the key.
     * @param second A sequence containing the keys to compare against.
     * @param keySelector A function used to extract the key from each
     *                    element of this sequence.
     * @param equalator The equality function used to compare keys.
     * @return A sequence containing the distinct elements whose keys
     *         occur in {@code second}.
     * @throws NullPointerException If {@code second}, {@code keySelector},
     *         or {@code equalator} is {@code null}.
     */
    <K> Enumerable<T> intersectBy(
        @NotNull Enumerable<? extends K> second,
        @NotNull Function<? super T, ? extends K> keySelector,
        @Nullable Equalator<? super K> equalator
    );

    /**
     * <p>Correlates the elements of this sequence with the elements of
     * another sequence based on matching keys, and produces a result value
     * for each matching pair.</p>
     *
     * <p>This method performs an inner equijoin. Only elements from this
     * sequence that have at least one matching element in {@code inner}
     * are included in the result.</p>
     *
     * <p>The {@code resultSelector} is invoked for each matching pair of
     * elements.</p>
     *
     * <p>The order of the elements in this sequence is preserved, and for
     * each element of this sequence, the order of its matching elements
     * from {@code inner} is preserved.</p>
     *
     * <p>The default equality semantics are used to compare keys.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, int id) {}
     * record Pet(String name, int ownerId) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", 1),
     *     new Person("Bob", 2),
     *     new Person("Charlie", 3)
     * );
     *
     * Enumerable<Pet> pets = Linq.of(
     *     new Pet("Buddy", 1),
     *     new Pet("Fluffy", 2),
     *     new Pet("Mittens", 2)
     * );
     *
     * Enumerable<String> result = people.join(
     *     pets,
     *     Person::id,
     *     Pet::ownerId,
     *     (person, pet) ->
     *         person.name() + " - " + pet.name()
     * );
     *
     * // result:
     * // Alice - Buddy
     * // Bob - Fluffy
     * // Bob - Mittens
     * }</pre>
     *
     * @param <K> The type of the join key.
     * @param <I> The type of the elements in the inner sequence.
     * @param <R> The type of the result elements.
     * @param inner The sequence to join with this sequence.
     * @param outerKeySelector A function used to extract the join key from
     *                         each element of this sequence.
     * @param innerKeySelector A function used to extract the join key from
     *                         each element of {@code inner}.
     * @param resultSelector A function used to create a result from two
     *                       matching elements.
     * @return A sequence containing the result elements obtained by
     *         performing an inner join on the two sequences.
     * @throws NullPointerException If {@code inner},
     *         {@code outerKeySelector}, {@code innerKeySelector}, or
     *         {@code resultSelector} is {@code null}.
     */
    <K, I, R> Enumerable<R> join(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T, ? super I, ? extends R> resultSelector
    );

    /**
     * <p>Correlates the elements of this sequence with the elements of
     * another sequence based on matching keys, and produces a result value
     * for each matching pair.</p>
     *
     * <p>This method performs an inner equijoin. Only elements from this
     * sequence that have at least one matching element in {@code inner}
     * are included in the result.</p>
     *
     * <p>The specified {@link Equalator} is used to compare join keys.</p>
     *
     * <p>The {@code resultSelector} is invoked for each matching pair of
     * elements.</p>
     *
     * <p>The order of the elements in this sequence is preserved, and for
     * each element of this sequence, the order of its matching elements
     * from {@code inner} is preserved.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> The type of the join key.
     * @param <I> The type of the elements in the inner sequence.
     * @param <R> The type of the result elements.
     * @param inner The sequence to join with this sequence.
     * @param outerKeySelector A function used to extract the join key from
     *                         each element of this sequence.
     * @param innerKeySelector A function used to extract the join key from
     *                         each element of {@code inner}.
     * @param resultSelector A function used to create a result from two
     *                       matching elements.
     * @param equalator The equality function used to compare join keys.
     * @return A sequence containing the result elements obtained by
     *         performing an inner join on the two sequences.
     * @throws NullPointerException If {@code inner},
     *         {@code outerKeySelector}, {@code innerKeySelector},
     *         {@code resultSelector}, or {@code equalator} is {@code null}.
     */
    <K, I, R> Enumerable<R> join(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T, ? super I, ? extends R> resultSelector,
        @Nullable Equalator<? super K> equalator
    );

    /**
     * <p>Returns the last element of a sequence.</p>
     *
     * <p>This method throws an exception if the sequence contains no elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * String last = fruits.last();
     *
     * System.out.println(last);
     *
     * // This code produces the following output:
     * //
     * // grape
     * }</pre>
     *
     * @return The last element in the sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    @NotNull
    default T last() {
        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            T result = enumerator.current();
            while (enumerator.moveNext()) {
                result = enumerator.current();
            }
            return result;
        }
    }


    /**
     * <p>Returns the last element of a sequence that satisfies a specified
     * condition.</p>
     *
     * <p>This method throws an exception if no element satisfies the
     * condition.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * String last =
     *     fruits.last(fruit -> fruit.length() > 5);
     *
     * System.out.println(last);
     *
     * // This code produces the following output:
     * //
     * // orange
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The last element in the sequence that satisfies the condition.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     * @throws NoSuchElementException If no element satisfies the condition or the sequence contains no elements.
     */
    @NotNull
    default T last(
        @NotNull Predicate<? super T> predicate
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (Enumerator<T> enumerator = enumerator()) {
            T result = null;
            boolean found = false;

            while (enumerator.moveNext()) {
                T current = enumerator.current();
                if (predicate.test(current)) {
                    result = current;
                    found = true;
                }
            }

            if (!found) {
                throw new NoSuchElementException("No element satisfies the condition.");
            }
            return result;
        }
    }


    /**
     * <p>Returns the last element of a sequence, or a default value if the
     * sequence contains no elements.</p>
     *
     * <p>If the sequence contains no elements, {@code null} is returned.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * String last =
     *     fruits.lastOrNull();
     *
     * System.out.println(last);
     *
     * // This code produces the following output:
     * //
     * // grape
     * }</pre>
     *
     * @return The last element in the sequence, or {@code null} if the
     *         sequence contains no elements.
     */
    @Nullable
    default T lastOrNull() {
        return lastOrDefault(null);
    }

    /**
     * <p>Returns the last element of a sequence that satisfies a specified
     * condition, or a default value if no such element is found.</p>
     *
     * <p>If no element satisfies the condition, {@code null} is returned.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * String last =
     *     fruits.lastOrNull(fruit -> fruit.length() > 6);
     *
     * System.out.println(last);
     *
     * // This code produces the following output:
     * //
     * // orange
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return The last element that satisfies the condition, or {@code null}
     *         if no such element is found.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @Nullable
    default T lastOrNull(
        @NotNull Predicate<? super T> predicate
    ) {
        return lastOrDefault(predicate, null);
    }

    /**
     * <p>Returns the last element of a sequence, or the specified default
     * value if the sequence contains no elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * String last = fruits.lastOrDefault("unknown");
     *
     * System.out.println(last);
     *
     * // This code produces the following output:
     * //
     * // grape
     * }</pre>
     *
     * @param defaultElement The value to return if the sequence contains
     *                        no elements.
     * @return The last element in the sequence, or {@code defaultElement}
     *         if the sequence contains no elements.
     */
    @Nullable
    default T lastOrDefault(@Nullable T defaultElement) {
        try (Enumerator<T> enumerator = enumerator()) {
            T result = defaultElement;

            while (enumerator.moveNext()) {
                result = enumerator.current();
            }
            return result;
        }
    }


    /**
     * <p>Returns the last element of a sequence that satisfies a specified
     * condition, or the specified default value if no such element is found.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * String last = fruits.lastOrDefault(
     *     fruit -> fruit.length() > 6,
     *     "unknown"
     * );
     *
     * System.out.println(last);
     *
     * // This code produces the following output:
     * //
     * // orange
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @param defaultElement The value to return if no element satisfies
     *                       the condition.
     * @return The last element that satisfies the condition, or
     *         {@code defaultElement} if no such element is found.
     * @throws NullPointerException If {@code predicate} is {@code null}.
     */
    @Nullable
    default T lastOrDefault(
        @NotNull Predicate<? super T> predicate,
        @Nullable T defaultElement
    ) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (Enumerator<T> enumerator = enumerator()) {
            T result = defaultElement;
            while (enumerator.moveNext()) {
                T current = enumerator.current();
                if (predicate.test(current)) {
                    result = current;
                }
            }
            return result;
        }
    }

    /**
     * <p>Correlates the elements of this sequence with the elements of
     * another sequence based on matching keys and produces a result element
     * for each element of this sequence.</p>
     *
     * <p>This method performs a left outer equijoin. Every element in this
     * sequence is included in the result, regardless of whether a matching
     * element exists in {@code inner}.</p>
     *
     * <p>If an element in this sequence has no matching element in
     * {@code inner}, {@code resultSelector} is invoked with {@code null}
     * as its inner element.</p>
     *
     * <p>If multiple elements in {@code inner} match an element in this
     * sequence, {@code resultSelector} is invoked once for each matching
     * element.</p>
     *
     * <p>The default equality semantics are used to compare keys.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, int id) {}
     * record Pet(String name, int ownerId) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", 1),
     *     new Person("Bob", 2),
     *     new Person("Charlie", 3)
     * );
     *
     * Enumerable<Pet> pets = Linq.of(
     *     new Pet("Buddy", 1),
     *     new Pet("Fluffy", 2)
     * );
     *
     * Enumerable<String> result = people.leftJoin(
     *     pets,
     *     Person::id,
     *     Pet::ownerId,
     *     (person, pet) ->
     *         person.name() + " - "
     *             + (pet == null ? "No pet" : pet.name())
     * );
     *
     * // This code produces the following output:
     * //
     * // Alice - Buddy
     * // Bob - Fluffy
     * // Charlie - No pet
     * }</pre>
     *
     * @param <K> The type of the join key.
     * @param <I> The type of the elements in the inner sequence.
     * @param <R> The type of the result elements.
     * @param inner The sequence to join with this sequence.
     * @param outerKeySelector A function used to extract the join key from
     *                         each element of this sequence.
     * @param innerKeySelector A function used to extract the join key from
     *                         each element of {@code inner}.
     * @param resultSelector A function used to create a result from an element
     *                       of this sequence and a matching element of
     *                       {@code inner}.
     * @return A sequence containing the result elements obtained by
     *         performing a left outer join on the two sequences.
     * @throws NullPointerException If {@code inner},
     *         {@code outerKeySelector}, {@code innerKeySelector}, or
     *         {@code resultSelector} is {@code null}.
     */
    @NotNull
    <K, I, R> Enumerable<R> leftJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T, @Nullable I, ? extends R> resultSelector
    );

    /**
     * <p>Correlates the elements of this sequence with the elements of
     * another sequence based on matching keys and produces a result element
     * for each element of this sequence.</p>
     *
     * <p>This method performs a left outer equijoin. Every element in this
     * sequence is included in the result, regardless of whether a matching
     * element exists in {@code inner}.</p>
     *
     * <p>If an element in this sequence has no matching element in
     * {@code inner}, {@code resultSelector} is invoked with {@code null}
     * as its inner element.</p>
     *
     * <p>If multiple elements in {@code inner} match an element in this
     * sequence, {@code resultSelector} is invoked once for each matching
     * element.</p>
     *
     * <p>The specified {@link Equalator} is used to compare join keys.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> The type of the join key.
     * @param <I> The type of the elements in the inner sequence.
     * @param <R> The type of the result elements.
     * @param inner The sequence to join with this sequence.
     * @param outerKeySelector A function used to extract the join key from
     *                         each element of this sequence.
     * @param innerKeySelector A function used to extract the join key from
     *                         each element of {@code inner}.
     * @param resultSelector A function used to create a result from an element
     *                       of this sequence and a matching element of
     *                       {@code inner}.
     * @param equalator The equality function used to compare join keys.
     * @return A sequence containing the result elements obtained by
     *         performing a left outer join on the two sequences.
     * @throws NullPointerException If {@code inner},
     *         {@code outerKeySelector}, {@code innerKeySelector},
     *         {@code resultSelector}, or {@code equalator} is {@code null}.
     */
    @NotNull
    <K, I, R> Enumerable<R> leftJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T, @Nullable I, ? extends R> resultSelector,
        @NotNull Equalator<? super K> equalator
    );

    /**
     * <p>Correlates the elements of this sequence with the elements of
     * another sequence based on matching keys and returns each matching
     * pair as a {@link Pair}.</p>
     *
     * <p>This method performs a left outer equijoin. Every element in this
     * sequence is included in the result, regardless of whether a matching
     * element exists in {@code inner}.</p>
     *
     * <p>If an element in this sequence has no matching element in
     * {@code inner}, the right value of the returned {@link Pair} is
     * {@code null}.</p>
     *
     * <p>If multiple elements in {@code inner} match an element in this
     * sequence, a separate {@link Pair} is produced for each matching
     * element.</p>
     *
     * <p>The default equality semantics are used to compare keys.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, int id) {}
     * record Pet(String name, int ownerId) {}
     *
     * Enumerable<Person> people = Linq.of(
     *     new Person("Alice", 1),
     *     new Person("Bob", 2),
     *     new Person("Charlie", 3)
     * );
     *
     * Enumerable<Pet> pets = Linq.of(
     *     new Pet("Buddy", 1),
     *     new Pet("Fluffy", 2)
     * );
     *
     * Enumerable<Pair<Person, Pet>> result = people.leftJoin(
     *     pets,
     *     Person::id,
     *     Pet::ownerId
     * );
     *
     * // Alice -> Buddy
     * // Bob -> Fluffy
     * // Charlie -> null
     * }</pre>
     *
     * @param <K> The type of the join key.
     * @param <I> The type of the elements in the inner sequence.
     * @param inner The sequence to join with this sequence.
     * @param outerKeySelector A function used to extract the join key from
     *                         each element of this sequence.
     * @param innerKeySelector A function used to extract the join key from
     *                         each element of {@code inner}.
     * @return A sequence containing a {@link Pair} for each matching pair
     *         of elements, including pairs whose right value is {@code null}
     *         when no matching inner element exists.
     * @throws NullPointerException If {@code inner},
     *         {@code outerKeySelector}, or {@code innerKeySelector} is
     *         {@code null}.
     */
    @NotNull
    <K, I> Enumerable<Pair<T, @Nullable I>> leftJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector
    );

    /**
     * <p>Correlates the elements of this sequence with the elements of
     * another sequence based on matching keys and returns each matching
     * pair as a {@link Pair}.</p>
     *
     * <p>This method performs a left outer equijoin. Every element in this
     * sequence is included in the result, regardless of whether a matching
     * element exists in {@code inner}.</p>
     *
     * <p>If an element in this sequence has no matching element in
     * {@code inner}, the right value of the returned {@link Pair} is
     * {@code null}.</p>
     *
     * <p>The specified {@link Equalator} is used to compare join keys.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * @param <K> The type of the join key.
     * @param <I> The type of the elements in the inner sequence.
     * @param inner The sequence to join with this sequence.
     * @param outerKeySelector A function used to extract the join key from
     *                         each element of this sequence.
     * @param innerKeySelector A function used to extract the join key from
     *                         each element of {@code inner}.
     * @param equalator The equality function used to compare join keys.
     * @return A sequence containing a {@link Pair} for each matching pair
     *         of elements, including pairs whose right value is {@code null}
     *         when no matching inner element exists.
     * @throws NullPointerException If {@code inner},
     *         {@code outerKeySelector}, {@code innerKeySelector}, or
     *         {@code equalator} is {@code null}.
     */
    @NotNull
    <K, I> Enumerable<Pair<T, @Nullable I>> leftJoin(
        @NotNull Enumerable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull Equalator<? super K> equalator
    );

    /**
     * <p>Returns the maximum value in a sequence.</p>
     *
     * <p>The elements are compared according to their natural ordering.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers =
     *     Linq.of(3, 7, 2, 9, 5);
     *
     * Integer max = numbers.max();
     *
     * System.out.println(max);
     *
     * // This code produces the following output:
     * //
     * // 9
     * }</pre>
     *
     * @return The maximum value in the sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    default T max() {
        return max((Comparator<? super T>) Comparator.naturalOrder());
    }

    /**
     * <p>Returns the maximum value in a sequence, using the specified
     * {@link Comparator} to compare elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "mango", "fig", "passionfruit");
     *
     * // Find the longest fruit name.
     * String longest = fruits.max(Comparator.comparingInt(String::length));
     *
     * System.out.println(longest);
     *
     * // This code produces the following output:
     * //
     * // passionfruit
     * }</pre>
     *
     * @param comparator The comparator used to compare elements.
     * @return The maximum value in the sequence, according to {@code comparator}.
     * @throws NullPointerException If {@code comparator} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     * @see #max()
     */
    @NotNull
    default T max(@NotNull Comparator<? super T> comparator) {
        NullCheck.requireNonNull(comparator, "comparator");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            T result = enumerator.current();
            while (enumerator.moveNext()) {
                T current = enumerator.current();
                if (comparator.compare(current, result) > 0) {
                    result = current;
                }
            }
            return result;
        }
    }

    /**
     * <p>Returns the maximum integer value obtained by applying the specified
     * selector to each element of a sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * int maxLength = fruits.maxToInt(String::length);
     *
     * System.out.println(maxLength);
     *
     * // This code produces the following output:
     * //
     * // 6
     * }</pre>
     *
     * @param selector A function that transforms an element into an
     *                 {@code int} value.
     * @return The maximum integer value produced by the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    default int maxToInt(
        @NotNull ToIntFunction<? super T> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            int result = selector.applyAsInt(enumerator.current());

            while (enumerator.moveNext()) {
                int current = selector.applyAsInt(enumerator.current());
                if (current > result) {
                    result = current;
                }
            }
            return result;
        }
    }

    /**
     * <p>Returns the maximum {@code long} value obtained by applying the
     * specified selector to each element of a sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> words =
     *     Linq.of("apple", "mango", "orange");
     *
     * long maxLength = words.maxToLong(String::length);
     *
     * System.out.println(maxLength);
     *
     * // This code produces the following output:
     * //
     * // 6
     * }</pre>
     *
     * @param selector A function that transforms an element into a
     *                 {@code long} value.
     * @return The maximum {@code long} value produced by the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    default long maxToLong(
        @NotNull ToLongFunction<? super T> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            long result = selector.applyAsLong(enumerator.current());

            while (enumerator.moveNext()) {
                long current = selector.applyAsLong(enumerator.current());
                if (current > result) {
                    result = current;
                }
            }
            return result;
        }
    }

    /**
     * <p>Returns the maximum {@code double} value obtained by applying the
     * specified selector to each element of a sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange");
     *
     * double maxLength =
     *     fruits.maxToDouble(String::length);
     *
     * System.out.println(maxLength);
     *
     * // This code produces the following output:
     * //
     * // 6.0
     * }</pre>
     *
     * @param selector A function that transforms an element into a
     *                 {@code double} value.
     * @return The maximum {@code double} value produced by the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    default double maxToDouble(
        @NotNull ToDoubleFunction<? super T> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            double result = selector.applyAsDouble(enumerator.current());

            while (enumerator.moveNext()) {
                double current = selector.applyAsDouble(enumerator.current());
                if (current > result) {
                    result = current;
                }
            }
            return result;
        }
    }

    /**
     * <p>Returns the maximum {@link BigDecimal} value obtained by applying
     * the specified selector to each element of a sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, BigDecimal price) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("Apple", new BigDecimal("1.50")),
     *     new Product("Mango", new BigDecimal("2.80")),
     *     new Product("Orange", new BigDecimal("2.10"))
     * );
     *
     * BigDecimal maxPrice =
     *     products.maxToDecimal(Product::price);
     *
     * System.out.println(maxPrice);
     *
     * // This code produces the following output:
     * //
     * // 2.80
     * }</pre>
     *
     * @param selector A function that transforms an element into a
     *                 {@link BigDecimal} value.
     * @return The maximum {@link BigDecimal} value produced by the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    @NotNull
    default BigDecimal maxToDecimal(
        @NotNull Function<? super T, ? extends BigDecimal> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            BigDecimal result = selector.apply(enumerator.current());

            while (enumerator.moveNext()) {
                BigDecimal current = selector.apply(enumerator.current());
                if (current.compareTo(result) > 0) {
                    result = current;
                }
            }
            return result;
        }
    }

    /**
     * <p>Returns the maximum element of a sequence according to a specified
     * key selector.</p>
     *
     * <p>The elements are compared according to the natural ordering of the
     * keys produced by {@code keySelector}.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int score) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 85),
     *     new Student("Bob", 92),
     *     new Student("Charlie", 78)
     * );
     *
     * Student best = students.maxBy(Student::score);
     *
     * System.out.println(best.name());
     *
     * // This code produces the following output:
     * //
     * // Bob
     * }</pre>
     *
     * @param <K> The type of the key used to compare elements.
     * @param keySelector A function that extracts a key from each element.
     * @return The element whose selected key is the maximum key in the sequence.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    @NotNull
    default  <K extends Comparable<? super K>> T maxBy(
        @NotNull Function<? super T, ? extends K> keySelector
    ) {
        return maxBy(keySelector, Comparator.naturalOrder());
    }


    /**
     * <p>Returns the maximum element of a sequence according to a specified
     * key selector and comparator.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int score) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 85),
     *     new Student("Bob", 92),
     *     new Student("Charlie", 78)
     * );
     *
     * Student best = students.maxBy(
     *     Student::name,
     *     Comparator.naturalOrder()
     * );
     *
     * System.out.println(best.name());
     *
     * // This code produces the following output:
     * //
     * // Charlie
     * }</pre>
     *
     * @param <K> The type of the key used to compare elements.
     * @param keySelector A function that extracts a key from each element.
     * @param comparator The comparator used to compare the selected keys.
     * @return The element whose selected key is the maximum key in the sequence.
     * @throws NullPointerException If {@code keySelector} or
     *                              {@code comparator} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    @NotNull
    default <K> T maxBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(comparator, "comparator");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            T result = enumerator.current();
            K resultKey = keySelector.apply(result);

            while (enumerator.moveNext()) {
                T current = enumerator.current();
                K currentKey = keySelector.apply(current);
                if (comparator.compare(currentKey, resultKey) > 0) {
                    result = current;
                    resultKey = currentKey;
                }
            }
            return result;
        }
    }

    /**
     * <p>Returns the minimum value in a sequence.</p>
     *
     * <p>The elements are compared according to their natural ordering.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers =
     *     Linq.of(3, 7, 2, 9, 5);
     *
     * Integer min = numbers.min();
     *
     * System.out.println(min);
     *
     * // This code produces the following output:
     * //
     * // 2
     * }</pre>
     *
     * @return The minimum value in the sequence.
     * @throws NoSuchElementException If the sequence contains no elements.
     * @see #min(Comparator)
     */
    @NotNull
    @SuppressWarnings("unchecked")
    default T min() {
        return min((Comparator<? super T>) Comparator.naturalOrder());
    }

    /**
     * <p>Returns the minimum value in a sequence, using the specified
     * {@link Comparator} to compare elements.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "mango", "fig", "passionfruit");
     *
     * // Find the shortest fruit name.
     * String shortest = fruits.min(Comparator.comparingInt(String::length));
     *
     * System.out.println(shortest);
     *
     * // This code produces the following output:
     * //
     * // fig
     * }</pre>
     *
     * @param comparator The comparator used to compare elements.
     * @return The minimum value in the sequence, according to {@code comparator}.
     * @throws NullPointerException If {@code comparator} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     * @see #min()
     */
    @NotNull
    default T min(@NotNull Comparator<? super T> comparator) {
        NullCheck.requireNonNull(comparator, "comparator");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            T result = enumerator.current();
            while (enumerator.moveNext()) {
                T current = enumerator.current();
                if (comparator.compare(current, result) < 0) {
                    result = current;
                }
            }
            return result;
        }
    }

    /**
     * <p>Returns the minimum {@code int} value obtained by applying the
     * specified selector to each element of a sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * int minLength = fruits.minToInt(String::length);
     *
     * System.out.println(minLength);
     *
     * // This code produces the following output:
     * //
     * // 5
     * }</pre>
     *
     * @param selector A function that transforms an element into an
     *                 {@code int} value.
     * @return The minimum {@code int} value produced by the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    default int minToInt(
        @NotNull ToIntFunction<? super T> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            int result = selector.applyAsInt(enumerator.current());

            while (enumerator.moveNext()) {
                int current = selector.applyAsInt(enumerator.current());
                if (current < result) {
                    result = current;
                }
            }
            return result;
        }
    }


    /**
     * <p>Returns the minimum {@code long} value obtained by applying the
     * specified selector to each element of a sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> words =
     *     Linq.of("apple", "mango", "orange");
     *
     * long minLength = words.minToLong(String::length);
     *
     * System.out.println(minLength);
     *
     * // This code produces the following output:
     * //
     * // 5
     * }</pre>
     *
     * @param selector A function that transforms an element into a
     *                 {@code long} value.
     * @return The minimum {@code long} value produced by the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    default long minToLong(
        @NotNull ToLongFunction<? super T> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            long result = selector.applyAsLong(enumerator.current());

            while (enumerator.moveNext()) {
                long current = selector.applyAsLong(enumerator.current());
                if (current < result) {
                    result = current;
                }
            }
            return result;
        }
    }


    /**
     * <p>Returns the minimum {@code double} value obtained by applying the
     * specified selector to each element of a sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange");
     *
     * double minLength =
     *     fruits.minToDouble(String::length);
     *
     * System.out.println(minLength);
     *
     * // This code produces the following output:
     * //
     * // 5.0
     * }</pre>
     *
     * @param selector A function that transforms an element into a
     *                 {@code double} value.
     * @return The minimum {@code double} value produced by the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    default double minToDouble(
        @NotNull ToDoubleFunction<? super T> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            double result = selector.applyAsDouble(enumerator.current());

            while (enumerator.moveNext()) {
                double current = selector.applyAsDouble(enumerator.current());
                if (current > result) {
                    result = current;
                }
            }
            return result;
        }
    }


    /**
     * <p>Returns the minimum {@link BigDecimal} value obtained by applying
     * the specified selector to each element of a sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, BigDecimal price) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("Apple", new BigDecimal("1.50")),
     *     new Product("Mango", new BigDecimal("2.80")),
     *     new Product("Orange", new BigDecimal("2.10"))
     * );
     *
     * BigDecimal minPrice =
     *     products.minToDecimal(Product::price);
     *
     * System.out.println(minPrice);
     *
     * // This code produces the following output:
     * //
     * // 1.50
     * }</pre>
     *
     * @param selector A function that transforms an element into a
     *                 {@link BigDecimal} value.
     * @return The minimum {@link BigDecimal} value produced by the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    @NotNull
    default BigDecimal minToDecimal(
        @NotNull Function<? super T, ? extends BigDecimal> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            BigDecimal result = selector.apply(enumerator.current());

            while (enumerator.moveNext()) {
                BigDecimal current = selector.apply(enumerator.current());
                if (current.compareTo(result) < 0) {
                    result = current;
                }
            }
            return result;
        }
    }

    /**
     * <p>Returns the minimum element of a sequence according to a specified
     * key selector.</p>
     *
     * <p>The elements are compared according to the natural ordering of the
     * keys produced by {@code keySelector}.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int score) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 85),
     *     new Student("Bob", 92),
     *     new Student("Charlie", 78)
     * );
     *
     * Student worst = students.minBy(Student::score);
     *
     * System.out.println(worst.name());
     *
     * // This code produces the following output:
     * //
     * // Charlie
     * }</pre>
     *
     * @param <K> The type of the key used to compare elements.
     * @param keySelector A function that extracts a key from each element.
     * @return The element whose selected key is the minimum key in the sequence.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    @NotNull
    default <K extends Comparable<? super K>> T minBy(
        @NotNull Function<? super T, ? extends K> keySelector
    ) {
        return minBy(keySelector, Comparator.naturalOrder());
    }


    /**
     * <p>Returns the minimum element of a sequence according to a specified
     * key selector and comparator.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int score) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 85),
     *     new Student("Bob", 92),
     *     new Student("Charlie", 78)
     * );
     *
     * Student first =
     *     students.minBy(
     *         Student::name,
     *         Comparator.naturalOrder()
     *     );
     *
     * System.out.println(first.name());
     *
     * // This code produces the following output:
     * //
     * // Alice
     * }</pre>
     *
     * @param <K> The type of the key used to compare elements.
     * @param keySelector A function that extracts a key from each element.
     * @param comparator The comparator used to compare the selected keys.
     * @return The element whose selected key is the minimum key in the sequence.
     * @throws NullPointerException If {@code keySelector} or
     *                              {@code comparator} is {@code null}.
     * @throws NoSuchElementException If the sequence contains no elements.
     */
    @NotNull
    default <K> T minBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    ) {
        NullCheck.requireNonNull(keySelector, "keySelector");
        NullCheck.requireNonNull(comparator, "comparator");

        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            T result = enumerator.current();
            K resultKey = keySelector.apply(result);

            while (enumerator.moveNext()) {
                T current = enumerator.current();
                K currentKey = keySelector.apply(current);
                if (comparator.compare(currentKey, resultKey) < 0) {
                    result = current;
                    resultKey = currentKey;
                }
            }
            return result;
        }
    }

    /**
     * Sorts the elements of a sequence in ascending order.
     *
     * <p>The elements are compared according to their natural ordering.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers =
     *     Linq.of(5, 2, 8, 1, 4);
     *
     * OrderedEnumerable<Integer> ordered =
     *     numbers.order();
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
     * @return An ordered sequence containing the elements in ascending order.
     */
    @NotNull
    OrderedEnumerable<T> order();


    /**
     * Sorts the elements of a sequence in ascending order according to
     * a specified comparator.
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * OrderedEnumerable<String> ordered =
     *     fruits.order(Comparator.comparingInt(String::length));
     *
     * ordered.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // apple
     * // grape
     * // mango
     * // orange
     * }</pre>
     *
     * @param comparator The comparator used to compare elements.
     * @return An ordered sequence containing the elements in ascending order.
     * @throws NullPointerException If {@code comparator} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> order(
        @NotNull Comparator<? super T> comparator
    );


    /**
     * Sorts the elements of a sequence in ascending order according to
     * a specified key selector.
     *
     * <p>The selected keys are compared according to their natural ordering.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int score) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 90),
     *     new Student("Bob", 75),
     *     new Student("Charlie", 85)
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students.orderBy(Student::score);
     *
     * ordered.forEach(student ->
     *     System.out.println(student.name()));
     *
     * // This code produces the following output:
     * //
     * // Bob
     * // Charlie
     * // Alice
     * }</pre>
     *
     * @param <K> The type of the ordering key.
     * @param keySelector A function that extracts the ordering key from
     *                    each element.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected key.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    <K extends Comparable<? super K>> OrderedEnumerable<T> orderBy(
        @NotNull Function<? super T, ? extends K> keySelector
    );


    /**
     * Sorts the elements of a sequence in ascending order according to
     * a specified key selector and comparator.
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int score) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 90),
     *     new Student("Bob", 75),
     *     new Student("Charlie", 85)
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students.orderBy(
     *         Student::name,
     *         Comparator.reverseOrder()
     *     );
     *
     * ordered.forEach(student ->
     *     System.out.println(student.name()));
     *
     * // This code produces the following output:
     * //
     * // Charlie
     * // Bob
     * // Alice
     * }</pre>
     *
     * @param <K> The type of the ordering key.
     * @param keySelector A function that extracts the ordering key from
     *                    each element.
     * @param comparator The comparator used to compare the selected keys.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected key.
     * @throws NullPointerException If {@code keySelector} or
     *                              {@code comparator} is {@code null}.
     */
    @NotNull
    <K> OrderedEnumerable<T> orderBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    );


    /**
     * Sorts the elements of a sequence in descending order according to
     * a specified key selector.
     *
     * <p>The selected keys are compared according to their natural ordering.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int score) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 90),
     *     new Student("Bob", 75),
     *     new Student("Charlie", 85)
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students.orderByDescending(Student::score);
     *
     * ordered.forEach(student ->
     *     System.out.println(student.name()));
     *
     * // This code produces the following output:
     * //
     * // Alice
     * // Charlie
     * // Bob
     * }</pre>
     *
     * @param <K> The type of the ordering key.
     * @param keySelector A function that extracts the ordering key from
     *                    each element.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected key in descending order.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    <K extends Comparable<? super K>> OrderedEnumerable<T> orderByDescending(
        @NotNull Function<? super T, ? extends K> keySelector
    );


    /**
     * Sorts the elements of a sequence in descending order according to
     * a specified key selector and comparator.
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int score) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 90),
     *     new Student("Bob", 75),
     *     new Student("Charlie", 85)
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students.orderByDescending(
     *         Student::name,
     *         Comparator.naturalOrder()
     *     );
     *
     * ordered.forEach(student ->
     *     System.out.println(student.name()));
     *
     * // This code produces the following output:
     * //
     * // Charlie
     * // Bob
     * // Alice
     * }</pre>
     *
     * @param <K> The type of the ordering key.
     * @param keySelector A function that extracts the ordering key from
     *                    each element.
     * @param comparator The comparator used to compare the selected keys.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected key in descending order.
     * @throws NullPointerException If {@code keySelector} or
     *                              {@code comparator} is {@code null}.
     */
    @NotNull
    <K> OrderedEnumerable<T> orderByDescending(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    );


    /**
     * Sorts the elements of a sequence in ascending order according to
     * an {@code int} key.
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * OrderedEnumerable<String> ordered =
     *     fruits.orderByInt(String::length);
     *
     * ordered.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // apple
     * // grape
     * // mango
     * // orange
     * }</pre>
     *
     * @param keySelector A function that extracts an {@code int} key from
     *                    each element.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected {@code int} key.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> orderByInt(
        @NotNull ToIntFunction<? super T> keySelector
    );


    /**
     * Sorts the elements of a sequence in descending order according to
     * an {@code int} key.
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * OrderedEnumerable<String> ordered =
     *     fruits.orderByIntDescending(String::length);
     *
     * ordered.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // passionfruit
     * // orange
     * // mango
     * // apple
     * }</pre>
     *
     * @param keySelector A function that extracts an {@code int} key from
     *                    each element.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected {@code int} key in descending order.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> orderByIntDescending(
        @NotNull ToIntFunction<? super T> keySelector
    );


    /**
     * Sorts the elements of a sequence in ascending order according to
     * a {@code long} key.
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record FileInfo(String name, long size) {}
     *
     * Enumerable<FileInfo> files = Linq.of(
     *     new FileInfo("a.txt", 500L),
     *     new FileInfo("b.txt", 1200L),
     *     new FileInfo("c.txt", 800L)
     * );
     *
     * OrderedEnumerable<FileInfo> ordered =
     *     files.orderByLong(FileInfo::size);
     *
     * // The elements are ordered by size:
     * //
     * // a.txt, c.txt, b.txt
     * }</pre>
     *
     * @param keySelector A function that extracts a {@code long} key from
     *                    each element.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected {@code long} key.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> orderByLong(
        @NotNull ToLongFunction<? super T> keySelector
    );


    /**
     * Sorts the elements of a sequence in descending order according to
     * a {@code long} key.
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record FileInfo(String name, long size) {}
     *
     * Enumerable<FileInfo> files = Linq.of(
     *     new FileInfo("a.txt", 500L),
     *     new FileInfo("b.txt", 1200L),
     *     new FileInfo("c.txt", 800L)
     * );
     *
     * OrderedEnumerable<FileInfo> ordered =
     *     files.orderByLongDescending(FileInfo::size);
     *
     * // The elements are ordered by size:
     * //
     * // b.txt, c.txt, a.txt
     * }</pre>
     *
     * @param keySelector A function that extracts a {@code long} key from
     *                    each element.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected {@code long} key in descending order.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> orderByLongDescending(
        @NotNull ToLongFunction<? super T> keySelector
    );


    /**
     * Sorts the elements of a sequence in ascending order according to
     * a {@code double} key.
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, double price) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("Apple", 2.5),
     *     new Product("Orange", 1.8),
     *     new Product("Banana", 2.1)
     * );
     *
     * OrderedEnumerable<Product> ordered =
     *     products.orderByDouble(Product::price);
     *
     * // The elements are ordered by price:
     * //
     * // Orange, Banana, Apple
     * }</pre>
     *
     * @param keySelector A function that extracts a {@code double} key from
     *                    each element.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected {@code double} key.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> orderByDouble(
        @NotNull ToDoubleFunction<? super T> keySelector
    );


    /**
     * Sorts the elements of a sequence in descending order according to
     * a {@code double} key.
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, double price) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("Apple", 2.5),
     *     new Product("Orange", 1.8),
     *     new Product("Banana", 2.1)
     * );
     *
     * OrderedEnumerable<Product> ordered =
     *     products.orderByDoubleDescending(Product::price);
     *
     * // The elements are ordered by price:
     * //
     * // Apple, Banana, Orange
     * }</pre>
     *
     * @param keySelector A function that extracts a {@code double} key from
     *                    each element.
     * @return An ordered sequence whose elements are sorted according to
     *         the selected {@code double} key in descending order.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> orderByDoubleDescending(
        @NotNull ToDoubleFunction<? super T> keySelector
    );

    /**
     * <p>Adds an element to the beginning of the sequence.</p>
     *
     * <p>This method does not modify the current sequence. Instead, it returns
     * a new sequence whose first element is {@code element}, followed by all
     * elements of the current sequence.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers =
     *     Linq.of(1, 2, 3, 4);
     *
     * Enumerable<Integer> result =
     *     numbers.prepend(0);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 0
     * // 1
     * // 2
     * // 3
     * // 4
     * }</pre>
     *
     * @param element The element to add to the beginning of the sequence.
     * @return A new sequence that begins with {@code element}, followed by
     *         the elements of the current sequence.
     */
    @NotNull
    Enumerable<T> prepend(
        @Nullable T element
    );

    /**
     * <p>Reverses the order of the elements in a sequence.</p>
     *
     * <p>This method does not sort the elements or compare their values.
     * It simply returns the elements in the reverse order from which they
     * are produced by the underlying sequence.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange", "grape");
     *
     * Enumerable<String> reversed =
     *     fruits.reverse();
     *
     * reversed.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // grape
     * // orange
     * // mango
     * // apple
     * }</pre>
     *
     * @return A sequence whose elements correspond to those of the current
     *         sequence in reverse order.
     */
    @NotNull
    Enumerable<T> reverse();

    /**
     * <p>Correlates the elements of two sequences based on matching keys
     * and produces result elements by using a specified result selector.</p>
     *
     * <p>This method performs a right outer join. Every element from the
     * second sequence is included in the result, regardless of whether
     * a matching element is found in the first sequence.</p>
     *
     * <p>When a matching element is found in the first sequence, the
     * {@code resultSelector} is invoked for the matching pair. When no
     * matching element is found, the outer element is {@code null}.</p>
     *
     * <p>The default equality semantics are used to compare keys.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Employee(int id, String name) {}
     * record Department(int id, String name) {}
     *
     * Enumerable<Employee> employees = Linq.of(
     *     new Employee(1, "Alice"),
     *     new Employee(2, "Bob")
     * );
     *
     * Enumerable<Department> departments = Linq.of(
     *     new Department(1, "IT"),
     *     new Department(2, "HR"),
     *     new Department(3, "Sales")
     * );
     *
     * Enumerable<String> result =
     *     employees.rightJoin(
     *         departments,
     *         Employee::id,
     *         Department::id,
     *         (employee, department) ->
     *             employee == null
     *                 ? department.name() + ": unassigned"
     *                 : employee.name() + " -> " + department.name()
     *     );
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // Alice -> IT
     * // Bob -> HR
     * // Sales: unassigned
     * }</pre>
     *
     * @param <I> The type of elements in the second sequence.
     * @param <K> The type of the join key.
     * @param <R> The type of the result elements.
     * @param inner The sequence to join to the current sequence.
     * @param outerKeySelector A function to extract the join key from each
     *                         element of the current sequence.
     * @param innerKeySelector A function to extract the join key from each
     *                         element of the second sequence.
     * @param resultSelector A function to create a result element from a
     *                       matching pair of elements.
     * @return A sequence containing the results of the right outer join.
     * @throws NullPointerException If {@code inner}, {@code outerKeySelector},
     *                              {@code innerKeySelector}, or
     *                              {@code resultSelector} is {@code null}.
     */
    @NotNull
    <I, K, R> Enumerable<R> rightJoin(
        @NotNull Iterable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T, ? super I, ? extends R> resultSelector
    );

    /**
     * <p>Correlates the elements of two sequences based on matching keys
     * and produces result elements by using a specified result selector.</p>
     *
     * <p>This method performs a right outer join. Every element from the
     * second sequence is included in the result, regardless of whether
     * a matching element is found in the first sequence.</p>
     *
     * <p>The specified {@code equalator} is used to compare and hash
     * join keys.</p>
     *
     * <p>When a matching element is found in the first sequence, the
     * {@code resultSelector} is invoked for the matching pair. When no
     * matching element is found, the outer element is {@code null}.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Employee(int id, String name) {}
     * record Department(int id, String name) {}
     *
     * Enumerable<Employee> employees = Linq.of(
     *     new Employee(1, "Alice"),
     *     new Employee(2, "Bob")
     * );
     *
     * Enumerable<Department> departments = Linq.of(
     *     new Department(1, "IT"),
     *     new Department(2, "HR"),
     *     new Department(3, "Sales")
     * );
     *
     * Enumerable<String> result =
     *     employees.rightJoin(
     *         departments,
     *         Employee::id,
     *         Department::id,
     *         (employee, department) ->
     *             employee == null
     *                 ? department.name() + ": unassigned"
     *                 : employee.name() + " -> " + department.name(),
     *         Equalator.defaultEqualator()
     *     );
     * }</pre>
     *
     * @param <I> The type of elements in the second sequence.
     * @param <K> The type of the join key.
     * @param <R> The type of the result elements.
     * @param inner The sequence to join to the current sequence.
     * @param outerKeySelector A function to extract the join key from each
     *                         element of the current sequence.
     * @param innerKeySelector A function to extract the join key from each
     *                         element of the second sequence.
     * @param resultSelector A function to create a result element from a
     *                       matching pair of elements.
     * @param equalator The equality function used to compare and hash keys.
     * @return A sequence containing the results of the right outer join.
     * @throws NullPointerException If {@code inner}, {@code outerKeySelector},
     *                              {@code innerKeySelector},
     *                              {@code resultSelector}, or {@code equalator}
     *                              is {@code null}.
     */
    @NotNull
    <I, K, R> Enumerable<R> rightJoin(
        @NotNull Iterable<? extends I> inner,
        @NotNull Function<? super T, ? extends K> outerKeySelector,
        @NotNull Function<? super I, ? extends K> innerKeySelector,
        @NotNull BinFunction<? super T, ? super I, ? extends R> resultSelector,
        @NotNull Equalator<? super K> equalator
    );

    /**
     * <p>Projects each element of the sequence into a new form.</p>
     *
     * <p>The selector function is invoked once for each element of the
     * sequence, and the returned values form the resulting sequence.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers =
     *     Linq.of(1, 2, 3, 4, 5);
     *
     * Enumerable<Integer> squares =
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
     * // 25
     * }</pre>
     *
     * @param <R> The type of the value returned by the selector.
     * @param selector A transform function to apply to each element.
     * @return A sequence whose elements are the result of invoking the
     *         selector function on each element of the sequence.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    <R> Enumerable<R> select(
        @NotNull Function<? super T, ? extends R> selector
    );


    /**
     * <p>Projects each element of the sequence into a new form by
     * incorporating the element's index.</p>
     *
     * <p>The second parameter of the selector represents the zero-based
     * index of the element in the source sequence.</p>
     *
     * <p>This method uses deferred execution.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of(
     *         "apple",
     *         "banana",
     *         "mango",
     *         "orange"
     *     );
     *
     * Enumerable<String> result =
     *     fruits.select((fruit, index) ->
     *         index + ": " + fruit
     *     );
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 0: apple
     * // 1: banana
     * // 2: mango
     * // 3: orange
     * }</pre>
     *
     * @param <R> The type of the value returned by the selector.
     * @param selector A transform function to apply to each element.
     *                 The second parameter represents the zero-based
     *                 index of the element in the source sequence.
     * @return A sequence whose elements are the result of invoking the
     *         selector function on each element of the sequence.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    <R> Enumerable<R> select(
        @NotNull BinFunction<? super T, Integer, ? extends R> selector
    );

    /**
     * Projects each element of the sequence to an {@link Iterable},
     * and flattens the resulting sequences into one sequence.
     *
     * @param <R> The type of the elements in the resulting sequence.
     * @param selector A transform function to apply to each element.
     * @return A flattened sequence containing the elements produced by
     *         the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    <R> Enumerable<R> selectMany(
        @NotNull Function<? super T, ? extends Enumerable<? extends R>> selector
    );


    /**
     * Projects each element of the sequence to an {@link Iterable},
     * and flattens the resulting sequences into one sequence.
     *
     * <p>The second parameter of the selector represents the zero-based
     * index of the source element.</p>
     *
     * @param <R> The type of the elements in the resulting sequence.
     * @param selector A transform function to apply to each source element.
     * @return A flattened sequence containing the elements produced by
     *         the selector.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    <R> Enumerable<R> selectMany(
        @NotNull BinFunction<? super T, Integer,
            ? extends Enumerable<? extends R>> selector
    );


    /**
     * Projects each element of the sequence to an {@link Iterable},
     * flattens the resulting sequences into one sequence, and applies
     * a result selector to each intermediate element.
     *
     * @param <C> The type of the intermediate elements.
     * @param <R> The type of the resulting elements.
     * @param collectionSelector A transform function that returns the
     *                           intermediate sequence for each source element.
     * @param resultSelector A transform function that combines the source
     *                       element and an intermediate element.
     * @return A flattened sequence containing the results produced by
     *         the result selector.
     * @throws NullPointerException If {@code collectionSelector} or
     *                              {@code resultSelector} is {@code null}.
     */
    @NotNull
    <C, R> Enumerable<R> selectMany(
        @NotNull Function<? super T, ? extends Iterable<? extends C>> collectionSelector,
        @NotNull BinFunction<? super T, ? super C, ? extends R> resultSelector
    );


    /**
     * Projects each element of the sequence to an {@link Iterable},
     * flattens the resulting sequences into one sequence, and applies
     * a result selector to each intermediate element.
     *
     * <p>The second parameter of {@code collectionSelector} represents
     * the zero-based index of the source element.</p>
     *
     * @param <C> The type of the intermediate elements.
     * @param <R> The type of the resulting elements.
     * @param collectionSelector A transform function that returns the
     *                           intermediate sequence for each source element.
     * @param resultSelector A transform function that combines the source
     *                       element and an intermediate element.
     * @return A flattened sequence containing the results produced by
     *         the result selector.
     * @throws NullPointerException If {@code collectionSelector} or
     *                              {@code resultSelector} is {@code null}.
     */
    @NotNull
    <C, R> Enumerable<R> selectMany(
        @NotNull BinFunction<? super T, Integer,
            ? extends Iterable<? extends C>> collectionSelector,
        @NotNull BinFunction<? super T, ? super C, ? extends R> resultSelector
    );

    /**
     * <p>Determines whether two sequences are equal by comparing their
     * corresponding elements using the default equality semantics.</p>
     *
     * <p>The sequences are enumerated in parallel. The sequences are
     * considered equal only when they contain the same number of elements
     * and each pair of corresponding elements is equal.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> first =
     *     Linq.of("apple", "banana", "orange");
     *
     * Enumerable<String> second =
     *     Linq.of("apple", "banana", "orange");
     *
     * boolean equal =
     *     first.sequenceEqual(second);
     *
     * System.out.println(equal);
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param other The sequence to compare with the current sequence.
     * @return {@code true} if the two sequences have the same number of
     *         elements and each pair of corresponding elements is equal;
     *         {@code false} otherwise.
     * @throws NullPointerException If {@code other} is {@code null}.
     */
    boolean sequenceEqual(
        @NotNull Enumerable<? extends T> other
    );


    /**
     * <p>Determines whether two sequences are equal by comparing their
     * corresponding elements using a specified {@link Equalator}.</p>
     *
     * <p>The sequences are enumerated in parallel. The sequences are
     * considered equal only when they contain the same number of elements
     * and each pair of corresponding elements is considered equal by
     * the specified {@code equalator}.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Person(String name, int age) {}
     *
     * Enumerable<Person> first =
     *     Linq.of(
     *         new Person("Alice", 20),
     *         new Person("Bob", 21)
     *     );
     *
     * Enumerable<Person> second =
     *     Linq.of(
     *         new Person("alice", 20),
     *         new Person("bob", 21)
     *     );
     *
     * boolean equal =
     *     first.sequenceEqual(
     *         second,
     *         Equalator.comparing(
     *             person -> person.name().toLowerCase()
     *         )
     *     );
     *
     * System.out.println(equal);
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param other The sequence to compare with the current sequence.
     * @param equalator The equality function used to compare corresponding
     *                  elements.
     * @return {@code true} if the two sequences have the same number of
     *         elements and each pair of corresponding elements is considered
     *         equal by {@code equalator}; {@code false} otherwise.
     * @throws NullPointerException If {@code other} or {@code equalator}
     *                              is {@code null}.
     */
    boolean sequenceEqual(
        @NotNull Enumerable<? extends T> other,
        @NotNull Equalator<? super T> equalator
    );

    /**
     * <p>Returns an enumerable that iterates over the elements of the source sequence in a randomized order.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(1, 2, 3, 4, 5);
     *
     * // Shuffle the elements.
     * Enumerable<Integer> shuffled = numbers.shuffle();
     *
     * shuffled.forEach(n -> System.out.print(n + " "));
     *
     * // This code produces output similar to the following (results will vary):
     * //
     * // 4 1 5 2 3
     * }</pre>
     *
     * @return A sequence whose elements correspond to those of the input sequence in a randomized order.
     */
    @NotNull
    Enumerable<T> shuffle();

    /**
     * <p>Returns the only element of a sequence, and throws an exception
     * if there is not exactly one element in the sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits1 = Linq.of("orange");
     *
     * String fruit = fruits1.single();
     *
     * System.out.println(fruit);
     *
     * // This code produces the following output:
     * //
     * // orange
     * }</pre>
     *
     * @return The single element of the input sequence.
     * @throws java.util.NoSuchElementException If the sequence contains no elements.
     * @throws IllegalStateException If the sequence contains more than one element.
     * @see #single(Predicate)
     */
    default T single() {
        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                throw new NoSuchElementException("Sequence contains no elements.");
            }

            T result = enumerator.current();

            if (enumerator.moveNext()) {
                throw new IllegalStateException("Sequence contains more than one element.");
            }

            return result;
        }
    }

    /**
     * <p>Returns the only element of a sequence that satisfies a specified condition,
     * and throws an exception if more than one such element exists.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "banana", "mango");
     *
     * // Get the only fruit with a name longer than 5 characters.
     * String fruit = fruits.single(f -> f.length() > 5);
     *
     * System.out.println(fruit);
     *
     * // This code produces the following output:
     * //
     * // banana
     * }</pre>
     *
     * @param predicate A function to test an element for a condition.
     * @return The single element of the input sequence that satisfies a condition.
     * @throws java.util.NoSuchElementException If no element satisfies the condition.
     * @throws IllegalStateException If more than one element satisfies the condition.
     * @see #single()
     */
    default T single(@NotNull Predicate<? super T> predicate) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (Enumerator<T> enumerator = enumerator()) {
            T result = null;
            boolean found = false;

            while (enumerator.moveNext()) {
                T current = enumerator.current();
                if (predicate.test(current)) {
                    if (found) {
                        throw new IllegalStateException("Sequence contains more than one matching element.");
                    }
                    result = current;
                    found = true;
                }
            }

            if (!found) {
                throw new NoSuchElementException("No element satisfies the condition.");
            }
            return result;
        }
    }

    /**
     * <p>Returns the only element of a sequence, or {@code null} if the sequence is empty;
     * this method throws an exception if there is more than one element in the sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> emptyFruits = Linq.empty();
     *
     * String fruit = emptyFruits.singleOrNull();
     *
     * System.out.println(fruit == null ? "No fruit found" : fruit);
     *
     * // This code produces the following output:
     * //
     * // No fruit found
     * }</pre>
     *
     * @return The single element of the input sequence, or {@code null} if the sequence contains no elements.
     * @throws IllegalStateException If the sequence contains more than one element.
     * @see #singleOrNull(Predicate)
     */
    @Nullable
    default T singleOrNull() {
        return singleOrDefault(null);
    }

    /**
     * <p>Returns the only element of a sequence that satisfies a specified condition or {@code null}
     * if no such element exists; this method throws an exception if more than one element satisfies the condition.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "banana", "mango");
     *
     * // Try to get the only fruit with a name longer than 10 characters.
     * String longFruit = fruits.singleOrNull(f -> f.length() > 10);
     *
     * System.out.println(longFruit == null ? "Not found" : longFruit);
     *
     * // This code produces the following output:
     * //
     * // Not found
     * }</pre>
     *
     * @param predicate A function to test an element for a condition.
     * @return The single element of the input sequence that satisfies the condition, or {@code null} if no such element is found.
     * @throws IllegalStateException If more than one element satisfies the condition.
     * @see #singleOrNull()
     */
    @Nullable
    default T singleOrNull(@NotNull Predicate<? super T> predicate) {
        return singleOrDefault(predicate, null);
    }

    /**
     * <p>Returns the only element of a sequence, or a specified default value if the sequence is empty;
     * this method throws an exception if there is more than one element in the sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> emptyFruits = Linq.empty();
     *
     * String fruit = emptyFruits.singleOrDefault("unknown");
     *
     * System.out.println(fruit);
     *
     * // This code produces the following output:
     * //
     * // unknown
     * }</pre>
     *
     * @param defaultValue The value to return if the sequence is empty.
     * @return The single element of the input sequence, or {@code defaultValue} if the sequence contains no elements.
     * @throws IllegalStateException If the sequence contains more than one element.
     * @see #singleOrDefault(Predicate, T)
     */
    @Nullable
    default T singleOrDefault(@Nullable T defaultValue) {
        try (Enumerator<T> enumerator = enumerator()) {
            if (!enumerator.moveNext()) {
                return defaultValue;
            }

            T result = enumerator.current();

            if (enumerator.moveNext()) {
                throw new IllegalStateException("Sequence contains more than one element.");
            }

            return result;
        }
    }

    /**
     * <p>Returns the only element of a sequence that satisfies a specified condition, or a specified default value
     * if no such element exists; this method throws an exception if more than one element satisfies the condition.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "banana", "mango");
     *
     * // Try to get the only fruit with a name longer than 10 characters, or return "defaultFruit".
     * String fruit = fruits.singleOrDefault(f -> f.length() > 10, "defaultFruit");
     *
     * System.out.println(fruit);
     *
     * // This code produces the following output:
     * //
     * // defaultFruit
     * }</pre>
     *
     * @param predicate A function to test an element for a condition.
     * @param defaultValue The value to return if no element satisfies the condition.
     * @return The single element of the input sequence that satisfies the condition, or {@code defaultValue} if no such element is found.
     * @throws IllegalStateException If more than one element satisfies the condition.
     * @see #singleOrDefault(T)
     */
    @Nullable
    default T singleOrDefault(@NotNull Predicate<? super T> predicate, @Nullable T defaultValue) {
        NullCheck.requireNonNull(predicate, "predicate");

        try (Enumerator<T> enumerator = enumerator()) {
            T result = defaultValue;
            boolean found = false;

            while (enumerator.moveNext()) {
                T current = enumerator.current();
                if (predicate.test(current)) {
                    if (found) {
                        throw new IllegalStateException("Sequence contains more than one matching element.");
                    }
                    result = current;
                    found = true;
                }
            }

            return result;
        }
    }

    /**
     * <p>Bypasses a specified number of elements in a sequence and then returns the remaining elements.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> grades = Linq.of(59, 82, 70, 56, 92, 98, 85);
     *
     * // Sort descending and skip the first three elements.
     * Enumerable<Integer> lowerGrades = grades.orderByDescending(g -> g).skip(3);
     *
     * System.out.println("All grades except the top three are:");
     * lowerGrades.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // All grades except the top three are:
     * // 82
     * // 70
     * // 59
     * // 56
     * }</pre>
     *
     * @param count The number of elements to skip before returning the remaining elements.
     * @return An enumerable that contains the elements that occur after the specified index in the input sequence.
     * @see #skipLast(int)
     * @see #skipWhile(Predicate)
     */
    @NotNull
    Enumerable<T> skip(int count);

    /**
     * <p>Returns a new enumerable collection that contains the elements from source with the last {@code count}
     * elements of the source collection omitted.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> grades = Linq.of(59, 82, 70, 56, 92, 98, 85);
     *
     * // Skip the last three elements in the sequence.
     * Enumerable<Integer> result = grades.skipLast(3);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 59
     * // 82
     * // 70
     * // 56
     * }</pre>
     *
     * @param count The number of elements to omit from the end of the collection.
     * @return A new enumerable collection that contains the elements from source minus {@code count} elements from the end of the collection.
     * @see #skip(int)
     */
    @NotNull
    Enumerable<T> skipLast(int count);

    /**
     * <p>Bypasses elements in a sequence as long as a specified condition is true and then returns the remaining elements.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> grades = Linq.of(59, 82, 70, 56, 92, 98, 85);
     *
     * // Sort descending and skip elements as long as they are greater than or equal to 80.
     * Enumerable<Integer> lowerGrades = grades.orderByDescending(g -> g)
     *                                         .skipWhile(g -> g >= 80);
     *
     * System.out.println("All grades below 80:");
     * lowerGrades.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // All grades below 80:
     * // 70
     * // 59
     * // 56
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An enumerable that contains the elements from the input sequence starting at the first element in the linear series that does not pass the test specified by {@code predicate}.
     * @see #skipWhile(BinPredicate)
     * @see #skip(int)
     */
    @NotNull
    Enumerable<T> skipWhile(@NotNull Predicate<? super T> predicate);

    /**
     * <p>Bypasses elements in a sequence as long as a specified condition is true and then returns the remaining elements.
     * The element's index is used in the logic of the predicate function.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> amounts = Linq.of(5000, 2500, 9000, 8000, 6500, 4000, 1500, 5500);
     *
     * // Skip elements as long as the element's index multiplied by 1000
     * // is less than or equal to the element itself.
     * Enumerable<Integer> result = amounts.skipWhile((amount, index) -> amount > index * 1000);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 4000
     * // 1500
     * // 5500
     * }</pre>
     *
     * @param predicate A function to test each source element for a condition; the second parameter of the function represents the index of the source element.
     * @return An enumerable that contains the elements from the input sequence starting at the first element in the linear series that does not pass the test specified by {@code predicate}.
     * @see #skipWhile(Predicate)
     */
    @NotNull
    Enumerable<T> skipWhile(@NotNull BinPredicate<? super T, Integer> predicate);
    /**
     * <p>Computes the sum of a sequence of {@code int} values that are obtained by
     * applying the specified selector to each element of the sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits =
     *     Linq.of("apple", "mango", "orange");
     *
     * // 5 + 5 + 6 = 16
     * long totalLength = fruits.sumToInt(String::length);
     *
     * System.out.println(totalLength);
     *
     * // This code produces the following output:
     * //
     * // 16
     * }</pre>
     *
     * @param selector A function that transforms an element into an
     *                 {@code int} value.
     * @return The sum of the projected values. Returns {@code 0} if the sequence contains no elements.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    default long sumToInt(
        @NotNull ToIntFunction<? super T> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        long sum = 0L;
        try (Enumerator<T> enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                // sum += selector.applyAsInt(enumerator.current());
                sum = Math.addExact(sum, selector.applyAsInt(enumerator.current()));
            }
        }
        return sum;
    }

    /**
     * <p>Computes the sum of a sequence of {@code long} values that are obtained by
     * applying the specified selector to each element of the sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record File(String name, long bytes) {}
     *
     * Enumerable<File> files = Linq.of(
     *     new File("doc1.txt", 1500L),
     *     new File("doc2.txt", 3200L),
     *     new File("image.png", 5300L)
     * );
     *
     * long totalBytes = files.sumToLong(File::bytes);
     *
     * System.out.println(totalBytes);
     *
     * // This code produces the following output:
     * //
     * // 10000
     * }</pre>
     *
     * @param selector A function that transforms an element into a
     *                 {@code long} value.
     * @return The sum of the projected values. Returns {@code 0L} if the sequence contains no elements.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    default long sumToLong(
        @NotNull ToLongFunction<? super T> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        long sum = 0L;
        try (Enumerator<T> enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                // sum += selector.applyAsLong(enumerator.current());
                sum = Math.addExact(sum, selector.applyAsLong(enumerator.current()));
            }
        }
        return sum;
    }

    /**
     * <p>Computes the sum of a sequence of {@code double} values that are obtained by
     * applying the specified selector to each element of the sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Package(String name, double weight) {}
     *
     * Enumerable<Package> packages = Linq.of(
     *     new Package("Box 1", 2.5),
     *     new Package("Box 2", 3.2),
     *     new Package("Box 3", 1.1)
     * );
     *
     * double totalWeight = packages.sumToDouble(Package::weight);
     *
     * System.out.println(totalWeight);
     *
     * // This code produces the following output:
     * //
     * // 6.8
     * }</pre>
     *
     * @param selector A function that transforms an element into a
     *                 {@code double} value.
     * @return The sum of the projected values. Returns {@code 0.0} if the sequence contains no elements.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    default double sumToDouble(
        @NotNull ToDoubleFunction<? super T> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        double sum = 0.0;
        try (Enumerator<T> enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                sum += selector.applyAsDouble(enumerator.current());
            }
        }
        return sum;
    }

    /**
     * <p>Computes the sum of a sequence of {@link BigDecimal} values that are obtained by
     * applying the specified selector to each element of the sequence.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, BigDecimal price) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("Apple", new BigDecimal("1.50")),
     *     new Product("Mango", new BigDecimal("2.80")),
     *     new Product("Orange", new BigDecimal("2.10"))
     * );
     *
     * BigDecimal totalPrice =
     *     products.sumToDecimal(Product::price);
     *
     * System.out.println(totalPrice);
     *
     * // This code produces the following output:
     * //
     * // 6.40
     * }</pre>
     *
     * @param selector A function that transforms an element into a
     *                 {@link BigDecimal} value.
     * @return The sum of the projected values. Returns {@link BigDecimal#ZERO} if the sequence contains no elements.
     * @throws NullPointerException If {@code selector} is {@code null}.
     */
    @NotNull
    default BigDecimal sumToDecimal(
        @NotNull Function<? super T, ? extends BigDecimal> selector
    ) {
        NullCheck.requireNonNull(selector, "selector");

        BigDecimal sum = BigDecimal.ZERO;
        try (Enumerator<T> enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                sum = sum.add(selector.apply(enumerator.current()));
            }
        }
        return sum;
    }

    /**
     * <p>Returns a specified number of contiguous elements from the start of a sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> grades = Linq.of(59, 82, 70, 56, 92, 98, 85);
     *
     * // Sort descending and take the first three elements.
     * Enumerable<Integer> topThreeGrades = grades.orderByDescending(g -> g).take(3);
     *
     * System.out.println("The top three grades are:");
     * topThreeGrades.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // The top three grades are:
     * // 98
     * // 92
     * // 85
     * }</pre>
     *
     * @param count The number of elements to return.
     * @return An enumerable that contains the specified number of elements from the start of the input sequence.
     * @see #takeLast(int)
     * @see #takeWhile(Predicate)
     */
    @NotNull
    Enumerable<T> take(int count);

    /**
     * <p>Returns a new enumerable collection that contains the last {@code count} elements from source.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> grades = Linq.of(59, 82, 70, 56, 92, 98, 85);
     *
     * // Take the last three elements in the sequence.
     * Enumerable<Integer> result = grades.takeLast(3);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 92
     * // 98
     * // 85
     * }</pre>
     *
     * @param count The number of elements to take from the end of the collection.
     * @return A new enumerable collection that contains the last {@code count} elements from source.
     * @see #take(int)
     */
    @NotNull
    Enumerable<T> takeLast(int count);

    /**
     * <p>Returns elements from a sequence as long as a specified condition is true,
     * and then skips the remaining elements.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "banana", "mango", "orange", "passionfruit", "grape");
     *
     * // Take fruits until a fruit's name length is greater than or equal to 8.
     * Enumerable<String> result = fruits.takeWhile(fruit -> fruit.length() < 8);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // apple
     * // banana
     * // mango
     * // orange
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An enumerable that contains the elements from the input sequence that occur before the element at which the test no longer passes.
     * @see #takeWhile(BinPredicate)
     * @see #take(int)
     */
    @NotNull
    Enumerable<T> takeWhile(@NotNull Predicate<? super T> predicate);

    /**
     * <p>Returns elements from a sequence as long as a specified condition is true.
     * The element's index is used in the logic of the predicate function.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> amounts = Linq.of(5000, 2500, 9000, 8000, 6500, 4000, 1500, 5500);
     *
     * // Take elements as long as the element's index multiplied by 1000
     * // is strictly less than the element itself.
     * Enumerable<Integer> result = amounts.takeWhile((amount, index) -> amount > index * 1000);
     *
     * result.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 5000
     * // 2500
     * // 9000
     * // 8000
     * // 6500
     * }</pre>
     *
     * @param predicate A function to test each source element for a condition; the second parameter of the function represents the index of the source element.
     * @return An enumerable that contains elements from the input sequence that occur before the element at which the test no longer passes.
     * @see #takeWhile(Predicate)
     */
    @NotNull
    Enumerable<T> takeWhile(@NotNull BinPredicate<? super T, Integer> predicate);

    /**
     * <p>Produces the set union of two sequences by using the default equality comparer.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> ints1 = Linq.of(5, 3, 9, 7, 5, 9, 3, 7);
     * Enumerable<Integer> ints2 = Linq.of(8, 3, 6, 4, 4, 9, 1, 0);
     *
     * Enumerable<Integer> union = ints1.union(ints2);
     *
     * union.forEach(n -> System.out.print(n + " "));
     *
     * // This code produces the following output:
     * //
     * // 5 3 9 7 8 6 4 1 0
     * }</pre>
     *
     * @param other An {@link Enumerable} whose distinct elements form the other set for the union.
     * @return An enumerable that contains the elements from both input sequences, excluding duplicates.
     * @see #union(Enumerable, Equalator)
     * @see #unionBy(Enumerable, Function)
     */
    @NotNull
    Enumerable<T> union(@NotNull Enumerable<? extends T> other);

    /**
     * <p>Produces the set union of two sequences by using a specified {@link Equalator}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> store1 = Linq.of("apple", "orange");
     * Enumerable<String> store2 = Linq.of("APPLE", "lemon");
     *
     * // Create an Equalator for case-insensitive string comparison.
     * Equalator<String> ignoreCaseEqualator = Equalator.of(
     *     String::equalsIgnoreCase,
     *     String::toLowerCase
     * );
     *
     * Enumerable<String> union = store1.union(store2, ignoreCaseEqualator);
     *
     * union.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // apple
     * // orange
     * // lemon
     * }</pre>
     *
     * @param other An {@link Enumerable} whose distinct elements form the other set for the union.
     * @param comparer The {@link Equalator} to compare values.
     * @return An enumerable that contains the elements from both input sequences, excluding duplicates.
     * @see #union(Enumerable)
     */
    @NotNull
    Enumerable<T> union(
        @NotNull Enumerable<? extends T> other,
        @NotNull Equalator<? super T> comparer
    );

    /**
     * <p>Produces the set union of two sequences according to a specified key selector function.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * record Planet(String name, int type) {}
     *
     * Enumerable<Planet> planets1 = Linq.of(
     *     new Planet("Mercury", 1),
     *     new Planet("Venus", 2)
     * );
     * Enumerable<Planet> planets2 = Linq.of(
     *     new Planet("Earth", 1), // Same type as Mercury
     *     new Planet("Mars", 3)
     * );
     *
     * // Union the planets by their type.
     * Enumerable<Planet> result = planets1.unionBy(planets2, Planet::type);
     *
     * result.forEach(p -> System.out.println(p.name()));
     *
     * // This code produces the following output:
     * //
     * // Mercury
     * // Venus
     * // Mars
     * }</pre>
     *
     * @param second An {@link Enumerable} whose distinct elements form the second set for the union.
     * @param keySelector A function to extract the key for each element.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An enumerable that contains the elements from both input sequences, excluding duplicates based on the extracted key.
     * @see #unionBy(Enumerable, Function, Equalator)
     */
    @NotNull
    <K> Enumerable<T> unionBy(
        @NotNull Enumerable<? extends T> second,
        @NotNull Function<? super T, ? extends K> keySelector
    );

    /**
     * <p>Produces the set union of two sequences according to a specified key selector function
     * and using a specified comparer.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * record Employee(String name, String department) {}
     *
     * Enumerable<Employee> team1 = Linq.of(
     *     new Employee("Alice", "SALES"),
     *     new Employee("Bob", "IT")
     * );
     * Enumerable<Employee> team2 = Linq.of(
     *     new Employee("Charlie", "sales"), // Same department, different case
     *     new Employee("Dave", "HR")
     * );
     *
     * Equalator<String> ignoreCaseEqualator = Equalator.of(
     *     String::equalsIgnoreCase,
     *     String::toLowerCase
     * );
     *
     * // Union the teams by their department name, ignoring case.
     * Enumerable<Employee> result = team1.unionBy(
     *     team2,
     *     Employee::department,
     *     ignoreCaseEqualator
     * );
     *
     * result.forEach(e -> System.out.println(e.name()));
     *
     * // This code produces the following output:
     * //
     * // Alice
     * // Bob
     * // Dave
     * }</pre>
     *
     * @param second An {@link Enumerable} whose distinct elements form the second set for the union.
     * @param keySelector A function to extract the key for each element.
     * @param comparer The {@link Equalator} to compare keys.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An enumerable that contains the elements from both input sequences, excluding duplicates based on the extracted key.
     * @see #unionBy(Enumerable, Function)
     */
    @NotNull
    <K> Enumerable<T> unionBy(
        @NotNull Enumerable<? extends T> second,
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Equalator<? super K> comparer
    );

    /**
     * <p>Filters a sequence of values based on a predicate.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "passionfruit", "banana", "mango",
     *                                     "orange", "blueberry", "grape", "strawberry");
     *
     * // Filter the sequence to include only those elements that have a length of less than 6.
     * Enumerable<String> query = fruits.where(fruit -> fruit.length() < 6);
     *
     * query.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // apple
     * // mango
     * // grape
     * }</pre>
     *
     * @param predicate A function to test each element for a condition.
     * @return An enumerable that contains elements from the input sequence that satisfy the condition.
     * @see #where(BinPredicate)
     */
    @NotNull
    Enumerable<T> where(@NotNull Predicate<? super T> predicate);

    /**
     * <p>Filters a sequence of values based on a predicate.
     * Each element's index is used in the logic of the predicate function.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(0, 30, 20, 15, 90, 85, 40, 75);
     *
     * // Filter the sequence to include only those elements whose value is
     * // less than or equal to their index multiplied by 10.
     * Enumerable<Integer> query = numbers.where((number, index) -> number <= index * 10);
     *
     * query.forEach(System.out::println);
     *
     * // This code produces the following output:
     * //
     * // 0
     * // 20
     * // 15
     * // 40
     * }</pre>
     *
     * @param predicate A function to test each source element for a condition;
     *                  the second parameter of the function represents the index of the source element.
     * @return An enumerable that contains elements from the input sequence that satisfy the condition.
     * @see #where(Predicate)
     */
    @NotNull
    Enumerable<T> where(@NotNull BinPredicate<? super T, Integer> predicate);

    /**
     * <p>Produces a sequence of tuples ({@link Pair}) with elements from the two specified sequences.</p>
     * <p>The method merges each element of the first sequence with an element that
     * has the same index in the second sequence. If the sequences do not have the same
     * number of elements, the method merges elements until it reaches the end of one of them.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(1, 2, 3, 4);
     * Enumerable<String> letters = Linq.of("A", "B", "C");
     *
     * // Zip the two sequences into a sequence of Pair objects.
     * Enumerable<Pair<Integer, String>> zipped = numbers.zip(letters);
     *
     * zipped.forEach(pair ->
     *     System.out.printf("Number: %d, Letter: %s\n", pair.getLeft(), pair.getRight())
     * );
     *
     * // This code produces the following output:
     * //
     * // Number: 1, Letter: A
     * // Number: 2, Letter: B
     * // Number: 3, Letter: C
     * }</pre>
     *
     * @param second The second sequence to merge.
     * @param <U> The type of the elements of the second input sequence.
     * @return An enumerable that contains pairs of elements from the two input sequences.
     * @see #zip(Enumerable, BinFunction)
     */
    @NotNull
    <U> Enumerable<Pair<T, U>> zip(@NotNull Enumerable<? extends U> second);

    /**
     * <p>Applies a specified function to the corresponding elements of two sequences,
     * producing a sequence of the results.</p>
     * <p>The method merges each element of the first sequence with an element that
     * has the same index in the second sequence. If the sequences do not have the same
     * number of elements, the method merges elements until it reaches the end of one of them.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(1, 2, 3, 4);
     * Enumerable<String> words = Linq.of("one", "two", "three");
     *
     * // Merge corresponding elements into a single string.
     * Enumerable<String> numbersAndWords = numbers.zip(
     *     words,
     *     (first, second) -> first + " " + second
     * );
     *
     * numbersAndWords.forEach(System.out::println);
     *
     * // This code produces the following output (notice that "4" is ignored
     * // because the 'words' array has only three elements):
     * //
     * // 1 one
     * // 2 two
     * // 3 three
     * }</pre>
     *
     * @param second The second sequence to merge.
     * @param resultSelector A function that specifies how to merge the elements from the two sequences.
     * @param <U> The type of the elements of the second input sequence.
     * @param <R> The type of the elements of the result sequence.
     * @return An enumerable that contains merged elements of two input sequences.
     * @see #zip(Enumerable)
     */
    @NotNull
    <U, R> Enumerable<R> zip(
        @NotNull Enumerable<? extends U> second,
        @NotNull BinFunction<? super T, ? super U, ? extends R> resultSelector
    );

    /**
     * <p>Creates an array from an enumerable sequence using the specified array generator function.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> packages = Linq.of("Coho Vineyard", "Wingtip Toys", "Adventure Works");
     *
     * // Create a string array from the enumerable sequence.
     * String[] array = packages.toArray(String[]::new);
     *
     * for (String pkg : array) {
     *     System.out.println(pkg);
     * }
     *
     * // This code produces the following output:
     * //
     * // Coho Vineyard
     * // Wingtip Toys
     * // Adventure Works
     * }</pre>
     *
     * @param generator A function which produces a new array of the desired type and the provided length.
     * @param <A> The element type of the resulting array.
     * @return An array that contains the elements from the input sequence.
     * @see #toArray()
     */
    @NotNull
    default T[] toArray(@NotNull IntFunction<T[]> generator) {
        NullCheck.requireNonNull(generator, "generator");

        return this.toList().toArray(generator);
    }

    /**
     * <p>Creates an array of {@link Object} from an enumerable sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "banana", "mango");
     *
     * // Create an Object array from the enumerable sequence.
     * Object[] array = fruits.toArray();
     *
     * for (Object obj : array) {
     *     System.out.println(obj);
     * }
     *
     * // This code produces the following output:
     * //
     * // apple
     * // banana
     * // mango
     * }</pre>
     *
     * @return An array of {@link Object} that contains the elements from the input sequence.
     * @see #toArray(IntFunction)
     */
    @NotNull
    default Object[] toArray() {
        return this.toList().toArray();
    }

    /**
     * <p>Creates a {@link java.util.Map} from an enumerable sequence according to a specified key selector function.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * record Package(String trackingNumber, double weight) {}
     *
     * Enumerable<Package> packages = Linq.of(
     *     new Package("1V0001", 1.5),
     *     new Package("1V0002", 2.2)
     * );
     *
     * // Create a map using TrackingNumber as the key and the Package object as the value.
     * Map<String, Package> dictionary = packages.toDictionary(Package::trackingNumber);
     *
     * for (Map.Entry<String, Package> entry : dictionary.entrySet()) {
     *     System.out.printf("Key: %s, Weight: %.1f\n", entry.getKey(), entry.getValue().weight());
     * }
     *
     * // This code produces the following output:
     * //
     * // Key: 1V0001, Weight: 1.5
     * // Key: 1V0002, Weight: 2.2
     * }</pre>
     *
     * @param keySelector A function to extract a key from each element.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return A {@link java.util.Map} that contains keys and values. The values are the original elements from the input sequence.
     * @throws NullPointerException If {@code keySelector} is {@code null}, or if it produces a {@code null} key.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys for two elements.
     * @see #toMap(Function, Function)
     */
    @NotNull
    <K> Map<K, T> toMap(
        @NotNull Function<? super T, ? extends K> keySelector
    );

    /**
     * <p>Creates a {@link Map} from an enumerable sequence according to specified key selector and element selector functions.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * record Employee(int id, String name) {}
     *
     * Enumerable<Employee> employees = Linq.of(
     *     new Employee(1001, "Alice"),
     *     new Employee(1002, "Bob")
     * );
     *
     * // Create a map using ID as the key and Name as the value.
     * Map<Integer, String> map = employees.toMap(
     *     Employee::id,
     *     Employee::name
     * );
     *
     * for (Map.Entry<Integer, String> entry : map.entrySet()) {
     *     System.out.printf("ID: %d, Name: %s\n", entry.getKey(), entry.getValue());
     * }
     *
     * // This code produces the following output:
     * //
     * // ID: 1001, Name: Alice
     * // ID: 1002, Name: Bob
     * }</pre>
     *
     * @param keySelector A function to extract a key from each element.
     * @param elementSelector A transform function to produce a result element value from each element.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <V> The type of the value returned by {@code elementSelector}.
     * @return A {@link Map} that contains values of type {@code V} selected from the input sequence.
     * @throws NullPointerException If {@code keySelector} or {@code elementSelector} is {@code null}, or if a key is {@code null}.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys for two elements.
     * @see #toMap(Function, Function, Equalator)
     */
    @NotNull
    <K, V> Map<K, V> toMap(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends V> elementSelector
    );

    /**
     * <p>Creates a {@link Map} from an enumerable sequence according to a specified key selector function and key comparer.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * record Package(String trackingNumber, double weight) {}
     *
     * Enumerable<Package> packages = Linq.of(
     *     new Package("1v0001", 1.5),
     *     new Package("1V0002", 2.2)
     * );
     *
     * Equalator<String> ignoreCaseEqualator = Equalator.of(
     *     String::equalsIgnoreCase,
     *     String::toLowerCase
     * );
     *
     * // Create a map using case-insensitive TrackingNumber as the key.
     * Map<String, Package> map = packages.toMap(
     *     Package::trackingNumber,
     *     ignoreCaseEqualator
     * );
     *
     * // Looking up "1V0001" will succeed even though the key is "1v0001".
     * System.out.println(map.containsKey("1V0001"));
     *
     * // This code produces the following output:
     * //
     * // true
     * }</pre>
     *
     * @param keySelector A function to extract a key from each element.
     * @param comparer An {@link Equalator} to compare keys.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return A {@link Map} that contains keys and values.
     * @throws NullPointerException If {@code keySelector} or {@code comparer} is {@code null}, or if a key is {@code null}.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     * @see #toMap(Function)
     */
    @NotNull
    <K> Map<K, T> toMap(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Equalator<? super K> comparer
    );

    /**
     * <p>Creates a {@link Map} from an enumerable sequence according to a specified key selector function, a comparer, and an element selector function.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * record Employee(int id, String department) {}
     *
     * Enumerable<Employee> employees = Linq.of(
     *     new Employee(1001, "Sales"),
     *     new Employee(1002, "IT")
     * );
     *
     * Equalator<String> ignoreCaseEqualator = Equalator.of(
     *     String::equalsIgnoreCase,
     *     String::toLowerCase
     * );
     *
     * // Create a map with Department as the key (case-insensitive) and ID as the value.
     * Map<String, Integer> map = employees.toMap(
     *     Employee::department,
     *     Employee::id,
     *     ignoreCaseEqualator
     * );
     *
     * System.out.println("Sales ID: " + map.get("sales"));
     *
     * // This code produces the following output:
     * //
     * // Sales ID: 1001
     * }</pre>
     *
     * @param keySelector A function to extract a key from each element.
     * @param elementSelector A transform function to produce a result element value from each element.
     * @param comparer An {@link Equalator} to compare keys.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <V> The type of the value returned by {@code elementSelector}.
     * @return A {@link Map} that contains values of type {@code V} selected from the input sequence.
     * @throws NullPointerException If {@code keySelector}, {@code elementSelector}, or {@code comparer} is {@code null}, or if a key is {@code null}.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     * @see #toMap(Function, Function)
     */
    @NotNull
    <K, V> Map<K, V> toMap(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends V> elementSelector,
        @NotNull Equalator<? super K> comparer
    );

    /**
     * <p>Creates an unmodifiable {@link Map} from an enumerable sequence according to a specified key selector function.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * record Package(String trackingNumber, double weight) {}
     *
     * Enumerable<Package> packages = Linq.of(
     *     new Package("1V0001", 1.5),
     *     new Package("1V0002", 2.2)
     * );
     *
     * Map<String, Package> map = packages.toUnmodifiableMap(Package::trackingNumber);
     * // map.put("1V0003", new Package("1V0003", 1.0)); // Throws UnsupportedOperationException
     * }</pre>
     *
     * @param keySelector A function to extract a key from each element.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An unmodifiable {@link Map} that contains keys and values.
     * @throws NullPointerException If {@code keySelector} is {@code null}, or if it produces a {@code null} key.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     */
    @NotNull
    default <K> Map<K, T> toUnmodifiableMap(@NotNull Function<? super T, ? extends K> keySelector) {
        return Collections.unmodifiableMap(toMap(keySelector));
    }

    /**
     * <p>Creates an unmodifiable {@link Map} from an enumerable sequence according to specified key selector and element selector functions.</p>
     *
     * @param keySelector A function to extract a key from each element.
     * @param elementSelector A transform function to produce a result element value from each element.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <V> The type of the value returned by {@code elementSelector}.
     * @return An unmodifiable {@link Map} that contains values of type {@code V} selected from the input sequence.
     * @throws NullPointerException If {@code keySelector} or {@code elementSelector} is {@code null}.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     */
    @NotNull
    default <K, V> Map<K, V> toUnmodifiableMap(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends V> elementSelector
    ) {
        return Collections.unmodifiableMap(toMap(keySelector, elementSelector));
    }

    /**
     * <p>Creates an unmodifiable {@link Map} from an enumerable sequence according to a specified key selector function and key comparer.</p>
     *
     * @param keySelector A function to extract a key from each element.
     * @param comparer An {@link Equalator} to compare keys.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An unmodifiable {@link Map} that contains keys and values.
     * @throws NullPointerException If parameters are {@code null}, or if it produces a {@code null} key.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     */
    @NotNull
    default <K> Map<K, T> toUnmodifiableMap(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Equalator<? super K> comparer
    ) {
        return Collections.unmodifiableMap(toMap(keySelector, comparer));
    }

    /**
     * <p>Creates an unmodifiable {@link Map} from an enumerable sequence according to a specified key selector function, a comparer, and an element selector function.</p>
     *
     * @param keySelector A function to extract a key from each element.
     * @param elementSelector A transform function to produce a result element value from each element.
     * @param comparer An {@link Equalator} to compare keys.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <V> The type of the value returned by {@code elementSelector}.
     * @return An unmodifiable {@link Map} that contains values of type {@code V} selected from the input sequence.
     * @throws NullPointerException If parameters are {@code null}, or if it produces a {@code null} key.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     */
    @NotNull
    default <K, V> Map<K, V> toUnmodifiableMap(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends V> elementSelector,
        @NotNull Equalator<? super K> comparer
    ) {
        return Collections.unmodifiableMap(toMap(keySelector, elementSelector, comparer));
    }

    /**
     * <p>Creates an unmodifiable {@link SortedMap} from an enumerable sequence according to a specified key selector function.
     * The keys are sorted according to their {@linkplain Comparable natural ordering}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * record Employee(int id, String name) {}
     *
     * Enumerable<Employee> employees = Linq.of(
     *     new Employee(1002, "Bob"),
     *     new Employee(1001, "Alice")
     * );
     *
     * // The map will be sorted by the employee ID (1001, then 1002)
     * SortedMap<Integer, String> map = employees.toUnmodifiableSortedMap(Employee::id, Employee::name);
     * }</pre>
     *
     * @param keySelector A function to extract a key from each element.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An unmodifiable {@link SortedMap} that contains keys and values.
     * @throws NullPointerException If {@code keySelector} is {@code null}, or if it produces a {@code null} key.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     * @throws ClassCastException If the keys cannot be cast to {@link Comparable}.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    default <K> SortedMap<K, T> toUnmodifiableSortedMap(@NotNull Function<? super T, ? extends K> keySelector) {
        SortedMap<K, T> sortedMap = new TreeMap<>((Comparator<? super K>) Comparator.naturalOrder());
        this.forEach(element -> {
            K key = keySelector.apply(element);
            if (sortedMap.containsKey(key)) throw new IllegalStateException("Duplicate key: " + key);
            sortedMap.put(key, element);
        });
        return Collections.unmodifiableSortedMap(sortedMap);
    }

    /**
     * <p>Creates an unmodifiable {@link SortedMap} from an enumerable sequence according to specified key selector and element selector functions.
     * The keys are sorted according to their {@linkplain Comparable natural ordering}.</p>
     *
     * @param keySelector A function to extract a key from each element.
     * @param elementSelector A transform function to produce a result element value from each element.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <V> The type of the value returned by {@code elementSelector}.
     * @return An unmodifiable {@link SortedMap} that contains values of type {@code V}.
     * @throws NullPointerException If selectors are {@code null}.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     * @throws ClassCastException If the keys cannot be cast to {@link Comparable}.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    default <K, V> SortedMap<K, V> toUnmodifiableSortedMap(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends V> elementSelector
    ) {
        SortedMap<K, V> sortedMap = new TreeMap<>((Comparator<? super K>) Comparator.naturalOrder());
        this.forEach(element -> {
            K key = keySelector.apply(element);
            if (sortedMap.containsKey(key)) throw new IllegalStateException("Duplicate key: " + key);
            sortedMap.put(key, elementSelector.apply(element));
        });
        return Collections.unmodifiableSortedMap(sortedMap);
    }

    /**
     * <p>Creates an unmodifiable {@link SortedMap} from an enumerable sequence according to a specified key selector function and key comparer.</p>
     *
     * @param keySelector A function to extract a key from each element.
     * @param keyComparator A {@link Comparator} to sort the keys.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @return An unmodifiable {@link SortedMap} that contains keys and values.
     * @throws NullPointerException If parameters are {@code null}, or if it produces a {@code null} key.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     */
    @NotNull
    default <K> SortedMap<K, T> toUnmodifiableSortedMap(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Comparator<? super K> keyComparator
    ) {
        SortedMap<K, T> sortedMap = new TreeMap<>(keyComparator);
        this.forEach(element -> {
            K key = keySelector.apply(element);
            if (sortedMap.containsKey(key)) throw new IllegalStateException("Duplicate key: " + key);
            sortedMap.put(key, element);
        });
        return Collections.unmodifiableSortedMap(sortedMap);
    }

    /**
     * <p>Creates an unmodifiable {@link SortedMap} from an enumerable sequence according to a specified key selector function, a key comparer, and an element selector function.</p>
     *
     * @param keySelector A function to extract a key from each element.
     * @param elementSelector A transform function to produce a result element value from each element.
     * @param keyComparator A {@link Comparator} to sort the keys.
     * @param <K> The type of the key returned by {@code keySelector}.
     * @param <V> The type of the value returned by {@code elementSelector}.
     * @return An unmodifiable {@link SortedMap} that contains values of type {@code V}.
     * @throws NullPointerException If parameters are {@code null}, or if it produces a {@code null} key.
     * @throws IllegalStateException If {@code keySelector} produces duplicate keys.
     */
    @NotNull
    default <K, V> SortedMap<K, V> toUnmodifiableSortedMap(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Function<? super T, ? extends V> elementSelector,
        @NotNull Comparator<? super K> keyComparator
    ) {
        SortedMap<K, V> sortedMap = new TreeMap<>(keyComparator);
        this.forEach(element -> {
            K key = keySelector.apply(element);
            if (sortedMap.containsKey(key)) throw new IllegalStateException("Duplicate key: " + key);
            sortedMap.put(key, elementSelector.apply(element));
        });
        return Collections.unmodifiableSortedMap(sortedMap);
    }


    /**
     * <p>Creates a {@link List} from an enumerable sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "passionfruit", "banana", "mango");
     *
     * // Create a list from the enumerable sequence.
     * List<String> list = fruits.toList();
     *
     * for (String fruit : list) {
     *     System.out.println(fruit);
     * }
     *
     * // This code produces the following output:
     * //
     * // apple
     * // passionfruit
     * // banana
     * // mango
     * }</pre>
     *
     * @return A {@link List} that contains elements from the input sequence.
     * @see #toUnmodifiableList()
     */
    @NotNull
    default List<T> toList() {
        final List<T> list = new ArrayList<>();
        this.forEach(list::add);
        return list;
    }

    /**
     * <p>Creates a {@link List} from an enumerable sequence and sorts the elements
     * according to their {@linkplain Comparable natural ordering}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(5, 1, 3, 2, 5, 4);
     *
     * // Create a sorted list from the enumerable sequence.
     * // Unlike a Set, a List retains duplicate elements.
     * List<Integer> sortedList = numbers.toSortedList();
     *
     * for (Integer number : sortedList) {
     *     System.out.print(number + " ");
     * }
     *
     * // This code produces the following output:
     * //
     * // 1 2 3 4 5 5
     * }</pre>
     *
     * @return A sorted {@link List} that contains elements from the input sequence.
     * @throws ClassCastException If the elements cannot be cast to {@link Comparable}, or if they are not mutually comparable.
     * @see #toSortedList(Comparator)
     * @see #toUnmodifiableSortedList()
     */
    @NotNull
    @SuppressWarnings("unchecked")
    default List<T> toSortedList() {
        List<T> list = toList();
        list.sort((Comparator<? super T>) Comparator.naturalOrder());
        return list;
    }

    /**
     * <p>Creates a {@link List} from an enumerable sequence and sorts the elements
     * using the specified {@link Comparator}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("passionfruit", "apple", "banana", "mango");
     *
     * // Create a sorted list using a custom comparator (e.g., sort by string length).
     * List<String> sortedList = fruits.toSortedList(
     *     Comparator.comparingInt(String::length)
     * );
     *
     * for (String fruit : sortedList) {
     *     System.out.println(fruit);
     * }
     *
     * // This code produces the following output:
     * //
     * // apple
     * // mango
     * // banana
     * // passionfruit
     * }</pre>
     *
     * @param comparator A {@link Comparator} to sort the elements.
     * @return A sorted {@link List} that contains elements from the input sequence.
     * @throws NullPointerException If {@code comparator} is {@code null}.
     * @see #toSortedList()
     * @see #toUnmodifiableSortedList(Comparator)
     */
    @NotNull
    default List<T> toSortedList(@NotNull Comparator<? super T> comparator) {
        List<T> list = toList();
        list.sort(comparator);
        return list;
    }

    /**
     * <p>Creates an unmodifiable {@link List} from an enumerable sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("apple", "banana", "mango");
     *
     * // Create an unmodifiable list from the enumerable sequence.
     * List<String> unmodifiableList = fruits.toUnmodifiableList();
     *
     * // unmodifiableList.add("orange"); // This will throw UnsupportedOperationException
     *
     * for (String fruit : unmodifiableList) {
     *     System.out.println(fruit);
     * }
     *
     * // This code produces the following output:
     * //
     * // apple
     * // banana
     * // mango
     * }</pre>
     *
     * @return An unmodifiable {@link List} that contains elements from the input sequence.
     * @see #toList()
     */
    @NotNull
    default List<T> toUnmodifiableList() {
        return Collections.unmodifiableList(toList());
    }

    /**
     * <p>Creates an unmodifiable {@link List} from an enumerable sequence, with elements
     * sorted according to their {@linkplain Comparable natural ordering}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(5, 1, 3, 2, 5, 4);
     *
     * // Create an unmodifiable sorted list.
     * List<Integer> unmodifiableSortedList = numbers.toUnmodifiableSortedList();
     *
     * // unmodifiableSortedList.add(6); // Throws UnsupportedOperationException
     * }</pre>
     *
     * @return An unmodifiable sorted {@link List} that contains elements from the input sequence.
     * @throws ClassCastException If the elements cannot be cast to {@link Comparable}, or if they are not mutually comparable.
     * @see #toSortedList()
     */
    @NotNull
    default List<T> toUnmodifiableSortedList() {
        return Collections.unmodifiableList(toSortedList());
    }

    /**
     * <p>Creates an unmodifiable {@link List} from an enumerable sequence, with elements
     * sorted using the specified {@link Comparator}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("passionfruit", "apple", "banana", "mango");
     *
     * // Create an unmodifiable sorted list using a custom comparator.
     * List<String> unmodifiableSortedList = fruits.toUnmodifiableSortedList(
     *     Comparator.comparingInt(String::length)
     * );
     *
     * // unmodifiableSortedList.sort(...); // Throws UnsupportedOperationException
     * }</pre>
     *
     * @param comparator A {@link Comparator} to sort the elements.
     * @return An unmodifiable sorted {@link List} that contains elements from the input sequence.
     * @throws NullPointerException If {@code comparator} is {@code null}.
     * @see #toSortedList(Comparator)
     */
    @NotNull
    default List<T> toUnmodifiableSortedList(@NotNull Comparator<? super T> comparator) {
        return Collections.unmodifiableList(toSortedList(comparator));
    }


    /**
     * <p>Creates a {@link Set} from an enumerable sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(1, 2, 2, 3, 3, 3, 4);
     *
     * // Create a hash set from the enumerable sequence to remove duplicates.
     * Set<Integer> set = numbers.toHashSet();
     *
     * for (Integer number : set) {
     *     System.out.println(number);
     * }
     *
     * // This code produces output similar to the following (order is not guaranteed):
     * //
     * // 1
     * // 2
     * // 3
     * // 4
     * }</pre>
     *
     * @return A {@link Set} that contains elements from the input sequence.
     * @see #toHashSet(Equalator)
     * @see #toUnmodifiableHashSet()
     */
    @NotNull
    default Set<T> toHashSet() {
        final HashSet<T> set = new HashSet<>();
        this.forEach(set::add);
        return set;
    }

    /**
     * <p>Creates an unmodifiable {@link Set} from an enumerable sequence.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(1, 2, 2, 3);
     *
     * // Create an unmodifiable set from the enumerable sequence.
     * Set<Integer> unmodifiableSet = numbers.toUnmodifiableHashSet();
     *
     * // unmodifiableSet.add(4); // This will throw UnsupportedOperationException
     *
     * for (Integer number : unmodifiableSet) {
     *     System.out.println(number);
     * }
     *
     * // This code produces output similar to the following:
     * //
     * // 1
     * // 2
     * // 3
     * }</pre>
     *
     * @return An unmodifiable {@link Set} that contains elements from the input sequence.
     * @see #toHashSet()
     */
    @NotNull
    default Set<T> toUnmodifiableHashSet() {
        return Collections.unmodifiableSet(toHashSet());
    }

    /**
     * <p>Creates a {@link SortedSet} from an enumerable sequence.
     * The elements are sorted according to their {@linkplain Comparable natural ordering}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(5, 1, 3, 2, 5, 4);
     *
     * // Create a sorted set from the enumerable sequence to remove duplicates and sort.
     * SortedSet<Integer> sortedSet = numbers.toSortedSet();
     *
     * for (Integer number : sortedSet) {
     *     System.out.print(number + " ");
     * }
     *
     * // This code produces the following output:
     * //
     * // 1 2 3 4 5
     * }</pre>
     *
     * @return A {@link SortedSet} that contains elements from the input sequence.
     * @throws ClassCastException If the elements cannot be cast to {@link Comparable}, or if they are not mutually comparable.
     * @see #toSortedSet(Comparator)
     * @see #toUnmodifiableSortedSet()
     */
    @NotNull
    @SuppressWarnings("unchecked")
    default SortedSet<T> toSortedSet() {
        SortedSet<T> sortedSet = new TreeSet<>((Comparator<? super T>) Comparator.naturalOrder());
        for (T element : this) {
            sortedSet.add(element);
        }
        return sortedSet;
    }

    /**
     * <p>Creates a {@link SortedSet} from an enumerable sequence using the specified {@link Comparator}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("passionfruit", "apple", "banana", "mango");
     *
     * // Create a sorted set using a custom comparator (e.g., sort by string length).
     * SortedSet<String> sortedSet = fruits.toSortedSet(
     *     Comparator.comparingInt(String::length)
     * );
     *
     * for (String fruit : sortedSet) {
     *     System.out.println(fruit);
     * }
     *
     * // This code produces the following output:
     * //
     * // apple
     * // mango
     * // banana
     * // passionfruit
     * }</pre>
     *
     * @param comparator A {@link Comparator} to sort the elements.
     * @return A {@link SortedSet} that contains elements from the input sequence.
     * @throws NullPointerException If {@code comparator} is {@code null}.
     * @see #toSortedSet()
     * @see #toUnmodifiableSortedSet(Comparator)
     */
    @NotNull
    default SortedSet<T> toSortedSet(@NotNull Comparator<? super T> comparator) {
        SortedSet<T> sortedSet = new TreeSet<>(comparator);
        for (T element : this) {
            sortedSet.add(element);
        }
        return sortedSet;
    }


    /**
     * <p>Creates an unmodifiable {@link SortedSet} from an enumerable sequence.
     * The elements are sorted according to their {@linkplain Comparable natural ordering}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<Integer> numbers = Linq.of(5, 1, 3, 2, 5, 4);
     *
     * // Create an unmodifiable sorted set from the enumerable sequence.
     * SortedSet<Integer> unmodifiableSortedSet = numbers.toUnmodifiableSortedSet();
     *
     * // unmodifiableSortedSet.add(6); // This will throw UnsupportedOperationException
     *
     * for (Integer number : unmodifiableSortedSet) {
     *     System.out.print(number + " ");
     * }
     *
     * // This code produces the following output:
     * //
     * // 1 2 3 4 5
     * }</pre>
     *
     * @return An unmodifiable {@link SortedSet} that contains elements from the input sequence.
     * @throws ClassCastException If the elements cannot be cast to {@link Comparable}, or if they are not mutually comparable.
     * @see #toSortedSet()
     * @see #toUnmodifiableSortedSet(Comparator)
     */
    @NotNull
    default SortedSet<T> toUnmodifiableSortedSet() {
        return Collections.unmodifiableSortedSet(toSortedSet());
    }

    /**
     * <p>Creates an unmodifiable {@link SortedSet} from an enumerable sequence using the specified {@link Comparator}.</p>
     * <b>Usage:</b>
     * <pre>{@code
     * Enumerable<String> fruits = Linq.of("passionfruit", "apple", "banana", "mango");
     *
     * // Create an unmodifiable sorted set using a custom comparator.
     * SortedSet<String> unmodifiableSortedSet = fruits.toUnmodifiableSortedSet(
     *     Comparator.comparingInt(String::length)
     * );
     *
     * for (String fruit : unmodifiableSortedSet) {
     *     System.out.println(fruit);
     * }
     *
     * // This code produces the following output:
     * //
     * // apple
     * // mango
     * // banana
     * // passionfruit
     * }</pre>
     *
     * @param comparator A {@link Comparator} to sort the elements.
     * @return An unmodifiable {@link SortedSet} that contains elements from the input sequence.
     * @throws NullPointerException If {@code comparator} is {@code null}.
     * @see #toUnmodifiableSortedSet()
     * @see #toSortedSet(Comparator)
     */
    @NotNull
    default SortedSet<T> toUnmodifiableSortedSet(@NotNull Comparator<? super T> comparator) {
        return Collections.unmodifiableSortedSet(toSortedSet(comparator));
    }
}
