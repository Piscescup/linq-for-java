package io.github.piscescup.linq4j;


import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Base enumerator implementation used by reference pipeline operations.
 *
 * <p>This class bridges the cursor-based {@link Enumerator} contract and the
 * look-ahead semantics required by {@link Iterator}. A call to
 * {@link #hasNext()} may advance the operation internally, but the produced
 * element is buffered so that a subsequent call to {@link #next()} returns
 * that element without advancing the operation again.</p>
 *
 * <p>Subclasses implement {@link #moveNextCore()} to produce the next output
 * element and call {@link #setCurrent(Object)} when an element is available.</p>
 *
 * @param <T_IN> the type of elements produced by the upstream enumerator
 * @param <T_OUT> the type of elements produced by this enumerator
 */
abstract class PipelineEnumerator<T_IN, T_OUT>
    implements Enumerator<T_OUT> {

    private static final  String ENUMERATOR_CLOSED = "The enumerator has already been closed.";

    /**
     * The upstream enumerator supplying elements to this operation.
     */
    protected final Enumerator<T_IN> upstream;

    /**
     * The current output element.
     */
    private T_OUT current;

    /**
     * Indicates whether the enumerator is currently positioned on an element.
     */
    private boolean hasCurrent;

    /**
     * Indicates that {@link #hasNext()} has already advanced the operation and
     * buffered the resulting element for {@link #next()}.
     */
    private boolean buffered;

    /**
     * Indicates that the end of the sequence has been reached.
     */
    private boolean finished;

    /**
     * Indicates that this enumerator has been closed.
     */
    private boolean closed;

    /**
     * Creates a pipeline enumerator that obtains its input from the specified
     * upstream enumerator.
     *
     * @param upstream the upstream enumerator
     */
    protected PipelineEnumerator(
        @NotNull Enumerator<T_IN> upstream
    ) {
        this.upstream = Objects.requireNonNull(
            upstream,
            "upstream"
        );
    }

    /**
     * Advances the operation-specific enumeration logic.
     *
     * <p>If an element is produced, the implementation must call
     * {@link #setCurrent(Object)} before returning {@code true}.</p>
     *
     * @return {@code true} if another element was produced;
     *         otherwise {@code false}
     */
    protected abstract boolean moveNextCore();

    /**
     * Sets the element produced by the current operation.
     *
     * @param value the produced element
     */
    protected final void setCurrent(T_OUT value) {
        this.current = value;
        this.hasCurrent = true;
    }

    /**
     * Advances this enumerator to the next output element.
     *
     * @return {@code true} if the enumerator was successfully advanced;
     *         otherwise {@code false}
     */
    @Override
    public final boolean moveNext() {
        ensureOpen();

        /*
         * hasNext() may already have advanced moveNextCore().
         * In that case the buffered element becomes the current element
         * without advancing again.
         */
        if (buffered) {
            buffered = false;
            hasCurrent = true;
            return true;
        }

        if (finished) {
            hasCurrent = false;
            return false;
        }

        hasCurrent = false;

        if (moveNextCore()) {
            if (!hasCurrent) {
                throw new IllegalStateException(
                    "moveNextCore() returned true without setting a current element."
                );
            }

            return true;
        }

        finished = true;
        hasCurrent = false;
        current = null;

        return false;
    }

    /**
     * Returns the element at the current cursor position.
     *
     * @return the current element
     * @throws IllegalStateException if this enumerator is not positioned on
     *         an element
     */
    @Override
    public final T_OUT current() {
        ensureOpen();

        if (!hasCurrent) {
            throw new IllegalStateException(
                "The enumerator is not positioned on an element."
            );
        }

        return current;
    }

    /**
     * Determines whether another element is available without consuming that
     * element from the perspective of the {@link Iterator} API.
     *
     * @return {@code true} if another element is available
     */
    @Override
    public final boolean hasNext() {
        ensureOpen();

        if (buffered) {
            return true;
        }

        if (finished) {
            return false;
        }

        hasCurrent = false;

        if (!moveNextCore()) {
            finished = true;
            current = null;
            return false;
        }

        if (!hasCurrent) {
            throw new IllegalStateException(
                "moveNextCore() returned true without setting a current element."
            );
        }

        buffered = true;

        /*
         * From the Enumerator point of view, hasNext() must not expose the
         * buffered element as current().
         */
        hasCurrent = false;

        return true;
    }

    /**
     * Returns the next element according to the {@link Iterator} contract.
     *
     * @return the next element
     * @throws NoSuchElementException if no further element exists
     */
    @Override
    public final T_OUT next() {
        ensureOpen();

        if (buffered) {
            buffered = false;
            hasCurrent = true;
            return current;
        }

        if (!moveNext()) {
            throw new NoSuchElementException();
        }

        return current;
    }

    /**
     * Closes this enumerator and its upstream enumerator.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        buffered = false;
        finished = true;
        hasCurrent = false;
        current = null;

        upstream.close();
    }

    /**
     * Ensures that this enumerator has not been closed.
     *
     * @throws IllegalStateException if this enumerator has already been closed
     */
    private void ensureOpen() {
        if (closed) {

            throw new IllegalStateException(
                ENUMERATOR_CLOSED
            );
        }
    }
}