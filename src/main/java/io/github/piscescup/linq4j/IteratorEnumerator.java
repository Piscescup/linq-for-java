package io.github.piscescup.linq4j;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An {@link Enumerator} implementation backed by an {@link Iterator}.
 *
 * <p>This enumerator adapts a standard Java {@link Iterator} to the
 * cursor-based {@link Enumerator} contract. Elements are consumed from the
 * backing iterator as the enumerator advances.</p>
 *
 * <p>The backing iterator is retained directly and is not copied. Since an
 * iterator generally represents a one-time traversal, this enumerator does
 * not support {@link #reset()}.</p>
 *
 * <p>The {@link #hasNext()} method delegates to the backing iterator and does
 * not consume an element. Therefore repeated calls to {@code hasNext()} do
 * not advance the enumeration.</p>
 *
 * <p>Closing this enumerator does not close or otherwise modify the backing
 * iterator because {@link Iterator} does not define a close operation.
 * After this enumerator has been closed, further enumeration operations are
 * not permitted.</p>
 *
 * @param <E> the type of elements enumerated
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
final class IteratorEnumerator<E> implements Enumerator<E> {

    /**
     * Error message used when an operation is attempted after this enumerator
     * has been closed.
     */
    private static final String ENUMERATOR_CLOSED =
        "The enumerator has already been closed.";

    /**
     * Error message used when {@link #current()} is requested while this
     * enumerator is not positioned on an element.
     */
    private static final String NO_CURRENT_ELEMENT =
        "The enumerator is not positioned on an element.";

    /**
     * The iterator supplying elements to this enumerator.
     */
    @NotNull
    private final Iterator<? extends E> iterator;

    /**
     * The element at the current cursor position.
     */
    private E current;

    /**
     * Indicates whether {@link #current} currently represents a valid element.
     */
    private boolean hasCurrent;

    /**
     * Indicates whether this enumerator has reached the end of the backing
     * iterator.
     */
    private boolean finished;

    /**
     * Indicates whether this enumerator has been closed.
     */
    private boolean closed;

    /**
     * Creates an enumerator backed by the specified iterator.
     *
     * @param iterator the iterator supplying elements
     *
     * @throws NullPointerException if {@code iterator} is {@code null}
     */
    IteratorEnumerator(
        @NotNull final Iterator<? extends E> iterator
    ) {
        this.iterator = Objects.requireNonNull(
            iterator,
            "iterator"
        );
    }

    /**
     * Advances this enumerator to the next element.
     *
     * <p>If another element exists, the element is consumed from the backing
     * iterator and becomes available through {@link #current()}.</p>
     *
     * <p>If the backing iterator is exhausted, this method returns
     * {@code false} and {@link #current()} becomes unavailable.</p>
     *
     * @return {@code true} if another element was available;
     *         otherwise {@code false}
     *
     * @throws IllegalStateException if this enumerator has been closed
     */
    @Override
    public boolean moveNext() {
        ensureOpen();

        if (finished) {
            hasCurrent = false;
            return false;
        }

        if (!iterator.hasNext()) {
            finished = true;
            current = null;
            hasCurrent = false;

            return false;
        }

        current = iterator.next();
        hasCurrent = true;

        return true;
    }

    /**
     * Returns the element at the current cursor position.
     *
     * @return the current element
     *
     * @throws IllegalStateException if this enumerator has been closed
     * @throws IllegalStateException if this enumerator is not positioned on
     *         an element
     */
    @Override
    public E current() {
        ensureOpen();

        if (!hasCurrent) {
            throw new IllegalStateException(
                NO_CURRENT_ELEMENT
            );
        }

        return current;
    }

    /**
     * Returns whether another element is available.
     *
     * <p>This method does not advance the backing iterator and may be invoked
     * repeatedly without consuming elements.</p>
     *
     * @return {@code true} if another element is available;
     *         otherwise {@code false}
     *
     * @throws IllegalStateException if this enumerator has been closed
     */
    @Override
    public boolean hasNext() {
        ensureOpen();

        if (finished) {
            return false;
        }

        boolean hasNext = iterator.hasNext();

        if (!hasNext) {
            finished = true;
        }

        return hasNext;
    }

    /**
     * Advances this enumerator and returns the next element.
     *
     * @return the next element
     *
     * @throws NoSuchElementException if no further element exists
     * @throws IllegalStateException if this enumerator has been closed
     */
    @Override
    public E next() {
        ensureOpen();

        if (!moveNext()) {
            throw new NoSuchElementException();
        }

        return current;
    }

    /**
     * Resetting an iterator-backed enumerator is not supported.
     *
     * <p>A Java {@link Iterator} does not provide a standard mechanism for
     * returning to its initial position. A new iterator must be obtained from
     * the original source instead.</p>
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void reset() {
        throw new UnsupportedOperationException(
            "Reset operation is not supported by an iterator-backed enumerator."
        );
    }

    /**
     * Closes this enumerator.
     *
     * <p>This operation is idempotent. Closing this enumerator does not close
     * or modify the backing iterator.</p>
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        finished = true;
        hasCurrent = false;
        current = null;
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