package io.github.piscescup.linq4j.benchmark;


import io.github.piscescup.linq4j.Enumerable;
import io.github.piscescup.linq4j.Linq;
import io.github.piscescup.linq4j.benchmark.BenchmarkData.DepartmentBudget;
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
public class JoinBenchmark {

    @Param({"10", "10000"})
    private int size;

    private Enumerable<Person> minePeople;

    private Enumerable<DepartmentBudget> mineBudgets;

    private io.github.piscescup.linq.Enumerable<Person>
        referencePeople;

    private io.github.piscescup.linq.Enumerable<DepartmentBudget>
        referenceBudgets;

    @Setup(Level.Trial)
    public void setup() {

        List<Person> people =
            BenchmarkData.createPeople(size);

        List<DepartmentBudget> budgets =
            BenchmarkData.createDepartmentBudgets();

        minePeople = Linq.of(people);
        mineBudgets = Linq.of(budgets);

        referencePeople =
            io.github.piscescup.linq.Linq
                .fromIterable(people);

        referenceBudgets =
            io.github.piscescup.linq.Linq
                .fromIterable(budgets);
    }

    @Benchmark
    public Object mineSimple() {
        return minePeople
            .join(
                mineBudgets,
                person ->
                    person
                        .getDepartment()
                        .getId(),
                DepartmentBudget::getDepartmentId,
                (person, budget) ->
                    person.getId()
                        + ":"
                        + budget.getDepartmentId()
            )
            .toList();
    }

    @Benchmark
    public Object referenceSimple() {
        return referencePeople
            .join(
                referenceBudgets,
                person ->
                    person
                        .getDepartment()
                        .getId(),
                DepartmentBudget::getDepartmentId,
                (person, budget) ->
                    person.getId()
                        + ":"
                        + budget.getDepartmentId()
            )
            .toList();
    }

    @Benchmark
    public Object mineComplex() {
        return minePeople
            .join(
                mineBudgets,
                person ->
                    person
                        .getDepartment()
                        .getId(),
                DepartmentBudget::getDepartmentId,
                (person, budget) -> {

                    double salaryRatio =
                        person.getSalary()
                            / budget.getAnnualBudget();

                    double performance =
                        person
                            .getProfile()
                            .getPerformanceScore();

                    return person.getFullName()
                        + ':'
                        + person
                        .getDepartment()
                        .getDivision()
                        + ':'
                        + budget.getCostCenter()
                        + ':'
                        + Math.round(
                        salaryRatio
                            * performance
                            * 100_000.0
                    );
                }
            )
            .toList();
    }

    @Benchmark
    public Object referenceComplex() {
        return referencePeople
            .join(
                referenceBudgets,
                person ->
                    person
                        .getDepartment()
                        .getId(),
                DepartmentBudget::getDepartmentId,
                (person, budget) -> {

                    double salaryRatio =
                        person.getSalary()
                            / budget.getAnnualBudget();

                    double performance =
                        person
                            .getProfile()
                            .getPerformanceScore();

                    return person.getFullName()
                        + ':'
                        + person
                        .getDepartment()
                        .getDivision()
                        + ':'
                        + budget.getCostCenter()
                        + ':'
                        + Math.round(
                        salaryRatio
                            * performance
                            * 100_000.0
                    );
                }
            )
            .toList();
    }
}