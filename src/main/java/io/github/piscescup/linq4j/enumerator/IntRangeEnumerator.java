package io.github.piscescup.linq4j.enumerator;


/**
 * An enumerator that produces a range of primitive {@code int} values.
 *
 * <p>The range starts at {@code startInclusive} and advances by
 * {@code step} until {@code endExclusive} is reached. The end value
 * itself is never included.</p>
 *
 * <p>A positive step produces an ascending range, while a negative step
 * produces a descending range. If the step direction cannot reach the
 * specified end, this enumerator represents an empty sequence.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class IntRangeEnumerator implements IntEnumerator {

    private final int startInclusive;

    private final int endExclusive;

    private final int step;

    /*
     * Use long internally to prevent overflow when advancing near
     * Integer.MIN_VALUE or Integer.MAX_VALUE.
     */
    private long next;

    private int current;

    private boolean hasCurrent;

    private boolean exhausted;

    private boolean closed;

    /**
     * Creates an ascending range enumerator with a step of {@code 1}.
     *
     * @param startInclusive the first value in the range
     * @param endExclusive the exclusive upper bound
     */
    public IntRangeEnumerator(int startInclusive, int endExclusive) {
        this(startInclusive, endExclusive, 1);
    }

    /**
     * Creates a range enumerator.
     *
     * @param startInclusive the first value in the range
     * @param endExclusive the exclusive end bound
     * @param step the amount added after each element
     * @throws IllegalArgumentException if {@code step} is zero
     */
    public IntRangeEnumerator(
        int startInclusive,
        int endExclusive,
        int step
    ) {
        if (step == 0) {
            throw new IllegalArgumentException("step must not be zero");
        }

        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
        this.step = step;
        this.next = startInclusive;
    }

    @Override
    public boolean moveNext() {
        if (closed || exhausted) {
            hasCurrent = false;
            return false;
        }

        if (!isWithinRange(next)) {
            hasCurrent = false;
            exhausted = true;
            return false;
        }

        current = (int) next;
        next += step;
        hasCurrent = true;

        return true;
    }

    @Override
    public int current() {
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

        next = startInclusive;
        hasCurrent = false;
        exhausted = false;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        exhausted = true;
        hasCurrent = false;
    }

    private boolean isWithinRange(long value) {
        return step > 0
            ? value < endExclusive
            : value > endExclusive;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                "The enumerator has already been closed."
            );
        }
    }
}