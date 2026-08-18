package io.github.piscescup.linq4j.benchmark;


import io.github.piscescup.linq4j.benchmark.BenchmarkData.Person;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.piscescup.linq4j.Enumerable;
import io.github.piscescup.linq4j.Linq;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class OrderByBenchmark {

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
            .orderBy(Person::getAge)
            .toList();
    }

    @Benchmark
    public Object referenceSimple() {
        return reference
            .orderBy(Person::getAge)
            .toList();
    }

    @Benchmark
    public Object mineComplex() {
        return mine
            .orderBy(person ->
                person
                    .getDepartment()
                    .getLevel()
                    * 10_000
                    + person
                    .getExperienceYears()
                    * 100
                    + person
                    .getProfile()
                    .getCompletedProjects()
            )
            .toList();
    }

    @Benchmark
    public Object referenceComplex() {
        return reference
            .orderBy(person ->
                person
                    .getDepartment()
                    .getLevel()
                    * 10_000
                    + person
                    .getExperienceYears()
                    * 100
                    + person
                    .getProfile()
                    .getCompletedProjects()
            )
            .toList();
    }

    @Benchmark
    public Object mineThenBy() {
        return mine
            .orderBy(person ->
                person
                    .getDepartment()
                    .getDivision()
            )
            .thenBy(Person::getAge)
            .thenBy(Person::getLastName)
            .toList();
    }

    @Benchmark
    public Object referenceThenBy() {
        return reference
            .orderBy(person ->
                person
                    .getDepartment()
                    .getDivision()
            )
            .thenBy(Person::getAge)
            .thenBy(Person::getLastName)
            .toList();
    }

    @Benchmark
    public Object mineComplexThenBy() {
        return mine
            .orderBy(person ->
                person
                    .getDepartment()
                    .getLevel()
            )
            .thenBy(person ->
                person.getExperienceYears()
                    * 100
                    + person
                    .getProfile()
                    .getCompletedProjects()
            )
            .thenBy(person ->
                person
                    .getAddress()
                    .getCity()
                    + ':'
                    + person.getLastName()
            )
            .toList();
    }

    @Benchmark
    public Object referenceComplexThenBy() {
        return reference
            .orderBy(person ->
                person
                    .getDepartment()
                    .getLevel()
            )
            .thenBy(person ->
                person.getExperienceYears()
                    * 100
                    + person
                    .getProfile()
                    .getCompletedProjects()
            )
            .thenBy(person ->
                person
                    .getAddress()
                    .getCity()
                    + ':'
                    + person.getLastName()
            )
            .toList();
    }
}