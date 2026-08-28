package io.github.piscescup.linq4j;

import io.github.piscescup.equalators.StringEqualators;
import io.github.piscescup.interfaces.Equalator;
import io.github.piscescup.interfaces.HashEqualator;
import io.github.piscescup.interfaces.Pair;
import io.github.piscescup.linq4j.core.Enumerable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
    private record NumericValue(
        String name,
        int intValue,
        long longValue,
        double doubleValue,
        BigDecimal decimalValue
    ) {}


    private static <T> List<T> list(@NotNull Enumerable<T> enumerable) {
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
            6,
            Linq.of("a", "bb", "ccc")
                .aggregateToResult(0, (sum, value) -> sum + value.length())
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
        assertTrue(Linq.<String>of().all(String::isEmpty));
        assertTrue(Linq.of("aa", "bbbb", "cccccc").all(s -> s.length() % 2 == 0));
        assertFalse(Linq.of("aa", "bbb", "cccccc").all(s -> s.length() % 2 == 0));

        assertFalse(Linq.of().any());
        assertTrue(Linq.of("a").any());
        assertTrue(Linq.of("a", "bb", "ccc").any(s -> s.length() == 2));
        assertFalse(Linq.of("a", "bb", "ccc").any(s -> s.length() > 10));

        AtomicInteger calls = new AtomicInteger();
        assertTrue(
            Linq.of("a", "bb", "ccc", "dddd").any(s -> {
                calls.incrementAndGet();
                return s.length() == 2;
            })
        );
        assertEquals(2, calls.get());
    }


    @Test
    void append_prepend_concat_reverse_defaultIfEmpty() {
        assertEquals(List.of("a", "b", "c", "d"), list(Linq.of("a", "b", "c").append("d")));
        assertEquals(Arrays.asList(null, "a", "b", "c"), list(Linq.of("a", "b", "c").prepend(null)));
        assertEquals(List.of("a", "b", "c", "d"), list(Linq.of("a", "b").concat(Linq.of("c", "d"))));
        assertEquals(List.of("c", "b", "a"), list(Linq.of("a", "b", "c").reverse()));
        assertEquals(List.of("a", "b"), list(Linq.of("a", "b").defaultIfEmpty("default")));
        assertEquals(List.of("default"), list(Linq.<String>of().defaultIfEmpty("default")));
    }


    @Test
    void averageToDouble_and_averageToDecimal() {
        assertEquals(
            25.0,
            Linq.of(
                    new Person("Alice", "IT", 20),
                    new Person("Bob", "HR", 30)
                )
                .averageToDouble(Person::age),
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

        assertThrows(ArithmeticException.class, () -> Linq.<Person>of().averageToDouble(Person::age));
        assertThrows(
            ArithmeticException.class,
            () -> Linq.<BigDecimal>of().averageToDecimal(x -> x, RoundingMode.HALF_UP)
        );
        assertThrows(
            NullPointerException.class,
            () -> Linq.of(new Person("Alice", "IT", 20)).averageToDouble(null)
        );
        assertThrows(
            NullPointerException.class,
            () -> Linq.of(BigDecimal.ONE).averageToDecimal(x -> x, null)
        );
    }


    @Test
    void cast() {
        assertEquals(List.of("a", "b"), list(Linq.<Object>of("a", "b").cast(String.class)));
        assertThrows(
            ClassCastException.class,
            () -> Linq.<Object>of("a", new Object()).cast(String.class).toList()
        );
        assertThrows(NullPointerException.class, () -> Linq.of("a").cast(null));
    }


    @Test
    void chunk() {
        List<List<String>> actual = Linq.of("a", "b", "c", "d", "e", "f", "g")
            .chunk(3)
            .select(Enumerable::toList)
            .toList();

        assertEquals(
            List.of(
                List.of("a", "b", "c"),
                List.of("d", "e", "f"),
                List.of("g")
            ),
            actual
        );

        assertThrows(IllegalArgumentException.class, () -> Linq.of("a", "b", "c").chunk(0));
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
        assertEquals(4L, Linq.of("a", "bb", "ccc", "dddd").count());
        assertEquals(2L, Linq.of("a", "bb", "ccc", "dddd").count(s -> s.length() % 2 == 0));
        assertThrows(NullPointerException.class, () -> Linq.of("a", "b", "c").count(null));
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
        assertEquals(List.of("a", "b", "c"), list(Linq.of("a", "a", "b", "c", "b").distinct()));
        assertEquals(List.of("A", "B"), list(Linq.of("A", "a", "B", "b").distinct(CASE_INSENSITIVE)));
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
            List.of("a", "c"),
            list(Linq.of("a", "b", "b", "c", "d").except(Linq.of("b", "d")))
        );
        assertEquals(
            List.of("B"),
            list(Linq.of("A", "a", "B").except(Linq.of("a"), CASE_INSENSITIVE))
        );
        assertEquals(
            List.of("B"),
            list(Linq.of("A", "a", "B").exceptInHash(Linq.of("a"), StringEqualators.ORDINAL_IGNORE_CASE))
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
            List.of("b", "c"),
            list(Linq.of("a", "b", "b", "c").intersect(Linq.of("b", "c", "d")))
        );
        assertEquals(
            List.of("A", "B"),
            list(Linq.of("A", "a", "B", "C").intersect(Linq.of("a", "b"), CASE_INSENSITIVE))
        );
        assertEquals(
            List.of("A", "B"),
            list(Linq.of("A", "a", "B", "C").intersectInHash(Linq.of("a", "b"), StringEqualators.ORDINAL_IGNORE_CASE))
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
    public void testSelectToPrimitive() {
        record Score(double math, double chinese, double english) {
            public double totalScore() {
                return math + chinese + english;
            }

            public double avgScore() {
                return totalScore() / 3;
            }
        }

        record Student(String name, int id, Score score) {

        }

        List<Person> people = List.of(
            new Person("Alice", "IT", 20),
            new Person("Bob", "HR", 21),
            new Person("Carol", "Sales", 22)
        );

        assertArrayEquals(
            new int[] {20, 21, 22},
            Linq.of(people)
                .selectToInt(Person::age)
                .toArray()
        );

        List<Student> students = List.of(
            new Student("Alice", 1001, new Score(98, 90, 93)),
            new Student("Bob", 1002, new Score(97, 91, 96)),
            new Student("Carol", 1003, new Score(99, 83, 87))
        );

        assertArrayEquals(
            new double[] {
                89.666666666666666666666666666667,
                93.666666666666666666666666666667,
                94.666666666666666666666666666667
            },
            Linq.of(students)
                .selectToDouble(s -> s.score().avgScore())
                .order()
                .toArray()
        );
    }

    @Test
    void union_allVariants() {
        assertEquals(
            List.of("a", "b", "c", "d"),
            list(Linq.of("a", "b", "b").union(Linq.of("b", "c", "d")))
        );
        assertEquals(
            List.of("A", "B", "C"),
            list(Linq.of("A", "B").union(Linq.of("a", "C"), CASE_INSENSITIVE))
        );
        assertEquals(
            List.of("A", "B", "C"),
            list(Linq.of("A", "B").unionInHash(Linq.of("a", "C"), StringEqualators.ORDINAL_IGNORE_CASE))
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
        assertEquals("b", Linq.of("a", "b", "c").elementAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.of("a", "b").elementAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.of("a", "b").elementAt(2));

        assertEquals("b", Linq.of("a", "b", "c").elementAtOrNull(1));
        assertNull(Linq.of("a", "b", "c").elementAtOrNull(-1));
        assertNull(Linq.of("a", "b", "c").elementAtOrNull(99));

        assertEquals("b", Linq.of("a", "b", "c").elementAtOrDefault(1, "default"));
        assertEquals("default", Linq.of("a", "b", "c").elementAtOrDefault(-1, "default"));
        assertEquals("default", Linq.of("a", "b", "c").elementAtOrDefault(99, "default"));
    }


    @Test
    void first_allOverloads() {
        assertEquals("a", Linq.of("a", "bb", "ccc").first());
        assertEquals("bb", Linq.of("a", "bb", "ccc", "dddd").first(s -> s.length() % 2 == 0));
        assertThrows(NoSuchElementException.class, () -> Linq.<String>of().first());
        assertThrows(NoSuchElementException.class, () -> Linq.of("a", "ccc").first(s -> s.length() % 2 == 0));
    }


    @Test
    void firstOrNull_allOverloads() {
        assertEquals("a", Linq.of("a", "bb").firstOrNull());
        assertNull(Linq.<String>of().firstOrNull());
        assertEquals("bb", Linq.of("a", "bb", "dddd").firstOrNull(s -> s.length() == 2));
        assertNull(Linq.of("a", "ccc").firstOrNull(s -> s.length() == 2));
    }


    @Test
    void firstOrDefault_allOverloads() {
        assertEquals("a", Linq.of("a", "bb").firstOrDefault("default"));
        assertEquals("default", Linq.<String>of().firstOrDefault("default"));
        assertEquals("bb", Linq.of("a", "bb", "dddd").firstOrDefault(s -> s.length() == 2, "default"));
        assertEquals("default", Linq.of("a", "ccc").firstOrDefault(s -> s.length() == 2, "default"));
    }


    @Test
    void last_allOverloads() {
        assertEquals("ccc", Linq.of("a", "bb", "ccc").last());
        assertEquals("dddd", Linq.of("a", "bb", "ccc", "dddd").last(s -> s.length() % 2 == 0));
        assertThrows(NoSuchElementException.class, () -> Linq.<String>of().last());
        assertThrows(NoSuchElementException.class, () -> Linq.of("a", "ccc").last(s -> s.length() % 2 == 0));
    }


    @Test
    void lastOrNull_allOverloads() {
        assertEquals("ccc", Linq.of("a", "bb", "ccc").lastOrNull());
        assertNull(Linq.<String>of().lastOrNull());
        assertEquals("dddd", Linq.of("a", "bb", "ccc", "dddd").lastOrNull(s -> s.length() % 2 == 0));
        assertNull(Linq.of("a", "ccc").lastOrNull(s -> s.length() % 2 == 0));
    }


    @Test
    void lastOrDefault_allOverloads() {
        assertEquals("ccc", Linq.of("a", "bb", "ccc").lastOrDefault("default"));
        assertEquals("default", Linq.<String>of().lastOrDefault("default"));
        assertEquals(
            "dddd",
            Linq.of("a", "bb", "ccc", "dddd").lastOrDefault(s -> s.length() % 2 == 0, "default")
        );
        assertEquals(
            "default",
            Linq.of("a", "ccc").lastOrDefault(s -> s.length() % 2 == 0, "default")
        );
    }


    @Test
    void single_allOverloads_andFailureCases() {
        assertEquals("a", Linq.of("a").single());
        assertThrows(NoSuchElementException.class, () -> Linq.<String>of().single());
        assertThrows(IllegalStateException.class, () -> Linq.of("a", "b").single());

        assertEquals("bb", Linq.of("a", "bb", "ccc").single(s -> s.length() == 2));
        assertThrows(NoSuchElementException.class, () -> Linq.of("a", "ccc").single(s -> s.length() == 2));
        assertThrows(IllegalStateException.class, () -> Linq.of("aa", "bb").single(s -> s.length() == 2));
    }


    @Test
    void singleOrNull_allOverloads() {
        assertEquals("a", Linq.of("a").singleOrNull());
        assertNull(Linq.<String>of().singleOrNull());
        assertThrows(IllegalStateException.class, () -> Linq.of("a", "b").singleOrNull());

        assertEquals("bb", Linq.of("a", "bb", "ccc").singleOrNull(s -> s.length() == 2));
        assertNull(Linq.of("a", "ccc").singleOrNull(s -> s.length() == 2));
        assertThrows(IllegalStateException.class, () -> Linq.of("aa", "bb").singleOrNull(s -> s.length() == 2));
    }


    @Test
    void singleOrDefault_allOverloads() {
        assertEquals("a", Linq.of("a").singleOrDefault("default"));
        assertEquals("default", Linq.<String>of().singleOrDefault("default"));
        assertThrows(IllegalStateException.class, () -> Linq.of("a", "b").singleOrDefault("default"));

        assertEquals("bb", Linq.of("a", "bb", "ccc").singleOrDefault(s -> s.length() == 2, "default"));
        assertEquals("default", Linq.of("a", "ccc").singleOrDefault(s -> s.length() == 2, "default"));
        assertThrows(
            IllegalStateException.class,
            () -> Linq.of("aa", "bb").singleOrDefault(s -> s.length() == 2, "default")
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
        assertEquals("ccc", Linq.of("a", "bb", "ccc").max());
        assertEquals("a", Linq.of("a", "bb", "ccc").max(Comparator.reverseOrder()));

        List<NumericValue> values = List.of(
            new NumericValue("one", 1, 10L, 1.5, new BigDecimal("1")),
            new NumericValue("five", 5, 50L, 5.5, new BigDecimal("5")),
            new NumericValue("three", 3, 30L, 3.5, new BigDecimal("3"))
        );

        assertEquals(5, Linq.of(values).maxToInt(NumericValue::intValue));
        assertEquals(50L, Linq.of(values).maxToLong(NumericValue::longValue));
        assertEquals(5.5, Linq.of(values).maxToDouble(NumericValue::doubleValue), 1e-12);
        assertEquals(new BigDecimal("5"), Linq.of(values).maxToDecimal(NumericValue::decimalValue));

        assertEquals("banana", Linq.of("a", "banana", "cat").maxBy(String::length));
        assertEquals(
            "a",
            Linq.of("a", "banana", "cat").maxBy(String::length, Comparator.reverseOrder())
        );
    }


    @Test
    void min_allVariants() {
        assertEquals("a", Linq.of("a", "bb", "ccc").min());
        assertEquals("ccc", Linq.of("a", "bb", "ccc").min(Comparator.reverseOrder()));

        List<NumericValue> values = List.of(
            new NumericValue("one", 1, 10L, 1.5, new BigDecimal("1")),
            new NumericValue("five", 5, 50L, 5.5, new BigDecimal("5")),
            new NumericValue("three", 3, 30L, 3.5, new BigDecimal("3"))
        );

        assertEquals(1, Linq.of(values).minToInt(NumericValue::intValue));
        assertEquals(10L, Linq.of(values).minToLong(NumericValue::longValue));
        assertEquals(1.5, Linq.of(values).minToDouble(NumericValue::doubleValue), 1e-12);
        assertEquals(new BigDecimal("1"), Linq.of(values).minToDecimal(NumericValue::decimalValue));

        assertEquals("a", Linq.of("a", "banana", "cat").minBy(String::length));
        assertEquals(
            "banana",
            Linq.of("a", "banana", "cat").minBy(String::length, Comparator.reverseOrder())
        );
    }


    @Test
    void maxMin_emptySequencesThrow() {
        assertThrows(NoSuchElementException.class, () -> Linq.<String>of().max());
        assertThrows(NoSuchElementException.class, () -> Linq.<String>of().min());
        assertThrows(NoSuchElementException.class, () -> Linq.<String>of().max(String::compareTo));
        assertThrows(NoSuchElementException.class, () -> Linq.<String>of().min(String::compareTo));
        assertThrows(NoSuchElementException.class, () -> Linq.<NumericValue>of().maxToInt(NumericValue::intValue));
        assertThrows(NoSuchElementException.class, () -> Linq.<NumericValue>of().minToInt(NumericValue::intValue));
    }


    @Test
    void sum_allVariants() {
        List<NumericValue> values = List.of(
            new NumericValue("one", 1, 10L, 1.5, new BigDecimal("1")),
            new NumericValue("two", 2, 20L, 2.0, new BigDecimal("2")),
            new NumericValue("three", 3, 30L, 3.5, new BigDecimal("3"))
        );

        assertEquals(6L, Linq.of(values).sumToInt(NumericValue::intValue));
        assertEquals(60L, Linq.of(values).sumToLong(NumericValue::longValue));
        assertEquals(7.0, Linq.of(values).sumToDouble(NumericValue::doubleValue), 1e-12);
        assertEquals(new BigDecimal("6"), Linq.of(values).sumToDecimal(NumericValue::decimalValue));

        assertEquals(0L, Linq.<NumericValue>of().sumToInt(NumericValue::intValue));
        assertEquals(BigDecimal.ZERO, Linq.<NumericValue>of().sumToDecimal(NumericValue::decimalValue));
    }

    // -------------------------------------------------------------------------
    // Ordering
    // -------------------------------------------------------------------------


    @Test
    void order_allOverloads() {
        assertEquals(List.of("a", "b", "c"), Linq.of("c", "a", "b").order().toList());
        assertEquals(
            List.of("c", "b", "a"),
            Linq.of("c", "a", "b").order(Comparator.reverseOrder()).toList()
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
        List<NumericValue> values = List.of(
            new NumericValue("third", 3, 30L, 3.0, BigDecimal.valueOf(3)),
            new NumericValue("first", 1, 10L, 1.0, BigDecimal.ONE),
            new NumericValue("second", 2, 20L, 2.0, BigDecimal.valueOf(2))
        );

        assertEquals(
            List.of("first", "second", "third"),
            Linq.of(values).orderByInt(NumericValue::intValue).select(NumericValue::name).toList()
        );
        assertEquals(
            List.of("third", "second", "first"),
            Linq.of(values).orderByIntDescending(NumericValue::intValue).select(NumericValue::name).toList()
        );
        assertEquals(
            List.of("first", "second", "third"),
            Linq.of(values).orderByLong(NumericValue::longValue).select(NumericValue::name).toList()
        );
        assertEquals(
            List.of("third", "second", "first"),
            Linq.of(values).orderByLongDescending(NumericValue::longValue).select(NumericValue::name).toList()
        );
        assertEquals(
            List.of("first", "second", "third"),
            Linq.of(values).orderByDouble(NumericValue::doubleValue).select(NumericValue::name).toList()
        );
        assertEquals(
            List.of("third", "second", "first"),
            Linq.of(values).orderByDoubleDescending(NumericValue::doubleValue).select(NumericValue::name).toList()
        );
    }

    // -------------------------------------------------------------------------
    // Projection / filtering / zip
    // -------------------------------------------------------------------------


    @Test
    void select_allOverloads() {
        assertEquals(
            List.of("A", "BB", "CCC"),
            Linq.of("a", "bb", "ccc").select(s -> s.toUpperCase()).toList()
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
            List.of("a", "A", "b", "B"),
            Linq.of("a", "b").selectMany(x -> Linq.of(x, x.toUpperCase())).toList()
        );

        assertEquals(
            List.of("0:a", "0:A", "1:b", "1:B"),
            Linq.of("a", "b")
                .selectMany((s, i) -> Linq.of(i + ":" + s, i + ":" + s.toUpperCase()))
                .toList()
        );

        assertEquals(
            List.of("a-x", "a-y", "b-x", "b-y"),
            Linq.of("a", "b")
                .selectMany(_ -> List.of("x", "y"), (s, suffix) -> s + "-" + suffix)
                .toList()
        );

        assertEquals(
            List.of("0:a-x", "0:a-y", "1:b-x", "1:b-y"),
            Linq.of("a", "b")
                .selectMany(
                    (s, i) -> List.of("x", "y"),
                    (s, suffix) -> (s.equals("a") ? "0:" : "1:") + s + "-" + suffix
                )
                .toList()
        );
    }

    @Test
    void where_allOverloads() {
        assertEquals(
            List.of("bb", "dddd"),
            Linq.of("a", "bb", "ccc", "dddd").where(s -> s.length() % 2 == 0).toList()
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
            List.of("a=x", "b=y"),
            pairStrings(Linq.of("a", "b", "c").zip(Linq.of("x", "y")))
        );

        assertEquals(
            List.of("ax", "by"),
            Linq.of("a", "b", "c")
                .zip(Linq.of("x", "y"), (left, right) -> left + right)
                .toList()
        );
    }


    @Test
    void sequenceEqual_allOverloads() {
        assertTrue(Linq.of("a", "b", "c").sequenceEqual(Linq.of("a", "b", "c")));
        assertFalse(Linq.of("a", "b", "c").sequenceEqual(Linq.of("a", "c", "b")));

        assertTrue(Linq.of("A", "b").sequenceEqual(Linq.of("a", "B"), CASE_INSENSITIVE));
        assertFalse(Linq.of("A", "b").sequenceEqual(Linq.of("a"), CASE_INSENSITIVE));
    }

    // -------------------------------------------------------------------------
    // Partition operators
    // -------------------------------------------------------------------------


    @Test
    void skip_skipLast_take_takeLast() {
        assertEquals(List.of("c", "d"), Linq.of("a", "b", "c", "d").skip(2).toList());
        assertEquals(List.of("a", "b"), Linq.of("a", "b", "c", "d").skipLast(2).toList());
        assertEquals(List.of("a", "b"), Linq.of("a", "b", "c", "d").take(2).toList());
        assertEquals(List.of("c", "d"), Linq.of("a", "b", "c", "d").takeLast(2).toList());
    }


    @Test
    void skipWhile_allOverloads() {
        assertEquals(
            List.of("ccc", "bb"),
            Linq.of("a", "bb", "ccc", "bb").skipWhile(s -> s.length() < 3).toList()
        );

        assertEquals(
            List.of("c", "d"),
            Linq.of("a", "b", "c", "d").skipWhile((value, index) -> index < 2).toList()
        );
    }


    @Test
    void takeWhile_allOverloads() {
        assertEquals(
            List.of("a", "bb"),
            Linq.of("a", "bb", "ccc", "bb").takeWhile(s -> s.length() < 3).toList()
        );

        assertEquals(
            List.of("a", "b"),
            Linq.of("a", "b", "c", "d").takeWhile((value, index) -> index < 2).toList()
        );
    }


    @Test
    void shuffle_preservesElements() {
        List<String> result = Linq.of("a", "b", "c", "d", "e").shuffle().toList();
        assertEquals(5, result.size());

        List<String> sorted = new ArrayList<>(result);
        Collections.sort(sorted);
        assertEquals(List.of("a", "b", "c", "d", "e"), sorted);
    }

    // -------------------------------------------------------------------------
    // Array / map / collection materialization
    // -------------------------------------------------------------------------


    @Test
    void toArray_allOverloads() {
        assertArrayEquals(new String[]{"a", "b", "c"}, Linq.of("a", "b", "c").toArray(String[]::new));
        assertArrayEquals(new Object[]{"a", "b", "c"}, Linq.of("a", "b", "c").toArray());
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
        assertEquals(List.of("c", "a", "b"), Linq.of("c", "a", "b").toList());
        assertEquals(List.of("a", "b", "c"), Linq.of("c", "a", "b").toSortedList());
        assertEquals(
            List.of("c", "b", "a"),
            Linq.of("c", "a", "b").toSortedList(Comparator.reverseOrder())
        );

        List<String> unmodifiable = Linq.of("c", "a", "b").toUnmodifiableList();
        assertEquals(List.of("c", "a", "b"), unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add("d"));

        List<String> naturalSorted = Linq.of("c", "a", "b").toUnmodifiableSortedList();
        assertEquals(List.of("a", "b", "c"), naturalSorted);
        assertThrows(UnsupportedOperationException.class, () -> naturalSorted.add("d"));

        List<String> reverseSorted = Linq.of("c", "a", "b")
            .toUnmodifiableSortedList(Comparator.reverseOrder());
        assertEquals(List.of("c", "b", "a"), reverseSorted);
        assertThrows(UnsupportedOperationException.class, () -> reverseSorted.add("d"));
    }


    @Test
    void setMaterializers() {
        assertEquals(Set.of("a", "b", "c"), Linq.of("a", "a", "b", "c").toHashSet());

        Set<String> unmodifiableHashSet = Linq.of("a", "a", "b", "c").toUnmodifiableHashSet();
        assertEquals(Set.of("a", "b", "c"), unmodifiableHashSet);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableHashSet.add("d"));

        SortedSet<String> naturalSorted = Linq.of("c", "a", "b").toSortedSet();
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(naturalSorted));

        SortedSet<String> sorted = Linq.of("c", "a", "b").toSortedSet(Comparator.reverseOrder());
        assertEquals(List.of("c", "b", "a"), new ArrayList<>(sorted));

        SortedSet<String> naturalUnmodifiable = Linq.of("c", "a", "b").toUnmodifiableSortedSet();
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(naturalUnmodifiable));
        assertThrows(UnsupportedOperationException.class, () -> naturalUnmodifiable.add("d"));

        SortedSet<String> reverseUnmodifiable = Linq.of("c", "a", "b")
            .toUnmodifiableSortedSet(Comparator.reverseOrder());
        assertEquals(List.of("c", "b", "a"), new ArrayList<>(reverseUnmodifiable));
        assertThrows(UnsupportedOperationException.class, () -> reverseUnmodifiable.add("d"));
    }
}
