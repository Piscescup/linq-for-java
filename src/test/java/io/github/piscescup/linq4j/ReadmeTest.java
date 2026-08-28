package io.github.piscescup.linq4j;

import io.github.piscescup.linq4j.base.Groupable;
import io.github.piscescup.linq4j.core.IntEnumerable;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 *
 * @author REN YuanTong
 * @since
 */
public class ReadmeTest {
    record Person(String name, int age) {
        @Override
        public @NonNull String toString() {
            return name + "-" + age;
        }
    }

    @Test
    public void testFiltering() {
        var evens = Linq.ofInts(1, 2, 3, 4, 5)
            .where(value -> value % 2 == 0)
            .toArray();

        System.out.println(Arrays.toString(evens));
    }

    @Test
    public void testProjection() {
        var nameLength = Linq.of("Alice", "Bob", "Charlie")
            .select(String::length)
            .toList();

        System.out.println(nameLength);
    }

    @Test
    public void testOrdering() {
        var people = Linq.of(
            new Person("Alice", 25),
            new Person("Bob", 20),
            new Person("Charlie", 25)
        );

        var ordered = people
            .orderBy(Person::age)
            .thenBy(Person::name)
            .toList();

        System.out.println(ordered);
    }

    @Test
    public void testGrouping() {
        var groups = Linq.of(
                new Person("Alice", 20),
                new Person("Bob", 20),
                new Person("Charlie", 25)
            )
            .groupBy(Person::age);

        groups.forEach(group ->
            System.out.println("Age " + group.getGroupKey() + ": " + group.getGroupElements())
        );

        var map = groups.toMap(
            Groupable::getGroupKey,
            Groupable::getGroupElements
        );

        System.out.println(map);
    }

    @Test
    public void testAggregation() {
        int sum = Linq.of(
            new Person("Alice", 20),
            new Person("Bob", 20),
            new Person("Charlie", 25)
        )
            .select(Person::age)
            .aggregateToResult(0, Integer::sum);
        System.out.println(sum);
    }

    @Test
    public void testPrimitive() {
        int total = IntEnumerable.ofInts(1, 2, 3, 4, 5)
            .sum();
        double avg = IntEnumerable.ofInts(1, 2, 3, 4, 5)
            .average();

        System.out.println("total = " + total + ", age = " + avg);
    }

    @Test
    public void testJoining() {
        record Order(int id, String customer) {}
        record Detail(int orderId, String product) {}

        var orders = Linq.of(
            new Order(1, "Alice"),
            new Order(2, "Bob")
        );
        var details = Linq.of(
            new Detail(1, "Product A"),
            new Detail(1, "Product B"),
            new Detail(2, "Product C")
        );

        var joined = orders.join(
            details,
            Order::id,
            Detail::orderId,
            (order, detail) -> order.customer() + " bought " + detail.product()
        );

        joined.forEach(System.out::println);
    }

    @Test
    public void testSetOperation() {
        var seq1 = Linq.of(1, 2, 3, 4);
        var seq2 = Linq.of(3, 4, 5, 6);

        var distinct = seq1
            .distinct()
            .toList();
        var except = seq1
            .except(seq2)
            .toList();
        var intersect = seq1
            .intersect(seq2)
            .toList();
        var union = seq1
            .union(seq2)
            .toList();

        System.out.println("distinct = " + distinct);
        System.out.println("except = " + except);
        System.out.println("intersect = " + intersect);
        System.out.println("union = " + union);
    }

    @Test
    public void testExample() {
        record Student(String name, int score) {}

        var names = Linq.of(
                new Student("Alice", 92),
                new Student("Bob", 75),
                new Student("Charlie", 88)
            )
            .where(student -> student.score() >= 80)
            .orderByDescending(Student::score)
            .select(Student::name)
            .toList();

        System.out.println(names);
    }
}
