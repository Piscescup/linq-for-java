package io.github.piscescup.linq4j.primitive;

import io.github.piscescup.linq4j.core.DoubleEnumerable;
import io.github.piscescup.linq4j.Linq;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DoubleEnumerableTest {

    private static final double DELTA = 1e-12;

    @Test
    void aggregateToResult() {
        assertEquals(10.0, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).aggregateToResult(0.0, Double::sum), DELTA);
    }

    @Test
    void all_any_andShortCircuit() {
        assertTrue(Linq.ofDoubles(new double[0]).all(value -> value > 0.0));
        assertTrue(Linq.ofDoubles(2.0, 4.0, 6.0).all(value -> value % 2.0 == 0.0));
        assertFalse(Linq.ofDoubles(2.0, 3.0, 6.0).all(value -> value % 2.0 == 0.0));
        assertFalse(Linq.ofDoubles(new double[0]).any());
        assertTrue(Linq.ofDoubles(1.0).any());
        assertTrue(Linq.ofDoubles(1.0, 2.0, 3.0).any(value -> value == 2.0));

        AtomicInteger calls = new AtomicInteger();
        assertTrue(Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).any(value -> {
            calls.incrementAndGet();
            return value == 2.0;
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void append_prepend_concat_defaultIfEmpty() {
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0}, Linq.ofDoubles(1.0, 2.0, 3.0).append(4.0).toArray());
        assertArrayEquals(new double[]{0.0, 1.0, 2.0, 3.0}, Linq.ofDoubles(1.0, 2.0, 3.0).prepend(0.0).toArray());
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0}, Linq.ofDoubles(1.0, 2.0).concat(Linq.ofDoubles(3.0, 4.0)).toArray());
        assertArrayEquals(new double[]{99.0}, Linq.ofDoubles(new double[0]).defaultIfEmpty(99.0).toArray());
    }

    @Test
    void contains_usesDoubleBitEquality() {
        assertTrue(Linq.ofDoubles(1.0, 2.0, 3.0).contains(2.0));
        assertFalse(Linq.ofDoubles(1.0, 2.0, 3.0).contains(4.0));
        assertTrue(Linq.ofDoubles(Double.NaN).contains(Double.longBitsToDouble(0x7ff0000000000001L)));
        assertTrue(Linq.ofDoubles(0.0).contains(0.0));
        assertFalse(Linq.ofDoubles(0.0).contains(-0.0));
    }

    @Test
    void count_allOverloads() {
        assertEquals(4L, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).count());
        assertEquals(2L, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).count(value -> value % 2.0 == 0.0));
    }

    @Test
    void elementAt_allVariants() {
        assertEquals(20.0, Linq.ofDoubles(10.0, 20.0, 30.0).elementAt(1), DELTA);
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.ofDoubles(10.0, 20.0, 30.0).elementAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.ofDoubles(10.0, 20.0, 30.0).elementAt(3));
        assertEquals(OptionalDouble.of(20.0), Linq.ofDoubles(10.0, 20.0, 30.0).elementAtOrEmpty(1));
        assertTrue(Linq.ofDoubles(10.0, 20.0, 30.0).elementAtOrEmpty(99).isEmpty());
        assertEquals(99.0, Linq.ofDoubles(10.0, 20.0, 30.0).elementAtOrDefault(99, 99.0), DELTA);
    }

    @Test
    void first_last_single_allVariants() {
        assertEquals(1.0, Linq.ofDoubles(1.0, 2.0, 3.0).first(), DELTA);
        assertEquals(2.0, Linq.ofDoubles(1.0, 2.0, 3.0).first(value -> value % 2.0 == 0.0), DELTA);
        assertTrue(Linq.ofDoubles(new double[0]).firstOrEmpty().isEmpty());

        assertEquals(3.0, Linq.ofDoubles(1.0, 2.0, 3.0).last(), DELTA);
        assertEquals(4.0, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).last(value -> value % 2.0 == 0.0), DELTA);
        assertThrows(NoSuchElementException.class, () -> Linq.ofDoubles(new double[0]).last());
        assertEquals(OptionalDouble.of(3.0), Linq.ofDoubles(1.0, 2.0, 3.0).lastOrEmpty());
        assertTrue(Linq.ofDoubles(new double[0]).lastOrEmpty().isEmpty());

        assertEquals(42.0, Linq.ofDoubles(42.0).single(), DELTA);
        assertThrows(NoSuchElementException.class, () -> Linq.ofDoubles(new double[0]).single());
        assertThrows(IllegalStateException.class, () -> Linq.ofDoubles(1.0, 2.0).single());
        assertEquals(2.0, Linq.ofDoubles(1.0, 2.0, 3.0).single(value -> value % 2.0 == 0.0), DELTA);
        assertThrows(IllegalStateException.class, () -> Linq.ofDoubles(2.0, 4.0).single(value -> value % 2.0 == 0.0));
        assertEquals(OptionalDouble.of(42.0), Linq.ofDoubles(42.0).singleOrEmpty());
        assertTrue(Linq.ofDoubles(new double[0]).singleOrEmpty().isEmpty());
    }

    @Test
    void numericAggregations() {
        assertEquals(10.0, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).sum(), DELTA);
        assertEquals(1.0, Linq.ofDoubles(3.0, 1.0, 4.0, 2.0).min(), DELTA);
        assertEquals(4.0, Linq.ofDoubles(3.0, 1.0, 4.0, 2.0).max(), DELTA);
        assertEquals(2.5, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).average(), DELTA);
        assertEquals(0.0, Linq.ofDoubles(new double[0]).sum(), DELTA);
        assertThrows(NoSuchElementException.class, () -> Linq.ofDoubles(new double[0]).min());
        assertThrows(NoSuchElementException.class, () -> Linq.ofDoubles(new double[0]).max());
        assertThrows(ArithmeticException.class, () -> Linq.ofDoubles(new double[0]).average());
    }

    @Test
    void where_select_andCrossProjection() {
        assertArrayEquals(
            new double[]{20.0, 40.0, 60.0},
            Linq.ofDoubles(1.0, 2.0, 3.0, 4.0, 5.0, 6.0).where(value -> value % 2.0 == 0.0).select(value -> value * 10.0).toArray()
        );
        assertArrayEquals(new int[]{10, 20, 30}, Linq.ofDoubles(1.0, 2.0, 3.0).selectToInt(value -> (int) value * 10).toArray());
        assertArrayEquals(new long[]{10L, 20L, 30L}, Linq.ofDoubles(1.0, 2.0, 3.0).selectToLong(value -> (long) value * 10L).toArray());
        assertEquals(
            java.util.List.of("value=1.0", "value=2.0", "value=3.0"),
            Linq.ofDoubles(1.0, 2.0, 3.0).selectToObj(value -> "value=" + value).toList()
        );
    }

    @Test
    void slicing() {
        assertArrayEquals(new double[]{3.0, 4.0}, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).skip(2).toArray());
        assertArrayEquals(new double[]{1.0, 2.0}, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).take(2).toArray());
        assertArrayEquals(new double[]{1.0, 2.0}, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).skipLast(2).toArray());
        assertArrayEquals(new double[]{3.0, 4.0}, Linq.ofDoubles(1.0, 2.0, 3.0, 4.0).takeLast(2).toArray());
        assertArrayEquals(new double[]{3.0, 2.0}, Linq.ofDoubles(1.0, 2.0, 3.0, 2.0).skipWhile(value -> value < 3.0).toArray());
        assertArrayEquals(new double[]{1.0, 2.0}, Linq.ofDoubles(1.0, 2.0, 3.0, 2.0).takeWhile(value -> value < 3.0).toArray());
    }

    @Test
    void reverse_order_and_shuffle() {
        assertArrayEquals(new double[]{3.0, 2.0, 1.0}, Linq.ofDoubles(1.0, 2.0, 3.0).reverse().toArray());

        assertArrayEquals(
            new double[]{Double.NEGATIVE_INFINITY, -0.0, 0.0, 1.0, Double.POSITIVE_INFINITY, Double.NaN},
            Linq.ofDoubles(Double.NaN, 0.0, Double.POSITIVE_INFINITY, -0.0, 1.0, Double.NEGATIVE_INFINITY).order().toArray()
        );

        assertArrayEquals(
            new double[]{Double.NaN, Double.POSITIVE_INFINITY, 1.0, 0.0, -0.0, Double.NEGATIVE_INFINITY},
            Linq.ofDoubles(Double.NaN, 0.0, Double.POSITIVE_INFINITY, -0.0, 1.0, Double.NEGATIVE_INFINITY).orderDescending().toArray()
        );

        double[] shuffled = Linq.ofDoubles(1.0, 2.0, 3.0, 4.0, 5.0).shuffle().toArray();
        Arrays.sort(shuffled);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0, 5.0}, shuffled);
    }

    @Test
    void sequenceEqual_andSetOperations_useDoubleBitEquality() {
        assertTrue(
            Linq.ofDoubles(1.0, Double.NaN, -0.0).sequenceEqual(
                Linq.ofDoubles(1.0, Double.longBitsToDouble(0x7ff0000000000001L), -0.0)
            )
        );
        assertFalse(Linq.ofDoubles(0.0).sequenceEqual(Linq.ofDoubles(-0.0)));

        assertArrayEquals(
            new double[]{Double.NaN, 0.0, -0.0},
            Linq.ofDoubles(Double.NaN, Double.longBitsToDouble(0x7ff0000000000001L), 0.0, -0.0, 0.0).distinct().toArray()
        );
        assertArrayEquals(new double[]{1.0, 3.0}, Linq.ofDoubles(1.0, 2.0, 2.0, 3.0, 4.0).except(Linq.ofDoubles(2.0, 4.0)).toArray());
        assertArrayEquals(new double[]{2.0, 3.0}, Linq.ofDoubles(1.0, 2.0, 2.0, 3.0).intersect(Linq.ofDoubles(2.0, 3.0, 4.0)).toArray());
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0}, Linq.ofDoubles(1.0, 2.0, 2.0).union(Linq.ofDoubles(2.0, 3.0, 4.0)).toArray());
    }

    @Test
    void boxed() {
        assertEquals(java.util.List.of(1.0, 2.0, 3.0), Linq.ofDoubles(1.0, 2.0, 3.0).boxed().toList());
    }

    @Test
    void executionMode_andOnClose() {
        DoubleEnumerable numbers = Linq.ofDoubles(1.0, 2.0, 3.0);
        assertTrue(numbers.isSequential());
        numbers.parallel();
        assertTrue(numbers.isParallel());
        numbers.sequential();
        assertTrue(numbers.isSequential());

        StringBuilder result = new StringBuilder();
        try (DoubleEnumerable closable = Linq.ofDoubles(1.0, 2.0, 3.0)) {
            closable.onClose(() -> result.append('A'))
                .onClose(() -> result.append('B'));
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals("AB", result.toString());
    }
}
