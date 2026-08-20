package io.github.piscescup.linq4j.primitive;

/**
 * Lightweight package-private hash set specialized for primitive
 * {@code double} values.
 *
 * <p>Values are compared using {@link Double#doubleToLongBits(double)}
 * semantics.</p>
 */
final class DoubleHashSet {

    private final LongHashSet bits = new LongHashSet();

    boolean add(double value) {
        return bits.add(Double.doubleToLongBits(value));
    }

    boolean contains(double value) {
        return bits.contains(Double.doubleToLongBits(value));
    }

    boolean remove(double value) {
        return bits.remove(Double.doubleToLongBits(value));
    }

    int size() {
        return bits.size();
    }

    boolean isEmpty() {
        return bits.isEmpty();
    }

    void clear() {
        bits.clear();
    }
}
