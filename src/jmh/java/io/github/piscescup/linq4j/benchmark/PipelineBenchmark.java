package io.github.piscescup.linq4j.benchmark;


import io.github.piscescup.linq4j.Enumerable;
import io.github.piscescup.linq4j.Linq;
import io.github.piscescup.linq4j.benchmark.BenchmarkData.Person;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class PipelineBenchmark {

    @Param({"10", "10000"})
    private int size;

    private Enumerable<Person> mine;

    private io.github.piscescup.linq.Enumerable<Person>
        reference;

    @Setup(Level.Trial)
    public void setup() {

        List<Person> data =
            BenchmarkData.createPeople(size);

        mine = Linq.of(data);

        reference =
            io.github.piscescup.linq.Linq
                .fromIterable(data);
    }

    @Benchmark
    public Object mineSimple() {
        return mine
            .groupBy(person ->
                person
                    .getAddress()
                    .getCity()
            )
            .orderBy(group ->
                group
                    .getGroupKey()
                    .charAt(0)
            )
            .thenBy(group ->
                group
                    .getGroupElements()
                    .size()
            )
            .select(group ->
                group
                    .getGroupElements()
                    .toString()
            )
            .distinct()
            .toList();
    }

    @Benchmark
    public Object referenceSimple() {
        return reference
            .groupBy(person ->
                person
                    .getAddress()
                    .getCity()
            )
            .orderBy(group ->
                group
                    .key()
                    .charAt(0)
            )
            .thenBy(group ->
                group
                    .elements()
                    .size()
            )
            .select(group ->
                group
                    .elements()
                    .toString()
            )
            .distinct()
            .toList();
    }

    @Benchmark
    public Object mineComplex() {
        return mine
            .where(person ->
                person.isActive()
                    && person.getAge() >= 23
                    && person.getSalary() >= 7_000.0
                    && person.getExperienceYears() >= 2
                    && person
                    .getProfile()
                    .getPerformanceScore() >= 60.0
            )
            .groupBy(person ->
                person
                    .getDepartment()
                    .getDivision()
                    + ':'
                    + person
                    .getAddress()
                    .getProvince()
                    + ':'
                    + person
                    .getProfile()
                    .getEducation()
            )
            .orderBy(group ->
                group
                    .getGroupKey()
                    .length()
            )
            .thenBy(group ->
                group
                    .getGroupElements()
                    .size()
            )
            .select(group -> {

                int count =
                    group
                        .getGroupElements()
                        .size();

                double totalSalary = 0.0;
                double totalPerformance = 0.0;

                for (Person person :
                    group.getGroupElements()) {

                    totalSalary +=
                        person.getSalary();

                    totalPerformance +=
                        person
                            .getProfile()
                            .getPerformanceScore();
                }

                double averagePerformance =
                    count == 0
                        ? 0.0
                        : totalPerformance / count;

                return group.getGroupKey()
                    + ':'
                    + count
                    + ':'
                    + Math.round(totalSalary)
                    + ':'
                    + Math.round(
                    averagePerformance * 100.0
                );
            })
            .distinct()
            .toList();
    }

    @Benchmark
    public Object referenceComplex() {
        return reference
            .where(person ->
                person.isActive()
                    && person.getAge() >= 23
                    && person.getSalary() >= 7_000.0
                    && person.getExperienceYears() >= 2
                    && person
                    .getProfile()
                    .getPerformanceScore() >= 60.0
            )
            .groupBy(person ->
                person
                    .getDepartment()
                    .getDivision()
                    + ':'
                    + person
                    .getAddress()
                    .getProvince()
                    + ':'
                    + person
                    .getProfile()
                    .getEducation()
            )
            .orderBy(group ->
                group
                    .key()
                    .length()
            )
            .thenBy(group ->
                group
                    .elements()
                    .size()
            )
            .select(group -> {

                int count =
                    group
                        .elements()
                        .size();

                double totalSalary = 0.0;
                double totalPerformance = 0.0;

                for (Person person :
                    group.elements()) {

                    totalSalary +=
                        person.getSalary();

                    totalPerformance +=
                        person
                            .getProfile()
                            .getPerformanceScore();
                }

                double averagePerformance =
                    count == 0
                        ? 0.0
                        : totalPerformance / count;

                return group.key()
                    + ':'
                    + count
                    + ':'
                    + Math.round(totalSalary)
                    + ':'
                    + Math.round(
                    averagePerformance * 100.0
                );
            })
            .distinct()
            .toList();
    }
}