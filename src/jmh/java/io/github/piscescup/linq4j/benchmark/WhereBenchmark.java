package io.github.piscescup.linq4j.benchmark;


import io.github.piscescup.linq4j.Enumerable;
import io.github.piscescup.linq4j.Linq;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;



@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class WhereBenchmark {

    @Param({"10",  "10000"})
    private int size;

    private Enumerable<BenchmarkData.Person> mine;

    private io.github.piscescup.linq.Enumerable<BenchmarkData.Person> reference;

    @Setup(Level.Trial)
    public void setup() {

        List<BenchmarkData.Person> data =
            BenchmarkData.createPeople(size);

        mine = Linq.of(data);

        reference =
            io.github.piscescup.linq.Linq.fromIterable(data);
    }

    @Benchmark
    public Object mineSimple() {
        return mine
            .where(person ->
                (person.getId() & 1L) == 0L
            )
            .toList();
    }

    @Benchmark
    public Object referenceSimple() {
        return reference
            .where(person ->
                (person.getId() & 1L) == 0L
            )
            .toList();
    }

    @Benchmark
    public Object mineComplex() {
        return mine
            .where(person ->
                person.isActive()
                    && person.getAge() >= 25
                    && person.getAge() <= 50
                    && person.getSalary() >= 8_000.0
                    && person.getExperienceYears() >= 3
                    && person
                    .getProfile()
                    .getPerformanceScore() >= 65.0
                    && person
                    .getProfile()
                    .getCompletedProjects() >= 5
                    && !person.getSkills().isEmpty()
            )
            .toList();
    }

    @Benchmark
    public Object referenceComplex() {
        return reference
            .where(person ->
                person.isActive()
                    && person.getAge() >= 25
                    && person.getAge() <= 50
                    && person.getSalary() >= 8_000.0
                    && person.getExperienceYears() >= 3
                    && person
                    .getProfile()
                    .getPerformanceScore() >= 65.0
                    && person
                    .getProfile()
                    .getCompletedProjects() >= 5
                    && !person.getSkills().isEmpty()
            )
            .toList();
    }
}