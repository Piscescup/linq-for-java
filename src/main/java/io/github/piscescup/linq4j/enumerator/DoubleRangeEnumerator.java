package io.github.piscescup.linq4j.enumerator;

/**
 * An enumerator that produces a fixed number of evenly spaced primitive
 * {@code double} values.
 *
 * <p>The value at index {@code i} is calculated from:</p>
 *
 * <pre>{@code
 * start + i * step
 * }</pre>
 *
 * <p>The number of generated values is determined by {@code count};
 * therefore, floating-point values are not compared against an end
 * boundary.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class DoubleRangeEnumerator implements DoubleEnumerator {

    private final double start;

    private final long count;

    private final double step;

    private long index;

    private double current;

    private boolean hasCurrent;

    private boolean closed;

    /**
     * Creates a fixed-length range of primitive {@code double} values.
     *
     * @param start The first value in the range.
     * @param count The number of values to generate.
     * @param step The difference between consecutive values.
     * @throws IllegalArgumentException If {@code start} is not finite,
     *         {@code count} is negative, or {@code step} is zero or not
     *         finite.
     */
    public DoubleRangeEnumerator(
        double start,
        long count,
        double step
    ) {
        if (!Double.isFinite(start)) {
            throw new IllegalArgumentException(
                "The start must be finite."
            );
        }

        if (count < 0L) {
            throw new IllegalArgumentException(
                "The count must be non-negative."
            );
        }

        if (!Double.isFinite(step) || step == 0.0) {
            throw new IllegalArgumentException(
                "The step must be finite and non-zero."
            );
        }

        this.start = start;
        this.count = count;
        this.step = step;
    }

    @Override
    public boolean moveNext() {
        if (closed || index >= count) {
            hasCurrent = false;
            return false;
        }

        /*
         * Calculate every value from its index instead of repeatedly
         * adding step, reducing accumulated floating-point error.
         */
        current = Math.fma((double) index, step, start);

        index++;
        hasCurrent = true;

        return true;
    }

    @Override
    public double current() {
        if (!hasCurrent || closed) {
            throw new IllegalStateException(
                "The enumerator is not positioned on a valid element."
            );
        }

        return current;
    }

    @Override
    public void reset() {
        ensureOpen();

        index = 0L;
        hasCurrent = false;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        hasCurrent = false;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                "The enumerator has already been closed."
            );
        }
    }
}