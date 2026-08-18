package io.github.piscescup.linq4j;

import io.github.piscescup.equalators.StringEqualators;
import io.github.piscescup.interfaces.Equalator;
import io.github.piscescup.interfaces.HashEqualator;
import io.github.piscescup.interfaces.Pair;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the public methods declared by {@link Enumerable}.
 *
 * <p>The tests intentionally create a fresh Enumerable for almost every assertion,
 * because Enumerable instances are documented as single-use query pipelines.</p>
 */
class EnumerableTest {

    private static final Equalator<String> CASE_INSENSITIVE =
        String::equalsIgnoreCase;


    private record Person(String name, String department, int age) {}
    private record Order(String category, int quantity) {}
    private record LeftRow(int id, String value) {}
    private record RightRow(int id, String value) {}

    private static <T> List<T> list(Enumerable<T> enumerable) {
        return enumerable.toList();
    }

    private static <L, R> List<String> pairStrings(Enumerable<Pair<L, R>> enumerable) {
        
        
        return enumerable
            .select(p -> String.valueOf(p.getLeft()) + "=" + String.valueOf(p.getRight()))
            .toList();
    }

    // -------------------------------------------------------------------------
    // aggregateToResult / aggregateBy
    // -------------------------------------------------------------------------

    @Test
    void aggregateToResult_allOverloads() {
        assertEquals(
            "[10]",
            Linq.of(1, 2, 3, 4)
                .aggregateToResult(
                    0,
                    Integer::sum,
                    sum -> "[" + sum + "]"
                )
        );

        assertEquals(
            10,
            Linq.of(1, 2, 3, 4)
                .aggregateToResult(0, Integer::sum)
        );

        assertEquals(
            "cba",
            Linq.of("a", "b", "c")
                .aggregateToResult((left, right) -> right + left)
        );
    }

    @Test
    void aggregateBy_allOverloads() {
        assertEquals(
            List.of("a=2", "b=1"),
            pairStrings(
                Linq.of("apple", "apricot", "banana")
                    .aggregateBySeed(
                        s -> s.substring(0, 1),
                        0,
                        (count, s) -> count + 1
                    )
            )
        );

        assertEquals(
            List.of("a=2", "B=2"),
            pairStrings(
                Linq.of("a", "A", "B", "b")
                    .aggregateBySeed(
                        s -> s,
                        0,
                        (count, s) -> count + 1,
                        CASE_INSENSITIVE
                    )
            )
        );

        assertEquals(
            List.of("a=12", "b=11"),
            pairStrings(
                Linq.of("apple", "apricot", "banana")
                    .aggregateBy(
                        s -> s.substring(0, 1),
                        key -> 10,
                        (count, s) -> count + 1
                    )
            )
        );

        assertEquals(
            List.of("a=12", "B=12"),
            pairStrings(
                Linq.of("a", "A", "B", "b")
                    .aggregateBy(
                        s -> s,
                        key -> 10,
                        (count, s) -> count + 1,
                        CASE_INSENSITIVE
                    )
            )
        );
    }

    @Test
    void aggregateByInHash_allOverloads() {
        assertEquals(
            List.of("fruit=5", "book=1"),
            pairStrings(
                Linq.of(
                        new Order("fruit", 2),
                        new Order("book", 1),
                        new Order("fruit", 3)
                    )
                    .aggregateBySeedInHash(
                        Order::category,
                        0,
                        (sum, order) -> sum + order.quantity(),
                        HashEqualator.defaultHashEqualator()
                    )
            )
        );

        assertEquals(
            List.of("A=2", "B=2"),
            pairStrings(
                Linq.of("A", "a", "B", "b")
                    .aggregateByInHash(
                        s -> s,
                        key -> 0,
                        (count, s) -> count + 1,
                        StringEqualators.ORDINAL_IGNORE_CASE
                    )
            )
        );
    }

    // -------------------------------------------------------------------------
    // Quantifiers / simple sequence operations / averages
    // -------------------------------------------------------------------------

    @Test
    void all_any_andShortCircuit() {
        assertTrue(Linq.<Integer>of().all(x -> x > 0));
        assertTrue(Linq.of(2, 4, 6).all(x -> x % 2 == 0));
        assertFalse(Linq.of(2, 3, 6).all(x -> x % 2 == 0));

        assertFalse(Linq.<Integer>of().any());
        assertTrue(Linq.of(1).any());
        assertTrue(Linq.of(1, 2, 3).any(x -> x == 2));
        assertFalse(Linq.of(1, 2, 3).any(x -> x > 10));

        AtomicInteger calls = new AtomicInteger();
        assertTrue(
            Linq.of(1, 2, 3, 4).any(x -> {
                calls.incrementAndGet();
                return x == 2;
            })
        );
        assertEquals(2, calls.get());
    }

    @Test
    void append_prepend_concat_reverse_defaultIfEmpty() {
        assertEquals(
            List.of(1, 2, 3, 4),
            list(Linq.of(1, 2, 3).append(4))
        );

        assertEquals(
            Arrays.asList(null, 1, 2, 3),
            list(Linq.of(1, 2, 3).prepend(null))
        );

        assertEquals(
            List.of(1, 2, 3, 4),
            list(Linq.of(1, 2).concat(Linq.of(3, 4)))
        );

        assertEquals(
            List.of(3, 2, 1),
            list(Linq.of(1, 2, 3).reverse())
        );

        assertEquals(
            List.of(1, 2),
            list(Linq.of(1, 2).defaultIfEmpty(99))
        );

        assertEquals(
            List.of(99),
            list(Linq.<Integer>of().defaultIfEmpty(99))
        );
    }

