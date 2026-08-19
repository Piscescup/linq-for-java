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
public class DistinctBenchmark {

    @Param({"10",  "100", "10000"})
    private int size;

    private Enumerable<String> mineSimple;

    private Enumerable<String> mineComplex;

    private io.github.piscescup.linq.Enumerable<String>
        referenceSimple;

    private io.github.piscescup.linq.Enumerable<String>
        referenceComplex;

    @Setup(Level.Trial)
    public void setup() {

        List<Person> people =
            BenchmarkData.createPeople(size);

        List<String> simpleKeys =
            people.stream()
                .map(
                    person ->
                        person.getAddress().getCity()
                )
                .toList();

        List<String> complexKeys =
            BenchmarkData.createDistinctKeys(people);

        mineSimple = Linq.of(simpleKeys);
        mineComplex = Linq.of(complexKeys);

        referenceSimple =
            io.github.piscescup.linq.Linq
                .fromIterable(simpleKeys);

        referenceComplex =
            io.github.piscescup.linq.Linq
                .fromIterable(complexKeys);
    }

    @Benchmark
    public Object mineSimple() {
        return mineSimple
            .distinct()
            .toList();
    }

    @Benchmark
    public Object referenceSimple() {
        return referenceSimple
            .distinct()
            .toList();
    }

    @Benchmark
    public Object mineComplex() {
        return mineComplex
            .distinct()
            .toList();
    }

    @Benchmark
    public Object referenceComplex() {
        return referenceComplex
            .distinct()
            .toList();
    }
}