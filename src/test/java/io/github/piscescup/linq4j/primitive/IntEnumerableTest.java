package io.github.piscescup.linq4j.primitive;

import io.github.piscescup.linq4j.core.IntEnumerable;
import io.github.piscescup.linq4j.Linq;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class IntEnumerableTest {

    @Test
    void aggregateToResult() {
        assertEquals(15, Linq.ofInts(1, 2, 3, 4, 5).aggregateToResult(0, Integer::sum));
    }

    @Test
    void all_any_andShortCircuit() {
        assertTrue(Linq.ofInts(new int[0]).all(value -> value > 0));
        assertTrue(Linq.ofInts(2, 4, 6).all(value -> value % 2 == 0));
        assertFalse(Linq.ofInts(2, 3, 6).all(value -> value % 2 == 0));
        assertFalse(Linq.ofInts(new int[0]).any());
        assertTrue(Linq.ofInts(1).any());
        assertTrue(Linq.ofInts(1, 2, 3).any(value -> value == 2));
        assertFalse(Linq.ofInts(1, 2, 3).any(value -> value > 10));

        AtomicInteger calls = new AtomicInteger();
        assertTrue(Linq.ofInts(1, 2, 3, 4).any(value -> {
            calls.incrementAndGet();
            return value == 2;
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void append_prepend_concat_defaultIfEmpty() {
        assertArrayEquals(new int[]{1, 2, 3, 4}, Linq.ofInts(1, 2, 3).append(4).toArray());
        assertArrayEquals(new int[]{0, 1, 2, 3}, Linq.ofInts(1, 2, 3).prepend(0).toArray());
        assertArrayEquals(new int[]{1, 2, 3, 4}, Linq.ofInts(1, 2).concat(Linq.ofInts(3, 4)).toArray());
        assertArrayEquals(new int[]{1, 2}, Linq.ofInts(1, 2).defaultIfEmpty(99).toArray());
        assertArrayEquals(new int[]{99}, Linq.ofInts(new int[0]).defaultIfEmpty(99).toArray());
    }

    @Test
    void contains_and_count() {
        assertTrue(Linq.ofInts(1, 2, 3).contains(2));
        assertFalse(Linq.ofInts(1, 2, 3).contains(4));
        assertEquals(4L, Linq.ofInts(1, 2, 3, 4).count());
        assertEquals(2L, Linq.ofInts(1, 2, 3, 4).count(value -> value % 2 == 0));
        assertEquals(0L, Linq.ofInts(new int[0]).count());
    }

    @Test
    void elementAt_allVariants() {
        assertEquals(20, Linq.ofInts(10, 20, 30).elementAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.ofInts(10, 20, 30).elementAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.ofInts(10, 20, 30).elementAt(3));
        assertEquals(OptionalInt.of(20), Linq.ofInts(10, 20, 30).elementAtOrEmpty(1));
        assertTrue(Linq.ofInts(10, 20, 30).elementAtOrEmpty(99).isEmpty());
        assertEquals(99, Linq.ofInts(10, 20, 30).elementAtOrDefault(99, 99));
    }

    @Test
    void first_allVariants() {
        assertEquals(1, Linq.ofInts(1, 2, 3).first());
        assertEquals(2, Linq.ofInts(1, 2, 3, 4).first(value -> value % 2 == 0));
        assertThrows(NoSuchElementException.class, () -> Linq.ofInts(new int[0]).first());
        assertThrows(NoSuchElementException.class, () -> Linq.ofInts(1, 3, 5).first(value -> value % 2 == 0));
        assertEquals(OptionalInt.of(1), Linq.ofInts(1, 2, 3).firstOrEmpty());
        assertTrue(Linq.ofInts(new int[0]).firstOrEmpty().isEmpty());
        assertEquals(OptionalInt.of(2), Linq.ofInts(1, 2, 4).firstOrEmpty(value -> value % 2 == 0));
        assertTrue(Linq.ofInts(1, 3, 5).firstOrEmpty(value -> value % 2 == 0).isEmpty());
    }

    @Test
    void last_allVariants() {
        assertEquals(3, Linq.ofInts(1, 2, 3).last());
        assertEquals(4, Linq.ofInts(1, 2, 3, 4).last(value -> value % 2 == 0));
        assertThrows(NoSuchElementException.class, () -> Linq.ofInts(new int[0]).last());
        assertThrows(NoSuchElementException.class, () -> Linq.ofInts(1, 3, 5).last(value -> value % 2 == 0));
        assertEquals(OptionalInt.of(3), Linq.ofInts(1, 2, 3).lastOrEmpty());
        assertTrue(Linq.ofInts(new int[0]).lastOrEmpty().isEmpty());
        assertEquals(OptionalInt.of(4), Linq.ofInts(1, 2, 3, 4).lastOrEmpty(value -> value % 2 == 0));
        assertTrue(Linq.ofInts(1, 3, 5).lastOrEmpty(value -> value % 2 == 0).isEmpty());
    }

    @Test
    void single_allVariants() {
        assertEquals(42, Linq.ofInts(42).single());
        assertThrows(NoSuchElementException.class, () -> Linq.ofInts(new int[0]).single());
        assertThrows(IllegalStateException.class, () -> Linq.ofInts(1, 2).single());
        assertEquals(2, Linq.ofInts(1, 2, 3).single(value -> value % 2 == 0));
        assertThrows(NoSuchElementException.class, () -> Linq.ofInts(1, 3, 5).single(value -> value % 2 == 0));
        assertThrows(IllegalStateException.class, () -> Linq.ofInts(2, 4).single(value -> value % 2 == 0));

        assertEquals(OptionalInt.of(42), Linq.ofInts(42).singleOrEmpty());
        assertTrue(Linq.ofInts(new int[0]).singleOrEmpty().isEmpty());
        assertThrows(IllegalStateException.class, () -> Linq.ofInts(1, 2).singleOrEmpty());
        assertEquals(OptionalInt.of(2), Linq.ofInts(1, 2, 3).singleOrEmpty(value -> value % 2 == 0));
        assertTrue(Linq.ofInts(1, 3, 5).singleOrEmpty(value -> value % 2 == 0).isEmpty());
        assertThrows(IllegalStateException.class, () -> Linq.ofInts(2, 4).singleOrEmpty(value -> value % 2 == 0));
    }

    @Test
    void numericAggregations() {
        assertEquals(10, Linq.ofInts(1, 2, 3, 4).sum());
        assertEquals(1, Linq.ofInts(3, 1, 4, 2).min());
        assertEquals(4, Linq.ofInts(3, 1, 4, 2).max());
        assertEquals(2.5, Linq.ofInts(1, 2, 3, 4).average(), 1e-12);
        assertEquals(0, Linq.ofInts(new int[0]).sum());
        assertThrows(NoSuchElementException.class, () -> Linq.ofInts(new int[0]).min());
        assertThrows(NoSuchElementException.class, () -> Linq.ofInts(new int[0]).max());
        assertThrows(ArithmeticException.class, () -> Linq.ofInts(new int[0]).average());
    }

    @Test
    void where_select_andCrossProjection() {
        assertArrayEquals(
            new int[]{20, 40, 60},
            Linq.ofInts(1, 2, 3, 4, 5, 6).where(value -> value % 2 == 0).select(value -> value * 10).toArray()
        );
        assertArrayEquals(new long[]{10L, 20L, 30L}, Linq.ofInts(1, 2, 3).selectToLong(value -> value * 10L).toArray());
        assertArrayEquals(new double[]{0.5, 1.0, 1.5}, Linq.ofInts(1, 2, 3).selectToDouble(value -> value / 2.0).toArray());
        assertEquals(
            java.util.List.of("value=1", "value=2", "value=3"),
            Linq.ofInts(1, 2, 3).selectToObj(value -> "value=" + value).toList()
        );
    }

    @Test
    void slicing() {
        assertArrayEquals(new int[]{3, 4}, Linq.ofInts(1, 2, 3, 4).skip(2).toArray());
        assertArrayEquals(new int[]{1, 2}, Linq.ofInts(1, 2, 3, 4).take(2).toArray());
        assertArrayEquals(new int[]{1, 2}, Linq.ofInts(1, 2, 3, 4).skipLast(2).toArray());
        assertArrayEquals(new int[]{3, 4}, Linq.ofInts(1, 2, 3, 4).takeLast(2).toArray());
        assertArrayEquals(new int[]{3, 2}, Linq.ofInts(1, 2, 3, 2).skipWhile(value -> value < 3).toArray());
        assertArrayEquals(new int[]{1, 2}, Linq.ofInts(1, 2, 3, 2).takeWhile(value -> value < 3).toArray());
    }

    @Test
    void reverse_order_and_shuffle() {
        assertArrayEquals(new int[]{3, 2, 1}, Linq.ofInts(1, 2, 3).reverse().toArray());
        assertArrayEquals(new int[]{1, 2, 3, 4}, Linq.ofInts(4, 1, 3, 2).order().toArray());
        assertArrayEquals(new int[]{4, 3, 2, 1}, Linq.ofInts(4, 1, 3, 2).orderDescending().toArray());

        int[] shuffled = Linq.ofInts(1, 2, 3, 4, 5).shuffle().toArray();
        Arrays.sort(shuffled);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, shuffled);
    }

    @Test
    void sequenceEqual_andSetOperations() {
        assertTrue(Linq.ofInts(1, 2, 3).sequenceEqual(Linq.ofInts(1, 2, 3)));
        assertFalse(Linq.ofInts(1, 2, 3).sequenceEqual(Linq.ofInts(1, 3, 2)));
        assertFalse(Linq.ofInts(1, 2, 3).sequenceEqual(Linq.ofInts(1, 2)));

        assertArrayEquals(new int[]{1, 2, 3}, Linq.ofInts(1, 1, 2, 3, 2).distinct().toArray());
        assertArrayEquals(new int[]{1, 3}, Linq.ofInts(1, 2, 2, 3, 4).except(Linq.ofInts(2, 4)).toArray());
        assertArrayEquals(new int[]{2, 3}, Linq.ofInts(1, 2, 2, 3).intersect(Linq.ofInts(2, 3, 4)).toArray());
        assertArrayEquals(new int[]{1, 2, 3, 4}, Linq.ofInts(1, 2, 2).union(Linq.ofInts(2, 3, 4)).toArray());
    }

    @Test
    void boxed() {
        assertEquals(java.util.List.of(1, 2, 3), Linq.ofInts(1, 2, 3).boxed().toList());
    }

    @Test
    void executionMode_andOnClose() {
        IntEnumerable numbers = Linq.ofInts(1, 2, 3);
        assertTrue(numbers.isSequential());
        assertFalse(numbers.isParallel());
        numbers.parallel();
        assertTrue(numbers.isParallel());
        numbers.sequential();
        assertTrue(numbers.isSequential());

        StringBuilder result = new StringBuilder();
        try (IntEnumerable closable = Linq.ofInts(1, 2, 3)) {
            closable.onClose(() -> result.append('A'))
                .onClose(() -> result.append('B'));
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals("AB", result.toString());
    }
}