    @Test
    void averageToDouble_and_averageToDecimal() {
        assertEquals(
            2.5,
            Linq.of(1, 2, 3, 4).averageToDouble(Integer::doubleValue),
            1e-12
        );

        assertEquals(
            new BigDecimal("2"),
            Linq.of(
                    new BigDecimal("1"),
                    new BigDecimal("2"),
                    new BigDecimal("3")
                )
                .averageToDecimal(x -> x, RoundingMode.HALF_UP)
        );

        assertThrows(
            ArithmeticException.class,
            () -> Linq.<Integer>of().averageToDouble(Integer::doubleValue)
        );

        assertThrows(
            ArithmeticException.class,
            () -> Linq.<BigDecimal>of()
                .averageToDecimal(x -> x, RoundingMode.HALF_UP)
        );

        assertThrows(
            NullPointerException.class,
            () -> Linq.of(1).averageToDouble(null)
        );

        assertThrows(
            NullPointerException.class,
            () -> Linq.of(BigDecimal.ONE)
                .averageToDecimal(x -> x, null)
        );
    }

    @Test
    void cast() {
        assertEquals(
            List.of("a", "b"),
            list(Linq.<Object>of("a", "b").cast(String.class))
        );

        assertThrows(
            ClassCastException.class,
            () -> Linq.<Object>of("a", 1)
                .cast(String.class)
                .toList()
        );

        assertThrows(
            NullPointerException.class,
            () -> Linq.of(1).cast(null)
        );
    }

    @Test
    void chunk() {
        List<List<Integer>> actual = Linq.of(1, 2, 3, 4, 5, 6, 7)
            .chunk(3)
            .select(Enumerable::toList)
            .toList();

        assertEquals(
            List.of(
                List.of(1, 2, 3),
                List.of(4, 5, 6),
                List.of(7)
            ),
            actual
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> Linq.of(1, 2, 3).chunk(0)
        );
    }

    // -------------------------------------------------------------------------
    // contains / count / countBy
    // -------------------------------------------------------------------------

    @Test
    void contains_allOverloads() {
        assertTrue(Linq.of("a", "b", "c").contains("b"));
        assertFalse(Linq.of("a", "b", "c").contains("x"));

        assertTrue(
            Linq.of("Apple", "Banana")
                .contains("apple", CASE_INSENSITIVE)
        );

        assertTrue(
            Linq.of("a", "b")
                .contains("a", null)
        );
    }

    @Test
    void count_allOverloads() {
        assertEquals(4L, Linq.of(1, 2, 3, 4).count());
        assertEquals(2L, Linq.of(1, 2, 3, 4).count(x -> x % 2 == 0));

        assertThrows(
            NullPointerException.class,
            () -> Linq.of(1, 2, 3).count(null)
        );
    }

    @Test
    void countBy_allOverloads() {
        assertEquals(
            List.of("a=2", "b=1"),
            pairStrings(
                Linq.of("apple", "apricot", "banana")
                    .countBy(s -> s.substring(0, 1))
            )
        );

        assertEquals(
            List.of("IT=3", "HR=1"),
            pairStrings(
                Linq.of("IT", "it", "HR", "It")
                    .countBy(s -> s, CASE_INSENSITIVE)
            )
        );

        assertEquals(
            List.of("A=2", "B=1"),
            pairStrings(
                Linq.of("A", "a", "B")
                    .countByInHash(s -> s, StringEqualators.ORDINAL_IGNORE_CASE)
            )
        );
    }

    // -------------------------------------------------------------------------
    // distinct / except / intersect / union
    // -------------------------------------------------------------------------

    @Test
    void distinct_allVariants() {
        assertEquals(
            List.of(1, 2, 3),
            list(Linq.of(1, 1, 2, 3, 2).distinct())
        );

        assertEquals(
            List.of("A", "B"),
            list(Linq.of("A", "a", "B", "b").distinct(CASE_INSENSITIVE))
        );

        assertEquals(
            List.of("A", "B"),
            list(Linq.of("A", "a", "B", "b").distinctInHash(StringEqualators.ORDINAL_IGNORE_CASE))
        );
    }

