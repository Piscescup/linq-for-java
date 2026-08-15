package io.github.piscescup.linq4j;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Represents an ordered sequence whose elements can be further ordered
 * by one or more additional keys.
 *
 * <p>An {@code OrderedEnumerable} is produced by an ordering operation
 * and provides methods for performing secondary, tertiary, and subsequent
 * ordering operations.</p>
 *
 * <p>When elements have equal keys at one ordering level, the subsequent
 * ordering level is used to determine their relative order.</p>
 *
 * @param <T> The type of elements in the sequence.
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface OrderedEnumerable<T> extends Enumerable<T> {

    /**
     * <p>Performs a subsequent ascending ordering of the elements in this
     * ordered sequence according to a specified key selector.</p>
     *
     * <p>The selected keys are compared according to their natural
     * ordering.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int grade, int age) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 90, 20),
     *     new Student("Bob", 90, 18),
     *     new Student("Charlie", 80, 21)
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students
     *         .orderByInt(Student::grade)
     *         .thenBy(Student::name);
     *
     * // Students with the same grade are ordered by name.
     * }</pre>
     *
     * @param <K> The type of the subsequent ordering key.
     * @param keySelector A function that extracts the subsequent ordering
     *                    key from each element.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    <K extends Comparable<? super K>> OrderedEnumerable<T> thenBy(
        @NotNull Function<? super T, ? extends K> keySelector
    );


    /**
     * <p>Performs a subsequent ascending ordering of the elements in this
     * ordered sequence according to a specified key selector and comparator.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, String className) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", "Class A"),
     *     new Student("Bob", "Class A"),
     *     new Student("Charlie", "Class B")
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students
     *         .orderBy(Student::className)
     *         .thenBy(
     *             Student::name,
     *             Comparator.naturalOrder()
     *         );
     * }</pre>
     *
     * @param <K> The type of the subsequent ordering key.
     * @param keySelector A function that extracts the subsequent ordering
     *                    key from each element.
     * @param comparator The comparator used to compare the selected keys.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} or
     *                              {@code comparator} is {@code null}.
     */
    @NotNull
    <K> OrderedEnumerable<T> thenBy(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    );


    /**
     * <p>Performs a subsequent descending ordering of the elements in this
     * ordered sequence according to a specified key selector.</p>
     *
     * <p>The selected keys are compared according to their natural
     * ordering.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int grade, int age) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 90, 20),
     *     new Student("Bob", 90, 18),
     *     new Student("Charlie", 80, 21)
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students
     *         .orderByInt(Student::grade)
     *         .thenByDescending(Student::age);
     *
     * // Students with the same grade are ordered by age descending.
     * }</pre>
     *
     * @param <K> The type of the subsequent ordering key.
     * @param keySelector A function that extracts the subsequent ordering
     *                    key from each element.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    <K extends Comparable<? super K>> OrderedEnumerable<T> thenByDescending(
        @NotNull Function<? super T, ? extends K> keySelector
    );


    /**
     * <p>Performs a subsequent descending ordering of the elements in this
     * ordered sequence according to a specified key selector and comparator.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int grade) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 90),
     *     new Student("Bob", 90),
     *     new Student("Charlie", 80)
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students
     *         .orderByInt(Student::grade)
     *         .thenByDescending(
     *             Student::name,
     *             Comparator.naturalOrder()
     *         );
     * }</pre>
     *
     * @param <K> The type of the subsequent ordering key.
     * @param keySelector A function that extracts the subsequent ordering
     *                    key from each element.
     * @param comparator The comparator used to compare the selected keys.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} or
     *                              {@code comparator} is {@code null}.
     */
    @NotNull
    <K> OrderedEnumerable<T> thenByDescending(
        @NotNull Function<? super T, ? extends K> keySelector,
        @NotNull Comparator<? super K> comparator
    );


    /**
     * <p>Performs a subsequent ascending ordering according to an
     * {@code int} key.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int grade, int age) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 90, 20),
     *     new Student("Bob", 90, 18),
     *     new Student("Charlie", 80, 21)
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students
     *         .orderByInt(Student::grade)
     *         .thenByInt(Student::age);
     *
     * // Students with the same grade are ordered by age ascending.
     * }</pre>
     *
     * @param keySelector A function that extracts an {@code int} key from
     *                    each element.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> thenByInt(
        @NotNull ToIntFunction<? super T> keySelector
    );


    /**
     * <p>Performs a subsequent descending ordering according to an
     * {@code int} key.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Student(String name, int grade, int age) {}
     *
     * Enumerable<Student> students = Linq.of(
     *     new Student("Alice", 90, 20),
     *     new Student("Bob", 90, 18),
     *     new Student("Charlie", 80, 21)
     * );
     *
     * OrderedEnumerable<Student> ordered =
     *     students
     *         .orderByInt(Student::grade)
     *         .thenByIntDescending(Student::age);
     *
     * // Students with the same grade are ordered by age descending.
     * }</pre>
     *
     * @param keySelector A function that extracts an {@code int} key from
     *                    each element.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> thenByIntDescending(
        @NotNull ToIntFunction<? super T> keySelector
    );


    /**
     * <p>Performs a subsequent ascending ordering according to a
     * {@code long} key.</p>
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
     *     files
     *         .orderBy(FileInfo::name)
     *         .thenByLong(FileInfo::size);
     *
     * // Files with the same name are ordered by size ascending.
     * }</pre>
     *
     * @param keySelector A function that extracts a {@code long} key from
     *                    each element.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> thenByLong(
        @NotNull ToLongFunction<? super T> keySelector
    );


    /**
     * <p>Performs a subsequent descending ordering according to a
     * {@code long} key.</p>
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
     *     files
     *         .orderBy(FileInfo::name)
     *         .thenByLongDescending(FileInfo::size);
     *
     * // Files with the same name are ordered by size descending.
     * }</pre>
     *
     * @param keySelector A function that extracts a {@code long} key from
     *                    each element.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> thenByLongDescending(
        @NotNull ToLongFunction<? super T> keySelector
    );


    /**
     * <p>Performs a subsequent ascending ordering according to a
     * {@code double} key.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, double price, double rating) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("Apple", 2.5, 4.5),
     *     new Product("Orange", 1.8, 4.2),
     *     new Product("Banana", 2.1, 4.5)
     * );
     *
     * OrderedEnumerable<Product> ordered =
     *     products
     *         .orderByDouble(Product::rating)
     *         .thenByDouble(Product::price);
     *
     * // Products with the same rating are ordered by price ascending.
     * }</pre>
     *
     * @param keySelector A function that extracts a {@code double} key from
     *                    each element.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> thenByDouble(
        @NotNull ToDoubleFunction<? super T> keySelector
    );


    /**
     * <p>Performs a subsequent descending ordering according to a
     * {@code double} key.</p>
     *
     * <b>Usage:</b>
     * <pre>{@code
     * record Product(String name, double price, double rating) {}
     *
     * Enumerable<Product> products = Linq.of(
     *     new Product("Apple", 2.5, 4.5),
     *     new Product("Orange", 1.8, 4.2),
     *     new Product("Banana", 2.1, 4.5)
     * );
     *
     * OrderedEnumerable<Product> ordered =
     *     products
     *         .orderByDouble(Product::rating)
     *         .thenByDoubleDescending(Product::price);
     *
     * // Products with the same rating are ordered by price descending.
     * }</pre>
     *
     * @param keySelector A function that extracts a {@code double} key from
     *                    each element.
     * @return An ordered sequence containing the additionally ordered
     *         elements.
     * @throws NullPointerException If {@code keySelector} is {@code null}.
     */
    @NotNull
    OrderedEnumerable<T> thenByDoubleDescending(
        @NotNull ToDoubleFunction<? super T> keySelector
    );
}