package io.github.piscescup.linq4j.enumerator;

/**
 * An enumerator that produces a range of primitive {@code long} values.
 *
 * <p>The range starts at {@code startInclusive} and repeatedly adds
 * {@code step} until {@code endExclusive} is reached. The end value is
 * never included.</p>
 *
 * <p>A positive step produces an ascending range, while a negative step
 * produces a descending range. If the direction of the step cannot reach
 * the specified end, this enumerator represents an empty sequence.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class LongRangeEnumerator implements LongEnumerator {

    private final long startInclusive;

    private final long endExclusive;

    private final long step;

    private long next;

    private long current;

    private boolean hasCurrent;

    private boolean exhausted;

    private boolean closed;

    /**
     * Creates an ascending range with a step of {@code 1}.
     *
     * @param startInclusive The first value in the range.
     * @param endExclusive The exclusive upper bound.
     */
    public LongRangeEnumerator(
        long startInclusive,
        long endExclusive
    ) {
        this(startInclusive, endExclusive, 1L);
    }

    /**
     * Creates a range of primitive {@code long} values.
     *
     * @param startInclusive The first value in the range.
     * @param endExclusive The exclusive end bound.
     * @param step The difference between consecutive values.
     * @throws IllegalArgumentException If {@code step} is zero.
     */
    public LongRangeEnumerator(
        long startInclusive,
        long endExclusive,
        long step
    ) {
        if (step == 0L) {
            throw new IllegalArgumentException(
                "The step must not be zero."
            );
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
            exhausted = true;
            hasCurrent = false;
            return false;
        }

        current = next;
        hasCurrent = true;

        try {
            next = Math.addExact(next, step);
        } catch (ArithmeticException exception) {
            /*
             * The current value is still valid. The next moveNext()
             * invocation will finish the enumeration.
             */
            exhausted = true;
        }

        return true;
    }

    @Override
    public long current() {
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
        return step > 0L
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