package io.github.piscescup.linq4j.benchmark.linq.operator;

import io.github.piscescup.linq4j.Linq;
import io.github.piscescup.linq4j.benchmark.common.BenchmarkData;
import io.github.piscescup.linq4j.benchmark.common.BenchmarkFunctions;
import io.github.piscescup.linq4j.core.Enumerable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@State(Scope.Benchmark)
public class WhereBenchmark {

    @Param({"10", "100", "10000"})
    private int size;

    private Enumerable<BenchmarkData.Person> mine;
    private io.github.piscescup.linq.Enumerable<BenchmarkData.Person> reference;

    @Setup(Level.Trial)
    public void setup() {
        List<BenchmarkData.Person> data = BenchmarkData.createPeople(size);
        mine = Linq.of(data);
        reference = io.github.piscescup.linq.Linq.fromIterable(data);
    }

    @Benchmark
    public Object mineSimple() {
        return mine.where(person -> (person.getId() & 1L) == 0L).toList();
    }

    @Benchmark
    public Object referenceSimple() {
        return reference.where(person -> (person.getId() & 1L) == 0L).toList();
    }

    @Benchmark
    public Object mineComplex() {
        return mine.where(BenchmarkFunctions::matchesStrictWherePredicate).toList();
    }

    @Benchmark
    public Object referenceComplex() {
        return reference.where(BenchmarkFunctions::matchesStrictWherePredicate).toList();
    }
}
