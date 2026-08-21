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
            .where(value -> value %2 == 0)
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
            .groupBy(Person::age)
            .toMap(
                Groupable::getGroupKey,
                Groupable::getGroupElements
            );

        System.out.println(groups);
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
        var result = IntEnumerable.ofInts(1, 2, 3, 4, 5) // or Linq.ofInts(1, 2, 3, 4, 5)
            .where(value -> value > 2)
            .select(value -> value * 2)
            .toArray();

        System.out.println(Arrays.toString(result));
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
