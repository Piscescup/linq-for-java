package io.github.piscescup.linq4j.benchmark.linq.operator;

import io.github.piscescup.linq4j.Linq;
import io.github.piscescup.linq4j.benchmark.common.BenchmarkData;
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
public class DistinctBenchmark {

    @Param({"10", "100", "10000"})
    private int size;

    private Enumerable<String> mineSimple;
    private Enumerable<String> mineComplex;
    private io.github.piscescup.linq.Enumerable<String> referenceSimple;
    private io.github.piscescup.linq.Enumerable<String> referenceComplex;

    @Setup(Level.Trial)
    public void setup() {
        List<BenchmarkData.Person> people = BenchmarkData.createPeople(size);
        List<String> simpleKeys = people.stream()
            .map(person -> person.getAddress().getCity())
            .toList();
        List<String> complexKeys = BenchmarkData.createDistinctKeys(people);

        mineSimple = Linq.of(simpleKeys);
        mineComplex = Linq.of(complexKeys);
        referenceSimple = io.github.piscescup.linq.Linq.fromIterable(simpleKeys);
        referenceComplex = io.github.piscescup.linq.Linq.fromIterable(complexKeys);
    }

    @Benchmark
    public Object mineSimple() {
        return mineSimple.distinct().toList();
    }

    @Benchmark
    public Object referenceSimple() {
        return referenceSimple.distinct().toList();
    }

    @Benchmark
    public Object mineComplex() {
        return mineComplex.distinct().toList();
    }

    @Benchmark
    public Object referenceComplex() {
        return referenceComplex.distinct().toList();
    }
}