    @Test
    void distinctBy_allVariants() {
        List<Person> people = List.of(
            new Person("Alice", "IT", 20),
            new Person("Bob", "HR", 21),
            new Person("Carol", "IT", 22)
        );

        assertEquals(
            List.of("Alice", "Bob"),
            Linq.of(people)
                .distinctBy(Person::department)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Alice", "Carol"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "it", 21),
                    new Person("Carol", "HR", 22)
                )
                .distinctBy(Person::department, CASE_INSENSITIVE)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Alice", "Carol"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "it", 21),
                    new Person("Carol", "HR", 22)
                )
                .distinctByInHash(Person::department, StringEqualators.ORDINAL_IGNORE_CASE)
                .select(Person::name)
                .toList()
        );
    }

    @Test
    void except_allVariants() {
        assertEquals(
            List.of(1, 3),
            list(Linq.of(1, 2, 2, 3, 4).except(Linq.of(2, 4)))
        );

        assertEquals(
            List.of("B"),
            list(
                Linq.of("A", "a", "B")
                    .except(Linq.of("a"), CASE_INSENSITIVE)
            )
        );

        assertEquals(
            List.of("B"),
            list(
                Linq.of("A", "a", "B")
                    .exceptInHash(Linq.of("a"), StringEqualators.ORDINAL_IGNORE_CASE)
            )
        );
    }

    @Test
    void exceptBy_allVariants() {
        List<Person> people = List.of(
            new Person("Alice", "IT", 20),
            new Person("Bob", "HR", 21),
            new Person("Carol", "Sales", 22)
        );

        assertEquals(
            List.of("Alice", "Carol"),
            Linq.of(people)
                .exceptBy(Linq.of("HR"), Person::department)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Bob", "Carol"),
            Linq.of(people)
                .exceptBy(Linq.of("it"), Person::department, CASE_INSENSITIVE)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Bob", "Carol"),
            Linq.of(people)
                .exceptByInHash(Linq.of("it"), Person::department, StringEqualators.ORDINAL_IGNORE_CASE)
                .select(Person::name)
                .toList()
        );
    }

    @Test
    void intersect_allVariants() {
        assertEquals(
            List.of(2, 3),
            list(Linq.of(1, 2, 2, 3).intersect(Linq.of(2, 3, 4)))
        );

        assertEquals(
            List.of("A", "B"),
            list(
                Linq.of("A", "a", "B", "C")
                    .intersect(Linq.of("a", "b"), CASE_INSENSITIVE)
            )
        );

        assertEquals(
            List.of("A", "B"),
            list(
                Linq.of("A", "a", "B", "C")
                    .intersectInHash(Linq.of("a", "b"), StringEqualators.ORDINAL_IGNORE_CASE)
            )
        );
    }

    @Test
    void intersectBy_allVariants() {
        List<Person> people = List.of(
            new Person("Alice", "IT", 20),
            new Person("Bob", "HR", 21),
            new Person("Carol", "Sales", 22)
        );

        assertEquals(
            List.of("Alice", "Carol"),
            Linq.of(people)
                .intersectBy(Linq.of("IT", "Sales"), Person::department)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Alice", "Bob"),
            Linq.of(people)
                .intersectBy(Linq.of("it", "hr"), Person::department, CASE_INSENSITIVE)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Alice", "Bob"),
            Linq.of(people)
                .intersectByInHash(Linq.of("it", "hr"), Person::department, StringEqualators.ORDINAL_IGNORE_CASE)
                .select(Person::name)
                .toList()
        );
    }

    @Test
    void union_allVariants() {
        assertEquals(
            List.of(1, 2, 3, 4),
            list(Linq.of(1, 2, 2).union(Linq.of(2, 3, 4)))
        );

        assertEquals(
            List.of("A", "B", "C"),
            list(
                Linq.of("A", "B")
                    .union(Linq.of("a", "C"), CASE_INSENSITIVE)
            )
        );

        assertEquals(
            List.of("A", "B", "C"),
            list(
                Linq.of("A", "B")
                    .unionInHash(Linq.of("a", "C"), StringEqualators.ORDINAL_IGNORE_CASE)
            )
        );
    }

    @Test
    void unionBy_allVariants() {
        Person a = new Person("Alice", "IT", 20);
        Person b = new Person("Bob", "HR", 21);
        Person c = new Person("Carol", "it", 22);
        Person d = new Person("David", "Sales", 23);

        assertEquals(
            List.of("Alice", "Bob", "David"),
            Linq.of(a, b)
                .unionBy(Linq.of(a, d), Person::department)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Alice", "Bob", "David"),
            Linq.of(a, b)
                .unionBy(Linq.of(c, d), Person::department, CASE_INSENSITIVE)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Alice", "Bob", "David"),
            Linq.of(a, b)
                .unionByInHash(Linq.of(c, d), Person::department, StringEqualators.ORDINAL_IGNORE_CASE)
                .select(Person::name)
                .toList()
        );
    }

    // -------------------------------------------------------------------------
    // Element operators
    // -------------------------------------------------------------------------

    @Test
    void elementAt_elementAtOrNull_elementAtOrDefault() {
        assertEquals(20, Linq.of(10, 20, 30).elementAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.of(1, 2).elementAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.of(1, 2).elementAt(2));

        assertEquals(20, Linq.of(10, 20, 30).elementAtOrNull(1));
        assertNull(Linq.of(10, 20, 30).elementAtOrNull(-1));
        assertNull(Linq.of(10, 20, 30).elementAtOrNull(99));

        assertEquals(20, Linq.of(10, 20, 30).elementAtOrDefault(1, 99));
        assertEquals(99, Linq.of(10, 20, 30).elementAtOrDefault(-1, 99));
        assertEquals(99, Linq.of(10, 20, 30).elementAtOrDefault(99, 99));
    }

    @Test
    void first_allOverloads() {
        assertEquals(1, Linq.of(1, 2, 3).first());
        assertEquals(2, Linq.of(1, 2, 3, 4).first(x -> x % 2 == 0));

        assertThrows(NoSuchElementException.class, () -> Linq.<Integer>of().first());
        assertThrows(NoSuchElementException.class, () -> Linq.of(1, 3).first(x -> x % 2 == 0));
    }

    @Test
    void firstOrNull_allOverloads() {
        assertEquals(1, Linq.of(1, 2).firstOrNull());
        assertNull(Linq.<Integer>of().firstOrNull());

        assertEquals(2, Linq.of(1, 2, 4).firstOrNull(x -> x % 2 == 0));
        assertNull(Linq.of(1, 3).firstOrNull(x -> x % 2 == 0));
    }

    @Test
    void firstOrDefault_allOverloads() {
        assertEquals(1, Linq.of(1, 2).firstOrDefault(99));
        assertEquals(99, Linq.<Integer>of().firstOrDefault(99));

        assertEquals(2, Linq.of(1, 2, 4).firstOrDefault(x -> x % 2 == 0, 99));
        assertEquals(99, Linq.of(1, 3).firstOrDefault(x -> x % 2 == 0, 99));
    }

    @Test
    void last_allOverloads() {
        assertEquals(3, Linq.of(1, 2, 3).last());
        assertEquals(4, Linq.of(1, 2, 3, 4).last(x -> x % 2 == 0));

        assertThrows(NoSuchElementException.class, () -> Linq.<Integer>of().last());
        assertThrows(NoSuchElementException.class, () -> Linq.of(1, 3).last(x -> x % 2 == 0));
    }

    @Test
    void lastOrNull_allOverloads() {
        assertEquals(3, Linq.of(1, 2, 3).lastOrNull());
        assertNull(Linq.<Integer>of().lastOrNull());

        assertEquals(4, Linq.of(1, 2, 3, 4).lastOrNull(x -> x % 2 == 0));
        assertNull(Linq.of(1, 3).lastOrNull(x -> x % 2 == 0));
    }

    @Test
    void lastOrDefault_allOverloads() {
        assertEquals(3, Linq.of(1, 2, 3).lastOrDefault(99));
        assertEquals(99, Linq.<Integer>of().lastOrDefault(99));

        assertEquals(4, Linq.of(1, 2, 3, 4).lastOrDefault(x -> x % 2 == 0, 99));
        assertEquals(99, Linq.of(1, 3).lastOrDefault(x -> x % 2 == 0, 99));
    }

    @Test
    void single_allOverloads_andFailureCases() {
        assertEquals(1, Linq.of(1).single());
        assertThrows(NoSuchElementException.class, () -> Linq.<Integer>of().single());
        assertThrows(IllegalStateException.class, () -> Linq.of(1, 2).single());

        assertEquals(2, Linq.of(1, 2, 3).single(x -> x % 2 == 0));
        assertThrows(NoSuchElementException.class, () -> Linq.of(1, 3).single(x -> x % 2 == 0));
        assertThrows(IllegalStateException.class, () -> Linq.of(2, 4).single(x -> x % 2 == 0));
    }

    @Test
    void singleOrNull_allOverloads() {
        assertEquals(1, Linq.of(1).singleOrNull());
        assertNull(Linq.<Integer>of().singleOrNull());
        assertThrows(IllegalStateException.class, () -> Linq.of(1, 2).singleOrNull());

        assertEquals(2, Linq.of(1, 2, 3).singleOrNull(x -> x % 2 == 0));
        assertNull(Linq.of(1, 3).singleOrNull(x -> x % 2 == 0));
        assertThrows(IllegalStateException.class, () -> Linq.of(2, 4).singleOrNull(x -> x % 2 == 0));
    }

    @Test
    void singleOrDefault_allOverloads() {
        assertEquals(1, Linq.of(1).singleOrDefault(99));
        assertEquals(99, Linq.<Integer>of().singleOrDefault(99));
        assertThrows(IllegalStateException.class, () -> Linq.of(1, 2).singleOrDefault(99));

        assertEquals(2, Linq.of(1, 2, 3).singleOrDefault(x -> x % 2 == 0, 99));
        assertEquals(99, Linq.of(1, 3).singleOrDefault(x -> x % 2 == 0, 99));
        assertThrows(
            IllegalStateException.class,
            () -> Linq.of(2, 4).singleOrDefault(x -> x % 2 == 0, 99)
        );
    }

    // -------------------------------------------------------------------------
    // Grouping
    // -------------------------------------------------------------------------

    @Test
    void groupBy_allOverloads() {
        assertEquals(
            List.of("IT:[Alice, Carol]", "HR:[Bob]"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "HR", 21),
                    new Person("Carol", "IT", 22)
                )
                .groupBy(Person::department)
                .select(g -> g.getGroupKey() + ":" +
                    g.getGroupElements().stream().map(Person::name).toList())
                .toList()
        );

        assertEquals(
            2,
            Linq.of("A", "a", "B")
                .groupBy(s -> s, CASE_INSENSITIVE)
                .count()
        );

        assertEquals(
            List.of("IT:[Alice, Carol]", "HR:[Bob]"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "HR", 21),
                    new Person("Carol", "IT", 22)
                )
                .groupBy(Person::department, Person::name)
                .select(g -> g.getGroupKey() + ":" + g.getGroupElements().toString())
                .toList()
        );

        assertEquals(
            List.of("IT:[Alice, Bob]", "HR:[Carol]"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "it", 21),
                    new Person("Carol", "HR", 22)
                )
                .groupBy(Person::department, Person::name, CASE_INSENSITIVE)
                .select(g -> g.getGroupKey() + ":" + g.getGroupElements().toString())
                .toList()
        );
    }

    @Test
    void groupByInHash_allOverloads() {
        assertEquals(
            2,
            Linq.of("A", "a", "B")
                .groupByInHash(s -> s, StringEqualators.ORDINAL_IGNORE_CASE)
                .count()
        );

        assertEquals(
            List.of("IT:[Alice, Bob]", "HR:[Carol]"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "it", 21),
                    new Person("Carol", "HR", 22)
                )
                .groupByInHash(Person::department, Person::name, StringEqualators.ORDINAL_IGNORE_CASE)
                .select(g -> g.getGroupKey() + ":" + g.getGroupElements().toString())
                .toList()
        );
    }

    @Test
    void groupToResult_allOverloads() {
        assertEquals(
            List.of("IT=2", "HR=1"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "HR", 21),
                    new Person("Carol", "IT", 22)
                )
                .groupToResult(Person::department, (key, group) -> key + "=" + group.count())
                .toList()
        );

        assertEquals(
            List.of("IT=2", "HR=1"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "it", 21),
                    new Person("Carol", "HR", 22)
                )
                .groupToResult(
                    Person::department,
                    (key, group) -> key + "=" + group.count(),
                    CASE_INSENSITIVE
                )
                .toList()
        );

        assertEquals(
            List.of("IT=[Alice, Carol]", "HR=[Bob]"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "HR", 21),
                    new Person("Carol", "IT", 22)
                )
                .groupToResult(
                    Person::department,
                    Person::name,
                    (key, group) -> key + "=" + group.toList()
                )
                .toList()
        );

        assertEquals(
            List.of("IT=[Alice, Bob]", "HR=[Carol]"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "it", 21),
                    new Person("Carol", "HR", 22)
                )
                .groupToResult(
                    Person::department,
                    Person::name,
                    (key, group) -> key + "=" + group.toList(),
                    CASE_INSENSITIVE
                )
                .toList()
        );
    }

    @Test
    void groupToResultInHash_allOverloads() {
        assertEquals(
            List.of("IT=2", "HR=1"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "it", 21),
                    new Person("Carol", "HR", 22)
                )
                .groupToResultInHash(
                    Person::department,
                    (key, group) -> key + "=" + group.count(),
                    StringEqualators.ORDINAL_IGNORE_CASE
                )
                .toList()
        );

        assertEquals(
            List.of("IT=[Alice, Bob]", "HR=[Carol]"),
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "it", 21),
                    new Person("Carol", "HR", 22)
                )
                .groupToResultInHash(
                    Person::department,
                    Person::name,
                    (key, group) -> key + "=" + group.toList(),
                    StringEqualators.ORDINAL_IGNORE_CASE
                )
                .toList()
        );
    }

    // -------------------------------------------------------------------------
    // Join operators
    // -------------------------------------------------------------------------

    @Test
    void groupJoin_allVariants() {
        List<LeftRow> left = List.of(
            new LeftRow(1, "A"),
            new LeftRow(2, "B"),
            new LeftRow(3, "C")
        );
        List<RightRow> right = List.of(
            new RightRow(1, "x"),
            new RightRow(1, "y"),
            new RightRow(3, "z")
        );

        assertEquals(
            List.of("A:[x, y]", "B:[]", "C:[z]"),
            Linq.of(left)
                .groupJoin(
                    Linq.of(right),
                    LeftRow::id,
                    RightRow::id,
                    (l, rs) -> l.value() + ":" + rs.select(RightRow::value).toList()
                )
                .toList()
        );

        assertEquals(
            List.of("A:[x, y]", "B:[]", "C:[z]"),
            Linq.of(left)
                .groupJoin(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    (l, rs) -> l.value() + ":" + rs.select(RightRow::value).toList(),
                    CASE_INSENSITIVE
                )
                .toList()
        );

        assertEquals(
            List.of("A:[x, y]", "B:[]", "C:[z]"),
            Linq.of(left)
                .groupJoinInHash(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    (l, rs) -> l.value() + ":" + rs.select(RightRow::value).toList(),
                    HashEqualator.defaultHashEqualator()
                )
                .toList()
        );
    }

    @Test
    void join_allVariants() {
        List<LeftRow> left = List.of(new LeftRow(1, "A"), new LeftRow(2, "B"));
        List<RightRow> right = List.of(new RightRow(1, "x"), new RightRow(1, "y"));

        assertEquals(
            List.of("A-x", "A-y"),
            Linq.of(left)
                .join(
                    Linq.of(right),
                    LeftRow::id,
                    RightRow::id,
                    (l, r) -> l.value() + "-" + r.value()
                )
                .toList()
        );

        assertEquals(
            List.of("A-x", "A-y"),
            Linq.of(left)
                .join(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    (l, r) -> l.value() + "-" + r.value(),
                    CASE_INSENSITIVE
                )
                .toList()
        );

        assertEquals(
            List.of("A-x", "A-y"),
            Linq.of(left)
                .joinInhash(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    (l, r) -> l.value() + "-" + r.value(),
                    HashEqualator.defaultHashEqualator()
                )
                .toList()
        );
    }

    @Test
    void leftJoin_allOverloadsAndHashVariants() {
        List<LeftRow> left = List.of(new LeftRow(1, "A"), new LeftRow(2, "B"));
        List<RightRow> right = List.of(new RightRow(1, "x"));

        assertEquals(
            List.of("A-x", "B-null"),
            Linq.of(left)
                .leftJoin(
                    Linq.of(right),
                    LeftRow::id,
                    RightRow::id,
                    (l, r) -> l.value() + "-" + (r == null ? "null" : r.value())
                )
                .toList()
        );

        assertEquals(
            List.of("A-x", "B-null"),
            Linq.of(left)
                .leftJoinOnEqualator(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    (l, r) -> l.value() + "-" + (r == null ? "null" : r.value()),
                    CASE_INSENSITIVE
                )
                .toList()
        );

        assertEquals(
            List.of("A=x", "B=null"),
            Linq.of(left)
                .leftJoin(
                    Linq.of(right),
                    LeftRow::id,
                    RightRow::id
                )
                .select(p ->
                    p.getLeft().value() + "=" +
                        (p.getRight() == null ? "null" : p.getRight().value())
                )
                .toList()
        );

        assertEquals(
            2L,
            Linq.of(left)
                .leftJoinOnEqualator(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    CASE_INSENSITIVE
                )
                .count()
        );

        assertEquals(
            List.of("A-x", "B-null"),
            Linq.of(left)
                .leftJoinInHash(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    (l, r) -> l.value() + "-" + (r == null ? "null" : r.value()),
                    HashEqualator.defaultHashEqualator()
                )
                .toList()
        );

        assertEquals(
            2L,
            Linq.of(left)
                .leftJoinInHash(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    HashEqualator.defaultHashEqualator()
                )
                .count()
        );
    }

    @Test
    void rightJoin_allVariants() {
        List<LeftRow> left = List.of(new LeftRow(1, "A"));
        List<RightRow> right = List.of(new RightRow(1, "x"), new RightRow(2, "y"));

        assertEquals(
            List.of("A-x", "null-y"),
            Linq.of(left)
                .rightJoin(
                    Linq.of(right),
                    LeftRow::id,
                    RightRow::id,
                    (l, r) -> (l == null ? "null" : l.value()) + "-" + r.value()
                )
                .toList()
        );

        assertEquals(
            List.of("A-x", "null-y"),
            Linq.of(left)
                .rightJoin(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    (l, r) -> (l == null ? "null" : l.value()) + "-" + r.value(),
                    CASE_INSENSITIVE
                )
                .toList()
        );

        assertEquals(
            List.of("A-x", "null-y"),
            Linq.of(left)
                .rightJoinInHash(
                    Linq.of(right),
                    l -> String.valueOf(l.id()),
                    r -> String.valueOf(r.id()),
                    (l, r) -> (l == null ? "null" : l.value()) + "-" + r.value(),
                    HashEqualator.defaultHashEqualator()
                )
                .toList()
        );
    }

    // -------------------------------------------------------------------------
    // max / min / sum
    // -------------------------------------------------------------------------

    @Test
    void max_allVariants() {
        assertEquals(5, Linq.of(1, 5, 3).max());
        assertEquals(5, Linq.of(1, 5, 3).max(Integer::compareTo));
        assertEquals(5, Linq.of(1, 5, 3).maxToInt(Integer::intValue));
        assertEquals(5L, Linq.of(1L, 5L, 3L).maxToLong(Long::longValue));
        assertEquals(5.5, Linq.of(1.0, 5.5, 3.0).maxToDouble(Double::doubleValue), 1e-12);
        assertEquals(
            new BigDecimal("5"),
            Linq.of(new BigDecimal("1"), new BigDecimal("5"), new BigDecimal("3"))
                .maxToDecimal(x -> x)
        );

        assertEquals(
            "banana",
            Linq.of("a", "banana", "cat").maxBy(String::length)
        );

        assertEquals(
            "a",
            Linq.of("a", "banana", "cat")
                .maxBy(String::length, Comparator.reverseOrder())
        );
    }

    @Test
    void min_allVariants() {
        assertEquals(1, Linq.of(1, 5, 3).min());
        assertEquals(1, Linq.of(1, 5, 3).min(Integer::compareTo));
        assertEquals(1, Linq.of(1, 5, 3).minToInt(Integer::intValue));
        assertEquals(1L, Linq.of(1L, 5L, 3L).minToLong(Long::longValue));
        assertEquals(1.0, Linq.of(1.0, 5.5, 3.0).minToDouble(Double::doubleValue), 1e-12);
        assertEquals(
            new BigDecimal("1"),
            Linq.of(new BigDecimal("1"), new BigDecimal("5"), new BigDecimal("3"))
                .minToDecimal(x -> x)
        );

        assertEquals(
            "a",
            Linq.of("a", "banana", "cat").minBy(String::length)
        );

        assertEquals(
            "banana",
            Linq.of("a", "banana", "cat")
                .minBy(String::length, Comparator.reverseOrder())
        );
    }

    @Test
    void maxMin_emptySequencesThrow() {
        assertThrows(NoSuchElementException.class, () -> Linq.<Integer>of().max());
        assertThrows(NoSuchElementException.class, () -> Linq.<Integer>of().min());
        assertThrows(NoSuchElementException.class, () -> Linq.<Integer>of().max(Integer::compareTo));
        assertThrows(NoSuchElementException.class, () -> Linq.<Integer>of().min(Integer::compareTo));
        assertThrows(NoSuchElementException.class, () -> Linq.<Integer>of().maxToInt(Integer::intValue));
        assertThrows(NoSuchElementException.class, () -> Linq.<Integer>of().minToInt(Integer::intValue));
    }

    @Test
    void sum_allVariants() {
        assertEquals(6L, Linq.of(1, 2, 3).sumToInt(Integer::intValue));
        assertEquals(6L, Linq.of(1L, 2L, 3L).sumToLong(Long::longValue));
        assertEquals(6.5, Linq.of(1.0, 2.0, 3.5).sumToDouble(Double::doubleValue), 1e-12);
        assertEquals(
            new BigDecimal("6"),
            Linq.of(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"))
                .sumToDecimal(x -> x)
        );

        assertEquals(0L, Linq.<Integer>of().sumToInt(Integer::intValue));
        assertEquals(BigDecimal.ZERO, Linq.<BigDecimal>of().sumToDecimal(x -> x));
    }

    // -------------------------------------------------------------------------
    // Ordering
    // -------------------------------------------------------------------------

    @Test
    void order_allOverloads() {
        assertEquals(
            List.of(1, 2, 3),
            Linq.of(3, 1, 2).order().toList()
        );

        assertEquals(
            List.of(3, 2, 1),
            Linq.of(3, 1, 2).order(Comparator.reverseOrder()).toList()
        );
    }

    @Test
    void orderBy_allOverloads() {
        List<Person> people = List.of(
            new Person("Bob", "IT", 30),
            new Person("Alice", "HR", 20),
            new Person("Carol", "Sales", 25)
        );

        assertEquals(
            List.of("Alice", "Carol", "Bob"),
            Linq.of(people)
                .orderBy(Person::age)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Bob", "Carol", "Alice"),
            Linq.of(people)
                .orderBy(Person::age, Comparator.reverseOrder())
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Bob", "Carol", "Alice"),
            Linq.of(people)
                .orderByDescending(Person::age)
                .select(Person::name)
                .toList()
        );

        assertEquals(
            List.of("Alice", "Carol", "Bob"),
            Linq.of(people)
                .orderByDescending(Person::age, Comparator.reverseOrder())
                .select(Person::name)
                .toList()
        );
    }

    @Test
    void primitiveOrderByVariants() {
        List<Person> people = List.of(
            new Person("Bob", "IT", 30),
            new Person("Alice", "HR", 20),
            new Person("Carol", "Sales", 25)
        );

        assertEquals(
            List.of("Alice", "Carol", "Bob"),
            Linq.of(people).orderByInt(Person::age).select(Person::name).toList()
        );

        assertEquals(
            List.of("Bob", "Carol", "Alice"),
            Linq.of(people).orderByIntDescending(Person::age).select(Person::name).toList()
        );

        assertEquals(
            List.of(1L, 2L, 3L),
            Linq.of(3L, 1L, 2L).orderByLong(Long::longValue).toList()
        );

        assertEquals(
            List.of(3L, 2L, 1L),
            Linq.of(3L, 1L, 2L).orderByLongDescending(Long::longValue).toList()
        );

        assertEquals(
            List.of(1.0, 2.0, 3.0),
            Linq.of(3.0, 1.0, 2.0).orderByDouble(Double::doubleValue).toList()
        );

        assertEquals(
            List.of(3.0, 2.0, 1.0),
            Linq.of(3.0, 1.0, 2.0).orderByDoubleDescending(Double::doubleValue).toList()
        );
    }

    // -------------------------------------------------------------------------
    // Projection / filtering / zip
    // -------------------------------------------------------------------------

    @Test
    void select_allOverloads() {
        assertEquals(
            List.of(2, 4, 6),
            Linq.of(1, 2, 3).select(x -> x * 2).toList()
        );

        assertEquals(
            List.of("0:a", "1:b", "2:c"),
            Linq.of("a", "b", "c")
                .select((value, index) -> index + ":" + value)
                .toList()
        );
    }

    @Test
    void selectMany_allOverloads() {
        assertEquals(
            List.of(1, 10, 2, 20),
            Linq.of(1, 2)
                .selectMany(x -> Linq.of(x, x * 10))
                .toList()
        );

        assertEquals(
            List.of("0:a", "0:A", "1:b", "1:B"),
            Linq.of("a", "b")
                .selectMany((s, i) -> Linq.of(i + ":" + s, i + ":" + s.toUpperCase()))
                .toList()
        );

        assertEquals(
            List.of("a1", "a2", "b1", "b2"),
            Linq.of("a", "b")
                .selectMany(
                    s -> List.of(1, 2),
                    (s, n) -> s + n
                )
                .toList()
        );

        assertEquals(
            List.of("0:a1", "0:a2", "1:b1", "1:b2"),
            Linq.of("a", "b")
                .selectMany(
                    (s, i) -> List.of(1, 2),
                    (s, n) -> (s.equals("a") ? "0:" : "1:") + s + n
                )
                .toList()
        );
    }

    @Test
    void where_allOverloads() {
        assertEquals(
            List.of(2, 4),
            Linq.of(1, 2, 3, 4).where(x -> x % 2 == 0).toList()
        );

        assertEquals(
            List.of("a", "c"),
            Linq.of("a", "b", "c", "d")
                .where((value, index) -> index % 2 == 0)
                .toList()
        );
    }

    @Test
    void zip_allOverloads() {
        assertEquals(
            List.of("1=a", "2=b"),
            pairStrings(Linq.of(1, 2, 3).zip(Linq.of("a", "b")))
        );

        assertEquals(
            List.of("1a", "2b"),
            Linq.of(1, 2, 3)
                .zip(Linq.of("a", "b"), (n, s) -> n + s)
                .toList()
        );
    }

    @Test
    void sequenceEqual_allOverloads() {
        assertTrue(Linq.of(1, 2, 3).sequenceEqual(Linq.of(1, 2, 3)));
        assertFalse(Linq.of(1, 2, 3).sequenceEqual(Linq.of(1, 3, 2)));

        assertTrue(
            Linq.of("A", "b")
                .sequenceEqual(Linq.of("a", "B"), CASE_INSENSITIVE)
        );
        assertFalse(
            Linq.of("A", "b")
                .sequenceEqual(Linq.of("a"), CASE_INSENSITIVE)
        );
    }

    // -------------------------------------------------------------------------
    // Partition operators
    // -------------------------------------------------------------------------

    @Test
    void skip_skipLast_take_takeLast() {
        assertEquals(List.of(3, 4), Linq.of(1, 2, 3, 4).skip(2).toList());
        assertEquals(List.of(1, 2), Linq.of(1, 2, 3, 4).skipLast(2).toList());

        assertEquals(List.of(1, 2), Linq.of(1, 2, 3, 4).take(2).toList());
        assertEquals(List.of(3, 4), Linq.of(1, 2, 3, 4).takeLast(2).toList());

    }

    @Test
    void skipWhile_allOverloads() {
        assertEquals(
            List.of(3, 2),
            Linq.of(1, 2, 3, 2).skipWhile(x -> x < 3).toList()
        );

        assertEquals(
            List.of("c", "d"),
            Linq.of("a", "b", "c", "d")
                .skipWhile((value, index) -> index < 2)
                .toList()
        );
    }

    @Test
    void takeWhile_allOverloads() {
        assertEquals(
            List.of(1, 2),
            Linq.of(1, 2, 3, 2).takeWhile(x -> x < 3).toList()
        );

        assertEquals(
            List.of("a", "b"),
            Linq.of("a", "b", "c", "d")
                .takeWhile((value, index) -> index < 2)
                .toList()
        );
    }

    @Test
    void shuffle_preservesElements() {
        List<Integer> result = Linq.of(1, 2, 3, 4, 5).shuffle().toList();
        assertEquals(5, result.size());

        List<Integer> sorted = new ArrayList<>(result);
        Collections.sort(sorted);
        assertEquals(List.of(1, 2, 3, 4, 5), sorted);
    }

    // -------------------------------------------------------------------------
    // Array / map / collection materialization
    // -------------------------------------------------------------------------

    @Test
    void toArray_allOverloads() {
        assertArrayEquals(
            new Integer[]{1, 2, 3},
            Linq.of(1, 2, 3).toArray(Integer[]::new)
        );

        assertArrayEquals(
            new Object[]{1, 2, 3},
            Linq.of(1, 2, 3).toArray()
        );
    }

    @Test
    void toMap_allOverloads() {
        Map<Integer, String> byLength = Linq.of("a", "bb", "ccc")
            .toMap(String::length);
        assertEquals("bb", byLength.get(2));

        Map<Integer, String> values = Linq.of("a", "bb", "ccc")
            .toMap(String::length, String::toUpperCase);
        assertEquals("BB", values.get(2));

        Map<String, String> caseInsensitive = Linq.of("A", "B")
            .toMapOnEqualator(s -> s, CASE_INSENSITIVE);
        assertEquals("A", caseInsensitive.get("a"));

        Map<String, Integer> caseInsensitiveValues = Linq.of("A", "BB")
            .toMapOnEqualator(s -> s, String::length, CASE_INSENSITIVE);
        assertEquals(2, caseInsensitiveValues.get("bb"));
    }

    @Test
    void toMapInHash_allOverloads() {
        Map<String, String> map = Linq.of("A", "B")
            .toMapInHash(s -> s, StringEqualators.ORDINAL_IGNORE_CASE);
        assertEquals("A", map.get("a"));

        Map<String, Integer> values = Linq.of("A", "BB")
            .toMapInHash(s -> s, String::length, StringEqualators.ORDINAL_IGNORE_CASE);
        assertEquals(2, values.get("bb"));
    }

    @Test
    void toUnmodifiableMap_allOverloads() {
        Map<Integer, String> m1 = Linq.of("a", "bb").toUnmodifiableMap(String::length);
        assertThrows(UnsupportedOperationException.class, () -> m1.put(3, "ccc"));

        Map<Integer, String> m2 = Linq.of("a", "bb")
            .toUnmodifiableMap(String::length, String::toUpperCase);
        assertEquals("BB", m2.get(2));
        assertThrows(UnsupportedOperationException.class, () -> m2.put(3, "CCC"));

        Map<String, String> m3 = Linq.of("A", "B")
            .toUnmodifiableMapOnEqualator(s -> s, CASE_INSENSITIVE);
        assertEquals("A", m3.get("a"));
        assertThrows(UnsupportedOperationException.class, () -> m3.put("C", "C"));

        Map<String, Integer> m4 = Linq.of("A", "BB")
            .toUnmodifiableMapOnEqualator(s -> s, String::length, CASE_INSENSITIVE);
        assertEquals(2, m4.get("bb"));
        assertThrows(UnsupportedOperationException.class, () -> m4.put("C", 1));
    }

    @Test
    void toUnmodifiableSortedMap_allOverloads() {
        SortedMap<Integer, String> natural = Linq.of("ccc", "a", "bb")
            .toUnmodifiableSortedMap(String::length);
        assertEquals(List.of(1, 2, 3), new ArrayList<>(natural.keySet()));
        assertThrows(UnsupportedOperationException.class, () -> natural.put(4, "dddd"));

        SortedMap<Integer, String> naturalValues = Linq.of("ccc", "a", "bb")
            .toUnmodifiableSortedMap(String::length, String::toUpperCase);
        assertEquals(List.of(1, 2, 3), new ArrayList<>(naturalValues.keySet()));
        assertEquals("BB", naturalValues.get(2));

        SortedMap<Integer, String> m1 = Linq.of("ccc", "a", "bb")
            .toUnmodifiableSortedMapOnComparator(String::length, Comparator.reverseOrder());
        assertEquals(List.of(3, 2, 1), new ArrayList<>(m1.keySet()));

        SortedMap<Integer, String> m2 = Linq.of("ccc", "a", "bb")
            .toUnmodifiableSortedMapOnComparator(
                String::length,
                String::toUpperCase,
                Comparator.reverseOrder()
            );
        assertEquals(List.of(3, 2, 1), new ArrayList<>(m2.keySet()));
        assertEquals("CCC", m2.get(3));
    }

    @Test
    void listMaterializers() {
        assertEquals(
            List.of(3, 1, 2),
            Linq.of(3, 1, 2).toList()
        );

        assertEquals(
            List.of(1, 2, 3),
            Linq.of(3, 1, 2).toSortedList()
        );

        assertEquals(
            List.of(3, 2, 1),
            Linq.of(3, 1, 2).toSortedList(Comparator.reverseOrder())
        );

        List<Integer> unmodifiable = Linq.of(3, 1, 2).toUnmodifiableList();
        assertEquals(List.of(3, 1, 2), unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add(4));

        List<Integer> naturalSorted = Linq.of(3, 1, 2).toUnmodifiableSortedList();
        assertEquals(List.of(1, 2, 3), naturalSorted);
        assertThrows(UnsupportedOperationException.class, () -> naturalSorted.add(4));

        List<Integer> reverseSorted = Linq.of(3, 1, 2)
            .toUnmodifiableSortedList(Comparator.reverseOrder());
        assertEquals(List.of(3, 2, 1), reverseSorted);
        assertThrows(UnsupportedOperationException.class, () -> reverseSorted.add(4));
    }

    @Test
    void setMaterializers() {
        assertEquals(
            Set.of(1, 2, 3),
            Linq.of(1, 1, 2, 3).toHashSet()
        );

        Set<Integer> unmodifiableHashSet = Linq.of(1, 1, 2, 3)
            .toUnmodifiableHashSet();
        assertEquals(Set.of(1, 2, 3), unmodifiableHashSet);
        assertThrows(
            UnsupportedOperationException.class,
            () -> unmodifiableHashSet.add(4)
        );

        SortedSet<Integer> naturalSorted = Linq.of(3, 1, 2).toSortedSet();
        assertEquals(List.of(1, 2, 3), new ArrayList<>(naturalSorted));

        SortedSet<Integer> sorted = Linq.of(3, 1, 2)
            .toSortedSet(Comparator.reverseOrder());
        assertEquals(List.of(3, 2, 1), new ArrayList<>(sorted));

        SortedSet<Integer> naturalUnmodifiable = Linq.of(3, 1, 2)
            .toUnmodifiableSortedSet();
        assertEquals(List.of(1, 2, 3), new ArrayList<>(naturalUnmodifiable));
        assertThrows(
            UnsupportedOperationException.class,
            () -> naturalUnmodifiable.add(4)
        );

        SortedSet<Integer> reverseUnmodifiable = Linq.of(3, 1, 2)
            .toUnmodifiableSortedSet(Comparator.reverseOrder());
        assertEquals(List.of(3, 2, 1), new ArrayList<>(reverseUnmodifiable));
        assertThrows(
            UnsupportedOperationException.class,
            () -> reverseUnmodifiable.add(4)
        );
    }
}
