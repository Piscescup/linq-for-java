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
@Fork(3)
@State(Scope.Benchmark)
public class GroupByBenchmark {

    @Param({"10",  "100", "10000"})
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
            .groupBy(
                person ->
                    person
                        .getDepartment()
                        .getId()
            )
            .toList();
    }

    @Benchmark
    public Object referenceSimple() {
        return reference
            .groupBy(
                person ->
                    person
                        .getDepartment()
                        .getId()
            )
            .toList();
    }

    @Benchmark
    public Object mineComplex() {
        return mine
            .groupBy(person ->
                person
                    .getDepartment()
                    .getDivision()
                    + ':'
                    + person
                    .getAddress()
                    .getProvince()
                    + ':'
                    + person.getAge() / 10
                    + ':'
                    + person
                    .getProfile()
                    .getEducation()
            )
            .toList();
    }

    @Benchmark
    public Object referenceComplex() {
        return reference
            .groupBy(person ->
                person
                    .getDepartment()
                    .getDivision()
                    + ':'
                    + person
                    .getAddress()
                    .getProvince()
                    + ':'
                    + person.getAge() / 10
                    + ':'
                    + person
                    .getProfile()
                    .getEducation()
            )
            .toList();
    }
}