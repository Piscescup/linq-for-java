package io.github.piscescup.linq4j.enumerator;

import io.github.piscescup.util.validation.ArgumentCheck;

/**
 * An enumerator that returns the specified primitive {@code double} value
 * a fixed number of times.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class DoubleRepeatEnumerator implements DoubleEnumerator {

    private final double element;

    private final int count;

    private int index;

    private boolean hasCurrent;

    private boolean closed;

    public DoubleRepeatEnumerator(double element, int count) {
        ArgumentCheck.requiresNonNegative(count);

        this.element = element;
        this.count = count;
    }

    @Override
    public boolean moveNext() {
        if (closed || index >= count) {
            hasCurrent = false;
            return false;
        }

        index++;
        hasCurrent = true;
        return true;
    }

    @Override
    public double current() {
        ensureCurrent();
        return element;
    }

    @Override
    public void reset() {
        ensureOpen();

        index = 0;
        hasCurrent = false;
    }

    @Override
    public void close() {
        closed = true;
        hasCurrent = false;
    }

    private void ensureCurrent() {
        if (closed || !hasCurrent) {
            throw new IllegalStateException(
                "The enumerator is not positioned on a valid element."
            );
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                "The enumerator has already been closed."
            );
        }
    }
}