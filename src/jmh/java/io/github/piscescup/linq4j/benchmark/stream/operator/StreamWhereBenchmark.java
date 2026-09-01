package io.github.piscescup.linq4j.benchmark.stream.operator;

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
public class StreamWhereBenchmark {

    @Param({"10", "100", "1000", "10000"})
    private int size;

    private Enumerable<BenchmarkData.Person> mine;
    private List<BenchmarkData.Person> data;

    @Setup(Level.Trial)
    public void setup() {
        data = BenchmarkData.createPeople(size);
        mine = Linq.of(data);
    }

    @Benchmark
    public Object mineWhere() {
        return mine
            .where(BenchmarkFunctions::matchesComplexPredicate)
            .toList();
    }

    @Benchmark
    public Object streamWhere() {
        return data.stream()
            .filter(BenchmarkFunctions::matchesComplexPredicate)
            .toList();
    }
}
