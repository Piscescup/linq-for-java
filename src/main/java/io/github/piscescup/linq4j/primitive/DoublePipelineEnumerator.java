package io.github.piscescup.linq4j.primitive;

import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

/**
 * Base implementation for primitive {@code double} pipeline enumerators.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
abstract class DoublePipelineEnumerator implements DoubleEnumerator {

    protected final DoubleEnumerator upstream;

    private double current;
    private boolean hasCurrent;
    private boolean finished;
    private boolean closed;

    protected DoublePipelineEnumerator(@NotNull DoubleEnumerator upstream) {
        this.upstream = NullCheck.requireNonNull(upstream, "upstream");
    }

    protected abstract boolean moveNextCore();

    protected final void setCurrent(double value) {
        current = value;
        hasCurrent = true;
    }

    @Override
    public final boolean moveNext() {
        ensureOpen();

        if (finished) {
            hasCurrent = false;
            return false;
        }

        hasCurrent = false;

        if (moveNextCore()) {
            if (!hasCurrent) {
                throw new IllegalStateException(
                    "moveNextCore() returned true without setting the current value."
                );
            }

            return true;
        }

        finished = true;
        return false;
    }

    @Override
    public final double current() {
        ensureOpen();

        if (!hasCurrent) {
            throw new IllegalStateException(
                "The enumerator is not positioned on an element."
            );
        }

        return current;
    }

    @Override
    public void remove() {
        ensureOpen();
        upstream.remove();
    }

    @Override
    public void reset() {
        ensureOpen();
        upstream.reset();

        current = 0.0;
        hasCurrent = false;
        finished = false;

        resetCore();
    }

    protected void resetCore() {
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        current = 0.0;
        hasCurrent = false;
        finished = true;

        upstream.close();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                "The enumerator has already been closed."
            );
        }
    }
}
