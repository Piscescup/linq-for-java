package io.github.piscescup.linq4j.primitive;

import io.github.piscescup.linq4j.Linq;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LongEnumerableTest {

    @Test
    void aggregateToResult() {
        assertEquals(15L, Linq.ofLongs(1L, 2L, 3L, 4L, 5L).aggregateToResult(0L, Long::sum));
    }

    @Test
    void all_any_andShortCircuit() {
        assertTrue(Linq.ofLongs(new long[0]).all(value -> value > 0L));
        assertTrue(Linq.ofLongs(2L, 4L, 6L).all(value -> value % 2L == 0L));
        assertFalse(Linq.ofLongs(2L, 3L, 6L).all(value -> value % 2L == 0L));
        assertFalse(Linq.ofLongs(new long[0]).any());
        assertTrue(Linq.ofLongs(1L).any());
        assertTrue(Linq.ofLongs(1L, 2L, 3L).any(value -> value == 2L));
        assertFalse(Linq.ofLongs(1L, 2L, 3L).any(value -> value > 10L));

        AtomicInteger calls = new AtomicInteger();
        assertTrue(Linq.ofLongs(1L, 2L, 3L, 4L).any(value -> {
            calls.incrementAndGet();
            return value == 2L;
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void append_prepend_concat_defaultIfEmpty() {
        assertArrayEquals(new long[]{1L, 2L, 3L, 4L}, Linq.ofLongs(1L, 2L, 3L).append(4L).toArray());
        assertArrayEquals(new long[]{0L, 1L, 2L, 3L}, Linq.ofLongs(1L, 2L, 3L).prepend(0L).toArray());
        assertArrayEquals(new long[]{1L, 2L, 3L, 4L}, Linq.ofLongs(1L, 2L).concat(Linq.ofLongs(3L, 4L)).toArray());
        assertArrayEquals(new long[]{1L, 2L}, Linq.ofLongs(1L, 2L).defaultIfEmpty(99L).toArray());
        assertArrayEquals(new long[]{99L}, Linq.ofLongs(new long[0]).defaultIfEmpty(99L).toArray());
    }

    @Test
    void contains_and_count() {
        assertTrue(Linq.ofLongs(1L, 2L, 3L).contains(2L));
        assertFalse(Linq.ofLongs(1L, 2L, 3L).contains(4L));
        assertEquals(4L, Linq.ofLongs(1L, 2L, 3L, 4L).count());
        assertEquals(2L, Linq.ofLongs(1L, 2L, 3L, 4L).count(value -> value % 2L == 0L));
    }

    @Test
    void elementAt_allVariants() {
        assertEquals(20L, Linq.ofLongs(10L, 20L, 30L).elementAt(1));
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.ofLongs(10L, 20L, 30L).elementAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> Linq.ofLongs(10L, 20L, 30L).elementAt(3));
        assertEquals(OptionalLong.of(20L), Linq.ofLongs(10L, 20L, 30L).elementAtOrEmpty(1));
        assertTrue(Linq.ofLongs(10L, 20L, 30L).elementAtOrEmpty(99).isEmpty());
        assertEquals(99L, Linq.ofLongs(10L, 20L, 30L).elementAtOrDefault(99, 99L));
    }

    @Test
    void first_last_single_allVariants() {
        assertEquals(1L, Linq.ofLongs(1L, 2L, 3L).first());
        assertEquals(2L, Linq.ofLongs(1L, 2L, 3L, 4L).first(value -> value % 2L == 0L));
        assertTrue(Linq.ofLongs(new long[0]).firstOrEmpty().isEmpty());
        assertEquals(OptionalLong.of(2L), Linq.ofLongs(1L, 2L, 4L).firstOrEmpty(value -> value % 2L == 0L));

        assertEquals(3L, Linq.ofLongs(1L, 2L, 3L).last());
        assertEquals(4L, Linq.ofLongs(1L, 2L, 3L, 4L).last(value -> value % 2L == 0L));
        assertThrows(NoSuchElementException.class, () -> Linq.ofLongs(new long[0]).last());
        assertEquals(OptionalLong.of(3L), Linq.ofLongs(1L, 2L, 3L).lastOrEmpty());
        assertTrue(Linq.ofLongs(new long[0]).lastOrEmpty().isEmpty());

        assertEquals(42L, Linq.ofLongs(42L).single());
        assertThrows(NoSuchElementException.class, () -> Linq.ofLongs(new long[0]).single());
        assertThrows(IllegalStateException.class, () -> Linq.ofLongs(1L, 2L).single());
        assertEquals(2L, Linq.ofLongs(1L, 2L, 3L).single(value -> value % 2L == 0L));
        assertThrows(IllegalStateException.class, () -> Linq.ofLongs(2L, 4L).single(value -> value % 2L == 0L));
        assertEquals(OptionalLong.of(42L), Linq.ofLongs(42L).singleOrEmpty());
        assertTrue(Linq.ofLongs(new long[0]).singleOrEmpty().isEmpty());
    }

    @Test
    void numericAggregations() {
        assertEquals(10L, Linq.ofLongs(1L, 2L, 3L, 4L).sum());
        assertEquals(1L, Linq.ofLongs(3L, 1L, 4L, 2L).min());
        assertEquals(4L, Linq.ofLongs(3L, 1L, 4L, 2L).max());
        assertEquals(2.5, Linq.ofLongs(1L, 2L, 3L, 4L).average(), 1e-12);
        assertEquals(0L, Linq.ofLongs(new long[0]).sum());
        assertThrows(NoSuchElementException.class, () -> Linq.ofLongs(new long[0]).min());
        assertThrows(NoSuchElementException.class, () -> Linq.ofLongs(new long[0]).max());
        assertThrows(ArithmeticException.class, () -> Linq.ofLongs(new long[0]).average());
    }

    @Test
    void where_select_andCrossProjection() {
        assertArrayEquals(
            new long[]{20L, 40L, 60L},
            Linq.ofLongs(1L, 2L, 3L, 4L, 5L, 6L).where(value -> value % 2L == 0L).select(value -> value * 10L).toArray()
        );
        assertArrayEquals(new int[]{10, 20, 30}, Linq.ofLongs(1L, 2L, 3L).selectToInt(value -> (int) value * 10).toArray());
        assertArrayEquals(new double[]{0.5, 1.0, 1.5}, Linq.ofLongs(1L, 2L, 3L).selectToDouble(value -> value / 2.0).toArray());
        assertEquals(
            java.util.List.of("value=1", "value=2", "value=3"),
            Linq.ofLongs(1L, 2L, 3L).selectToObj(value -> "value=" + value).toList()
        );
    }

    @Test
    void slicing() {
        assertArrayEquals(new long[]{3L, 4L}, Linq.ofLongs(1L, 2L, 3L, 4L).skip(2).toArray());
        assertArrayEquals(new long[]{1L, 2L}, Linq.ofLongs(1L, 2L, 3L, 4L).take(2).toArray());
        assertArrayEquals(new long[]{1L, 2L}, Linq.ofLongs(1L, 2L, 3L, 4L).skipLast(2).toArray());
        assertArrayEquals(new long[]{3L, 4L}, Linq.ofLongs(1L, 2L, 3L, 4L).takeLast(2).toArray());
        assertArrayEquals(new long[]{3L, 2L}, Linq.ofLongs(1L, 2L, 3L, 2L).skipWhile(value -> value < 3L).toArray());
        assertArrayEquals(new long[]{1L, 2L}, Linq.ofLongs(1L, 2L, 3L, 2L).takeWhile(value -> value < 3L).toArray());
    }

    @Test
    void reverse_order_and_shuffle() {
        assertArrayEquals(new long[]{3L, 2L, 1L}, Linq.ofLongs(1L, 2L, 3L).reverse().toArray());
        assertArrayEquals(new long[]{1L, 2L, 3L, 4L}, Linq.ofLongs(4L, 1L, 3L, 2L).order().toArray());
        assertArrayEquals(new long[]{4L, 3L, 2L, 1L}, Linq.ofLongs(4L, 1L, 3L, 2L).orderDescending().toArray());

        long[] shuffled = Linq.ofLongs(1L, 2L, 3L, 4L, 5L).shuffle().toArray();
        Arrays.sort(shuffled);
        assertArrayEquals(new long[]{1L, 2L, 3L, 4L, 5L}, shuffled);
    }

    @Test
    void sequenceEqual_andSetOperations() {
        assertTrue(Linq.ofLongs(1L, 2L, 3L).sequenceEqual(Linq.ofLongs(1L, 2L, 3L)));
        assertFalse(Linq.ofLongs(1L, 2L, 3L).sequenceEqual(Linq.ofLongs(1L, 3L, 2L)));
        assertArrayEquals(new long[]{1L, 2L, 3L}, Linq.ofLongs(1L, 1L, 2L, 3L, 2L).distinct().toArray());
        assertArrayEquals(new long[]{1L, 3L}, Linq.ofLongs(1L, 2L, 2L, 3L, 4L).except(Linq.ofLongs(2L, 4L)).toArray());
        assertArrayEquals(new long[]{2L, 3L}, Linq.ofLongs(1L, 2L, 2L, 3L).intersect(Linq.ofLongs(2L, 3L, 4L)).toArray());
        assertArrayEquals(new long[]{1L, 2L, 3L, 4L}, Linq.ofLongs(1L, 2L, 2L).union(Linq.ofLongs(2L, 3L, 4L)).toArray());
    }

    @Test
    void boxed() {
        assertEquals(java.util.List.of(1L, 2L, 3L), Linq.ofLongs(1L, 2L, 3L).boxed().toList());
    }

    @Test
    void executionMode_andOnClose() {
        LongEnumerable numbers = Linq.ofLongs(1L, 2L, 3L);
        assertTrue(numbers.isSequential());
        numbers.parallel();
        assertTrue(numbers.isParallel());
        numbers.sequential();
        assertTrue(numbers.isSequential());

        StringBuilder result = new StringBuilder();
        try (LongEnumerable closable = Linq.ofLongs(1L, 2L, 3L)) {
            closable .onClose(() -> result.append('A'))
                .onClose(() -> result.append('B'));
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals("AB", result.toString());
    }
}
