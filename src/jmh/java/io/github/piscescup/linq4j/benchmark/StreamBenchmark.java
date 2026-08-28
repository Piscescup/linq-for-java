package io.github.piscescup.linq4j.benchmark;

import io.github.piscescup.linq4j.Linq;
import io.github.piscescup.linq4j.core.Enumerable;
import org.openjdk.jmh.annotations.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@State(Scope.Benchmark)
public class StreamBenchmark {

    @Param({"10", "100", "1000", "10000"})
    private int size;

    private Enumerable<BenchmarkData.Person> mine;

    private List<BenchmarkData.Person> data;

    @Setup(Level.Trial)
    public void setup() {
        data = BenchmarkData.createPeople(size);
        mine = Linq.of(data);
    }

    /*
     * ============================================================
     * Baseline
     * ============================================================
     */

    @Benchmark
    public Object mineBaseline() {
        return mine.toList();
    }

    @Benchmark
    public Object streamBaseline() {
        return data.stream().toList();
    }

    /*
     * ============================================================
     * Single operator: where / filter
     * ============================================================
     */

    @Benchmark
    public Object mineWhere() {
        return mine
            .where(StreamBenchmark::matchesComplexPredicate)
            .toList();
    }

    @Benchmark
    public Object streamWhere() {
        return data.stream()
            .filter(StreamBenchmark::matchesComplexPredicate)
            .toList();
    }

    /*
     * ============================================================
     * Single operator: select / map
     * ============================================================
     */

    @Benchmark
    public Object mineSelect() {
        return mine
            .select(BenchmarkData.Person::getAge)
            .toList();
    }

    @Benchmark
    public Object streamSelect() {
        return data.stream()
            .map(BenchmarkData.Person::getAge)
            .toList();
    }

    /*
     * ============================================================
     * Single operator: distinct
     * ============================================================
     */

    @Benchmark
    public Object mineDistinct() {
        return mine
            .select(person -> person.getAddress().getCity())
            .distinct()
            .toList();
    }

    @Benchmark
    public Object streamDistinct() {
        return data.stream()
            .map(person -> person.getAddress().getCity())
            .distinct()
            .toList();
    }

    /*
     * ============================================================
     * Single operator: groupBy
     * ============================================================
     */

    @Benchmark
    public Object mineGroupBy() {
        return mine
            .groupBy(person -> person.getAddress().getCity())
            .toList();
    }

    @Benchmark
    public Object streamGroupBy() {
        return data.stream()
            .collect(
                Collectors.groupingBy(
                    person -> person.getAddress().getCity(),
                    LinkedHashMap::new,
                    Collectors.toList()
                )
            );
    }

    /*
     * ============================================================
     * Single operator: orderBy / sorted
     * ============================================================
     */

    @Benchmark
    public Object mineOrderBy() {
        return mine
            .orderBy(BenchmarkData.Person::getAge)
            .toList();
    }

    @Benchmark
    public Object streamOrderBy() {
        return data.stream()
            .sorted(Comparator.comparingInt(BenchmarkData.Person::getAge))
            .toList();
    }

    /*
     * ============================================================
     * Combined operators: where + select
     * ============================================================
     */

    @Benchmark
    public Object mineWhereSelect() {
        return mine
            .where(person -> person.getAge() >= 18)
            .select(BenchmarkData.Person::getAge)
            .toList();
    }

    @Benchmark
    public Object streamWhereSelect() {
        return data.stream()
            .filter(person -> person.getAge() >= 18)
            .map(BenchmarkData.Person::getAge)
            .toList();
    }

    /*
     * ============================================================
     * Combined operators: where + groupBy
     * ============================================================
     */

    @Benchmark
    public Object mineWhereGroupBy() {
        return mine
            .where(StreamBenchmark::matchesComplexPredicate)
            .groupBy(StreamBenchmark::complexGroupKey)
            .toList();
    }

    @Benchmark
    public Object streamWhereGroupBy() {
        return data.stream()
            .filter(StreamBenchmark::matchesComplexPredicate)
            .collect(
                Collectors.groupingBy(
                    StreamBenchmark::complexGroupKey,
                    LinkedHashMap::new,
                    Collectors.toList()
                )
            );
    }

    /*
     * ============================================================
     * Pipeline depth
     * ============================================================
     */

    @Benchmark
    public long mineDepth1() {
        return mine
            .where(person -> person.getAge() >= 18)
            .count();
    }

    @Benchmark
    public long streamDepth1() {
        return data.stream()
            .filter(person -> person.getAge() >= 18)
            .count();
    }

    @Benchmark
    public long mineDepth2() {
        return mine
            .where(person -> person.getAge() >= 18)
            .select(person -> person)
            .count();
    }

