package io.github.piscescup.linq4j.primitive;

import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

/**
 * Base implementation for primitive {@code int} pipeline enumerators.
 *
 * <p>This class wraps an upstream {@link IntEnumerator} and provides
 * common cursor-state management for primitive {@code int} pipeline
 * operations.</p>
 *
 * <p>Subclasses implement {@link #moveNextCore()} to define how the next
 * output value is produced. When an output value is available, subclasses
 * must call {@link #setCurrent(int)} before returning {@code true}.</p>
 *
 * <p>The current value is stored directly as a primitive {@code int}, so no
 * boxing into {@link Integer} is required.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
abstract class IntPipelineEnumerator implements IntEnumerator {

    /**
     * The enumerator representing the output of the immediately preceding
     * pipeline stage.
     */
    protected final IntEnumerator upstream;

    /**
     * The current primitive value.
     */
    private int current;

    /**
     * Whether the enumerator is currently positioned on a valid element.
     */
    private boolean hasCurrent;

    /**
     * Whether this enumerator has reached the end of the sequence.
     */
    private boolean finished;

    /**
     * Whether this enumerator has been closed.
     */
    private boolean closed;

    /**
     * Creates a pipeline enumerator wrapping the specified upstream
     * enumerator.
     *
     * @param upstream the upstream primitive enumerator
     */
    protected IntPipelineEnumerator(
        @NotNull IntEnumerator upstream
    ) {
        this.upstream = NullCheck.requireNonNull(
            upstream,
            "upstream"
        );
    }

    /**
     * Advances this pipeline operation to its next output value.
     *
     * <p>If this method returns {@code true}, the implementation must call
     * {@link #setCurrent(int)} before returning.</p>
     *
     * @return {@code true} if another output value is available;
     *         otherwise {@code false}
     */
    protected abstract boolean moveNextCore();

    /**
     * Sets the current primitive value produced by this pipeline operation.
     *
     * <p>This method should normally be called from
     * {@link #moveNextCore()} immediately before returning {@code true}.</p>
     *
     * @param value the current primitive {@code int} value
     */
    protected final void setCurrent(int value) {
        this.current = value;
        this.hasCurrent = true;
    }

    /**
     * Advances this enumerator to the next output element.
     *
     * @return {@code true} if another element is available;
     *         otherwise {@code false}
     */
    @Override
    public final boolean moveNext() {
        ensureOpen();

        if (finished) {
            hasCurrent = false;
            return false;
        }

        /*
         * The previous current value is no longer valid once a new
         * advancement starts.
         */
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
        hasCurrent = false;

        return false;
    }

    /**
     * Returns the primitive value at the current cursor position.
     *
     * @return the current primitive {@code int} value
     * @throws IllegalStateException if this enumerator is not positioned on
     *         a valid element
     */
    @Override
    public final int current() {
        ensureOpen();

        if (!hasCurrent) {
            throw new IllegalStateException(
                "The enumerator is not positioned on an element."
            );
        }

        return current;
    }

    /**
     * Removes the current element from the underlying source if the upstream
     * enumerator supports removal.
     */
    @Override
    public void remove() {
        ensureOpen();
        upstream.remove();
    }

    /**
     * Resets this enumerator and its upstream enumerator to their initial
     * positions.
     *
     * <p>If the upstream enumerator does not support resetting, this method
     * propagates its {@link UnsupportedOperationException}.</p>
     */
    @Override
    public void reset() {
        ensureOpen();

        upstream.reset();

        current = 0;
        hasCurrent = false;
        finished = false;

        resetCore();
    }

    /**
     * Resets operation-specific traversal state.
     *
     * <p>Subclasses that maintain additional mutable state may override this
     * method. The default implementation does nothing.</p>
     */
    protected void resetCore() {
    }

    /**
     * Closes this enumerator and its upstream enumerator.
     *
     * <p>This method is idempotent.</p>
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        current = 0;
        hasCurrent = false;
        finished = true;

        upstream.close();
    }

    /**
     * Ensures that this enumerator has not been closed.
     *
     * @throws IllegalStateException if this enumerator has already been
     *         closed
     */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                "The enumerator has already been closed."
            );
        }
    }
}