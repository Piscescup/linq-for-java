package io.github.piscescup.linq4j.benchmark.linq.scenario;

import io.github.piscescup.linq4j.Linq;
import io.github.piscescup.linq4j.benchmark.common.BenchmarkData;
import io.github.piscescup.linq4j.benchmark.common.BenchmarkData.Person;
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
public class ReferenceGroupingPipelineBenchmark {

    @Param({"10", "100", "10000"})
    private int size;

    private Enumerable<Person> mine;
    private io.github.piscescup.linq.Enumerable<Person> reference;

    @Setup(Level.Trial)
    public void setup() {
        List<Person> data = BenchmarkData.createPeople(size);
        mine = Linq.of(data);
        reference = io.github.piscescup.linq.Linq.fromIterable(data);
    }

    @Benchmark
    public Object mineGroupingPipeline() {
        return mine
            .groupBy(person -> person.getAddress().getCity())
            .orderBy(group -> group.getGroupKey().charAt(0))
            .thenBy(group -> group.getGroupElements().size())
            .select(group -> group.getGroupElements().toString())
            .distinct()
            .toList();
    }

    @Benchmark
    public Object referenceGroupingPipeline() {
        return reference
            .groupBy(person -> person.getAddress().getCity())
            .orderBy(group -> group.key().charAt(0))
            .thenBy(group -> group.elements().size())
            .select(group -> group.elements().toString())
            .distinct()
            .toList();
    }
}