    @Benchmark
    public long streamDepth2() {
        return data.stream()
            .filter(person -> person.getAge() >= 18)
            .map(person -> person)
            .count();
    }

    @Benchmark
    public long mineDepth4() {
        return mine
            .where(person -> person.getAge() >= 18)
            .select(person -> person)
            .where(person -> person.getSalary() >= 5_000.0)
            .select(person -> person)
            .count();
    }

    @Benchmark
    public long streamDepth4() {
        return data.stream()
            .filter(person -> person.getAge() >= 18)
            .map(person -> person)
            .filter(person -> person.getSalary() >= 5_000.0)
            .map(person -> person)
            .count();
    }

    /*
     * ============================================================
     * Scenario: grouping pipeline
     * ============================================================
     */

    @Benchmark
    public Object mineSimple() {
        return mine
            .groupBy(
                person -> person.getAddress().getCity()
            )
            .orderBy(
                group -> group.getGroupKey().charAt(0)
            )
            .thenBy(
                group -> group.getGroupElements().size()
            )
            .select(
                group -> group.getGroupElements().toString()
            )
            .distinct()
            .toList();
    }

    @Benchmark
    public Object streamSimple() {
        return data
            .stream()
            .collect(
                Collectors.groupingBy(
                    person -> person.getAddress().getCity(),
                    LinkedHashMap::new,
                    Collectors.toList()
                )
            )
            .entrySet()
            .stream()
            .sorted(
                Comparator
                    .<Map.Entry<String, List<BenchmarkData.Person>>>comparingInt(
                        entry -> entry.getKey().charAt(0)
                    )
                    .thenComparingInt(
                        entry -> entry.getValue().size()
                    )
            )
            .map(
                entry -> entry.getValue().toString()
            )
            .distinct()
            .toList();
    }

    /*
     * ============================================================
     * Scenario: complex pipeline
     * ============================================================
     */

    @Benchmark
    public Object mineComplex() {
        return mine
            .where(StreamBenchmark::matchesComplexPredicate)
            .groupBy(StreamBenchmark::complexGroupKey)
            .orderBy(
                group -> group.getGroupKey().length()
            )
            .thenBy(
                group -> group.getGroupElements().size()
            )
            .select(group -> {
                int count = group
                    .getGroupElements()
                    .size();

                double totalSalary = 0.0;
                double totalPerformance = 0.0;

                for (BenchmarkData.Person person :
                    group.getGroupElements()) {

                    totalSalary += person.getSalary();

                    totalPerformance += person
                        .getProfile()
                        .getPerformanceScore();
                }

                double averagePerformance = count == 0
                    ? 0.0
                    : totalPerformance / count;

                return group.getGroupKey() + ':' +
                    count + ':' +
                    Math.round(totalSalary) + ':' +
                    Math.round(averagePerformance * 100.0);
            })
            .distinct()
            .toList();
    }

    @Benchmark
    public Object streamComplex() {
        return data
            .stream()
            .filter(StreamBenchmark::matchesComplexPredicate)
            .collect(
                Collectors.groupingBy(
                    StreamBenchmark::complexGroupKey,
                    LinkedHashMap::new,
                    Collectors.toList()
                )
            )
            .entrySet()
            .stream()
            .sorted(
                Comparator
                    .<Map.Entry<String, List<BenchmarkData.Person>>>comparingInt(
                        entry -> entry.getKey().length()
                    )
                    .thenComparingInt(
                        entry -> entry.getValue().size()
                    )
            )
            .map(entry -> {
                List<BenchmarkData.Person> group =
                    entry.getValue();

                int count = group.size();

                double totalSalary = 0.0;
                double totalPerformance = 0.0;

                for (BenchmarkData.Person person : group) {
                    totalSalary += person.getSalary();

                    totalPerformance += person
                        .getProfile()
                        .getPerformanceScore();
                }

                double averagePerformance = count == 0
                    ? 0.0
                    : totalPerformance / count;

                return entry.getKey() + ':' +
                    count + ':' +
                    Math.round(totalSalary) + ':' +
                    Math.round(averagePerformance * 100.0);
            })
            .distinct()
            .toList();
    }

    /*
     * ============================================================
     * Shared benchmark functions
     * ============================================================
     */

    private static boolean matchesComplexPredicate(
        BenchmarkData.Person person
    ) {
        return person.isActive()
            && person.getAge() >= 23
            && person.getSalary() >= 7_000.0
            && person.getExperienceYears() >= 2
            && person.getProfile().getPerformanceScore() >= 60.0;
    }

    private static String complexGroupKey(
        BenchmarkData.Person person
    ) {
        return person.getDepartment().getDivision() + ':'
            + person.getAddress().getProvince() + ':'
            + person.getProfile().getEducation();
    }
}
