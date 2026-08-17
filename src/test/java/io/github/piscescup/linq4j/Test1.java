package io.github.piscescup.linq4j;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public class Test1 {
    record Person(String name, int age, String address) implements Comparable<Person> {
        public String email() {
            return String.format("%s-%d@%s.com", name, age, name.charAt(0) + address.replace(" ", "").toUpperCase(Locale.ROOT));
        }

        @Override
        public int compareTo(@NonNull Person o) {
            return Integer.compare(this.age, o.age);
        }
    }

    private static final List<Person> TEST_DATA = List.of(
        new Person("Alice", 21, "New York"),
        new Person("Bob", 25, "Los Angeles"),
        new Person("Charlie", 19, "Chicago"),
        new Person("David", 30, "New York"),
        new Person("Eve", 25, "San Francisco"),
        new Person("Frank", 18, "Chicago"),
        new Person("Grace", 27, "Seattle"),
        new Person("Henry", 35, "Boston"),
        new Person("Ivy", 21, "Seattle"),
        new Person("Jack", 30, "Los Angeles"),
        new Person("Kate", 24, "New York"),
        new Person("Leo", 27, "Boston"),
        new Person("Mia", 19, "San Francisco"),
        new Person("Noah", 32, "Chicago"),
        new Person("Olivia", 24, "Seattle"),
        new Person("Peter", 40, "New York"),
        new Person("Queen", 18, "Boston"),
        new Person("Ryan", 32, "Los Angeles"),
        new Person("Sophia", 28, "San Francisco"),
        new Person("Tom", 28, "Chicago")
    );

    @Test
    public void test() {
        Enumerable<Person> people = Linq.of(TEST_DATA);

        System.out.println(people.max());

        people
            .select(Person::email)
            .forEach(System.out::println);
    }
}
