# LINQ for Java

A Java implementation inspired by [.NET LINQ](https://learn.microsoft.com/en-us/dotnet/csharp/linq/), providing an `Enumerable`-based query model and a rich set of composable query operators.

---

## Introduction to LINQ

### What is LINQ?

[Language Integrated Query](https://learn.microsoft.com/en-us/dotnet/csharp/linq/) (LINQ) is a query model introduced by .NET that provides a consistent and expressive way to query, transform, and aggregate data.

Instead of manually writing loops, temporary collections, and intermediate state, LINQ allows data-processing operations to be composed into fluent query pipelines.

### LINQ Expressions

In C#, LINQ supports both query syntax:

```csharp
// Specify the data source.
int[] numbers = [ 5, 10, 8, 3, 6, 12 ];

// Query syntax:
IEnumerable<int> numQuery1 =
    from num in numbers
    where num % 2 == 0
    orderby num
    select num;

// Execute the query.
foreach (var i in numQuery1 )
{
    Console.Write(i + " ");
}

// Output: 6 8 10 12
```

and method syntax:

```csharp
// Specify the data source.
int[] numbers = [ 5, 10, 8, 3, 6, 12 ];

//Method syntax:
IEnumerable<int> numQuery2 = numbers
    .Where(num => num % 2 == 0)
    .OrderBy(n => n);
  
// Execute the query.
foreach (var i in numQuery2 )
{
    Console.Write(i + " ");
}

// Output: 6 8 10 12
```

### LINQ Features

* **Unified Query Model**

  Provides a consistent way to query and process different kinds of data sources.
* **Deferred Execution**

  Many LINQ operations do not execute immediately. A query is evaluated only when its results are actually enumerated or consumed.
* **Composability**

  Query operators such as `where`, `select`, `orderBy`, and `groupBy` can be chained together to build complex query pipelines.
* **Rich Query Operators**

  Provides operations for filtering, projection, sorting, grouping, aggregation, joining, set operations, element retrieval, and more.
* **Functional Programming Support**

  LINQ works naturally with lambda expressions and functions, making data transformations concise and expressive.

### LINQ vs Java Stream

Although Java `Stream` and C# LINQ both support functional-style data processing, they are based on different programming models.

The Java `Stream API` focuses primarily on stream-processing operations such as filtering, mapping, reducing, and parallel execution. A `Stream` is generally intended to be consumed once and cannot be reused after a terminal operation.

LINQ, on the other hand, provides a broader query model based on enumerable sequences. In addition to filtering and projection, LINQ provides a rich collection of query operators for grouping, joining, ordering, set operations, element retrieval, sequence comparison, and more.

Some typical differences include:

* A Java `Stream` is single-use, while an `IEnumerable<T>` represents an enumerable sequence that can generally be enumerated multiple times.
* LINQ provides dedicated query operators such as `Join`, `GroupJoin`, `GroupBy`, `First`, `Last`, `Except`, and `Intersect`.
* LINQ places greater emphasis on expressing data access as composable queries, while Java `Stream` is primarily designed as a functional data-processing pipeline.
* Both models support lazy or deferred processing for many intermediate operations.

The following example shows a simple comparison between Java `Stream` and `linq-for-java`:

```java
// Java Stream
var result = people.stream()
    .filter(person -> person.age() >= 18)
    .sorted(Comparator.comparing(Person::name))
    .map(Person::name)
    .toList();

// linq-for-java
var peoples = Linq.of(people);

var adultNames = peoples
    .where(person -> person.age() >= 18)
    .orderBy(Person::name)
    .select(Person::name)
    .toList();

var ageSum = peoples
    .selectToInt(Person::age)
    .sum();
```

`linq-for-java` is designed to bring the LINQ-style enumerable and query-operator model to Java rather than replace the standard Java `Stream API`.

---

## About `linq-for-java`

`linq-for-java` is a Java library inspired by .NET LINQ. It provides an `Enumerable`-based query model and a rich set of composable query operators for Java.

The goal of this project is not to replace the Java `Stream API`, but to provide a LINQ-style programming experience for developers who prefer the query model and API design of .NET LINQ.

With `linq-for-java`, queries can be written as fluent pipelines using familiar operations such as `where`, `select`, `orderBy`, `groupBy`, `join`, `aggregate`, and many others.

```java
var result = Linq.of(people)
    .where(person -> person.age() >= 18)
    .orderBy(Person::name)
    .select(Person::name)
    .toList();
```

The library also provides specialized enumerable types for primitive values, including `IntEnumerable`, `LongEnumerable`, and `DoubleEnumerable`, to reduce unnecessary boxing and unboxing in primitive-data pipelines.

## Features

* LINQ-style `Enumerable` query model
* Fluent and composable query operators
* Deferred execution
* Filtering with `where`
* Projection with `select`
* Grouping with `groupBy`
* Joining with `join` and `groupJoin`
* Ordering with `orderBy` and `thenBy`
* Aggregation with `aggregate`, `count`, `min`, `max`, and more
* Set operations such as `distinct`, `except`, and `intersect`
* Element operators such as `first`, `last`, and `elementAt`
* Java lambda and functional-interface support
* Primitive-specialized enumerable types:

  * `IntEnumerable`
  * `LongEnumerable`
  * `DoubleEnumerable`

---

## Installation

The latest version of `linq-for-java` is available from the [Maven Central Repository](https://central.sonatype.com/).

### Maven

```xml
<dependency>
    <groupId>io.github.piscescup</groupId>
    <artifactId>linq-for-java</artifactId>
    <version>${linq_for_java_version}</version>
</dependency>
```

### Gradle

```groovy
dependencies {
    implementation "io.github.piscescup:linq-for-java:${linq_for_java_version}"
}
```

### Gradle Kotlin DSL

```kotlin
dependencies {
    implementation("io.github.piscescup:linq-for-java:$linqForJavaVersion")
}
```

---
## Quick Start
Create an enumerable sequence using Linq:

```java
import io.github.piscescup.linq4j.Linq;

// From varargs
var numbers = Linq.of(1, 2, 3, 4, 5);

// From array
String[] array = {"a", "b", "c"};
var strings = Linq.of(array);

// From Collection
List<String> list = List.of("x", "y", "z");
var fromList = Linq.of(list);

// Primitive specific
var ints = Linq.ofInts(1, 2, 3, 4, 5);
```
Define a sample data class for the following examples:

```java
import org.jetbrains.annotations.NotNull;

record Person(String name, int age) {
    @Override
    @NotNull
    public String toString() {
        return name + "-" + age;
    }
}
```
### Filtering
Use where to select elements that satisfy a condition:

```java
var evens = Linq.ofInts(1, 2, 3, 4, 5)
    .where(value -> value % 2 == 0)
    .toArray();

System.out.println(Arrays.toString(evens));
```
Output:
```text
[2, 4]
```

### Projection
Use select to transform each element:

```java
var nameLengths = Linq.of("Alice", "Bob", "Charlie")
    .select(String::length)
    .toList();

System.out.println(nameLengths);
```
Output: 
```text
[5, 3, 7]
```
### Ordering
Use orderBy and thenBy to perform multi-level ordering:

```java
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
```
Output: 
```test
[Bob-20, Alice-25, Charlie-25]
```
### Grouping
Use groupBy to group elements by a selected key. Each group is represented by a Grouping<K, V> interface, which provides getKey() and getElements():
```java
var groups = Linq.of(
        new Person("Alice", 20),
        new Person("Bob", 20),
        new Person("Charlie", 25)
    )
    .groupBy(Person::age);

// Iterate groups
groups.forEach(group ->
    System.out.println(
        "Age " + group.getGroupKey() + ": " 
        + group.getGroupElements()
));
```
Output: 
```test
Age 20: [Alice-20, Bob-20]
Age 25: [Charlie-25]
```

```java
// Or convert the Groupable to a Map
var map = groups.toMap(
    Grouping::getKey,
    Grouping::getElements
);
System.out.println(map);
```
Output: 
```text
{20=[Alice-20, Bob-20], 25=[Charlie-25]}
```

### Aggregation
LINQ-style aggregation can be used to reduce a sequence into a single value:

```java
int sum = Linq.of(
            new Person("Alice", 20),
            new Person("Bob", 20),
            new Person("Charlie", 25)
        )
        .select(Person::age)
        .aggregateToResult(0, Integer::sum); // identity + accumulator

System.out.println(sum);
```
Output:
```text
65
```
For primitive enumerable, dedicated methods like sum(), average(), min(), and max() are available:


```java
int total = IntEnumerable.ofInts(1, 2, 3, 4, 5)
        .sum();
double avg = IntEnumerable.ofInts(1, 2, 3, 4, 5)
        .average();

System.out.println("total = " + total + ", age = " + avg);
```
Output:
```text
total = 15, avg = 3.0
```
### Joins
Perform inner joins using join:

```java
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
    Detail::orderId
    (order, detail) -> order.customer() + " bought " + detail.product()
);

joined.forEach(System.out::println);
```

Output:
```text
Alice bought Product A
Alice bought Product B
Bob bought Product C
```

### Set Operations
Use `distinct`, `except`, `intersect`, and `union`:

```java
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
System.out.println("union = " + union);            // [1, 2, 3, 4, 5, 6]
```

Output: 
```text
distinct = [1, 2, 3, 4]
except = [1, 2]
intersect = [3, 4]
union = [1, 2, 3, 4, 5, 6]
```

---

## Primitive Enumerable

For primitive values, `linq-for-java` provides specialized enumerable types to reduce unnecessary boxing and unboxing.

Currently supported primitive enumerable types are:

* `IntEnumerable`
* `LongEnumerable`
* `DoubleEnumerable`

To create a primitive enumerable, use the corresponding factory method provided by `Linq` or the specialized enumerable type.

```java
var result = IntEnumerable.ofInts(1, 2, 3, 4, 5)
    // or Linq.ofInts(1, 2, 3, 4, 5)
    .where(value -> value > 2)
    .select(value -> value * 2)
    .toArray();

System.out.println(Arrays.toString(result));
```

Output:

```text
[6, 8, 10]
```

---

## Deferred Execution

Many query operators in `linq-for-java` use deferred execution.

Creating a query does not necessarily enumerate the source immediately. Instead, the query describes how the source should be processed when the sequence is consumed.

```java
var query = Linq.of(1, 2, 3, 4, 5)
            .where(value -> {
              System.out.println("Filtering: " + value);
              return value > 2;
            })
            .select(value -> value * 10);
```
The filter lambda above will not execute until query is enumerated by a terminal operators executed:

```java
// The lambda executes only when toList() is called
var result = query.toList();
// Console output: Filtering: 1, Filtering: 2, Filtering: 3, Filtering: 4, Filtering: 5
```

Below are some intermediate and terminal operators:
- **Intermediate operators**: `where`, `select`, `orderBy`, `skip`, `take`, `distinct`, `union`, etc.

- **Terminal operators**: `toList()`, `toArray()`, `toMap()`, `aggregateToResult()`, `count()`, `first()`, `forEach()`, `sum()`, etc.

---

## More Examples

More usage examples can be found in the project's tests:

* [Enumerable Examples](https://github.com/Piscescup/linq-for-java/blob/main/src/test/java/io/github/piscescup/linq4j/EnumerableTest.java)
* [IntEnumerable Examples](https://github.com/Piscescup/linq-for-java/blob/main/src/test/java/io/github/piscescup/linq4j/primitive/IntEnumerableTest.java)
* [LongEnumerable Examples](https://github.com/Piscescup/linq-for-java/blob/main/src/test/java/io/github/piscescup/linq4j/primitive/LongEnumerableTest.java)
* [DoubleEnumerable Examples](https://github.com/Piscescup/linq-for-java/blob/main/src/test/java/io/github/piscescup/linq4j/primitive/DoubleEnumerableTest.java)

---

## Requirements

* **JDK 25 or later**: This project leverages modern Java features such as record types, pattern matching, and unnamed variables.
* [`commons-lib`](https://central.sonatype.com/artifact/io.github.piscescup/commons-lib): A lightweight utility dependency used internally.

---

## Example

```java
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
```

Output:

```text
[Alice, Charlie]
```

---

## Contributing

Contributions are welcome! Here’s how you can help:

1. Fork the [linq-for-java](https://github.com/Piscescup/linq-for-java) repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add some amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

Please ensure your code adheres to the existing style and includes appropriate tests. For major changes, please open an issue first to discuss what you would like to change.

## License

See the `LICENSE` file for details.
