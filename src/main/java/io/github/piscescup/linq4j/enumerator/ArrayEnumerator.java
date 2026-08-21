package io.github.piscescup.linq4j.enumerator;

import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An {@link Enumerator} implementation backed by an array.
 *
 * <p>This enumerator traverses the elements of the supplied array in their
 * natural index order, beginning at index {@code 0} and continuing through
 * the final element.</p>
 *
 * <p>The supplied array is retained directly and is not copied. Consequently,
 * changes made to the array while this enumerator is being consumed may be
 * observed by subsequent enumeration operations.</p>
 *
 * <p>The {@link #hasNext()} method does not advance the enumeration and may be
 * called repeatedly without consuming elements.</p>
 *
 * <p>This enumerator supports {@link #reset()}, which returns the cursor to
 * its initial position before the first element.</p>
 *
 * @param <E> the type of elements enumerated
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class ArrayEnumerator<E> implements Enumerator<E> {

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
     * The array containing the elements to enumerate.
     */
    @NotNull
    private final E[] values;

    /**
     * The index of the current element.
     *
     * <p>A value of {@code -1} indicates that enumeration has not yet started.
     * A value equal to {@code values.length} indicates that the sequence has
     * been exhausted.</p>
     */
    private int index = -1;

    /**
     * Indicates whether this enumerator has been closed.
     */
    private boolean closed;

    /**
     * Creates an enumerator over the specified array.
     *
     * <p>The supplied array is retained directly and is not copied.</p>
     *
     * @param values the array whose elements are to be enumerated
     *
     * @throws NullPointerException if {@code values} is {@code null}
     */
    public ArrayEnumerator(
        @NotNull final E[] values
    ) {
        this.values = Objects.requireNonNull(
            values,
            "values"
        );
    }

    /**
     * Advances this enumerator to the next element.
     *
     * <p>If this method returns {@code true}, {@link #current()} returns the
     * element at the new cursor position. If this method returns
     * {@code false}, the sequence has been exhausted and {@link #current()}
     * is no longer valid.</p>
     *
     * @return {@code true} if another element was available;
     *         otherwise {@code false}
     *
     * @throws IllegalStateException if this enumerator has been closed
     */
    @Override
    public boolean moveNext() {
        ensureOpen();

        int nextIndex = index + 1;

        if (nextIndex >= values.length) {
            index = values.length;
            return false;
        }

        index = nextIndex;
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

        if (index < 0 || index >= values.length) {
            throw new IllegalStateException(
                NO_CURRENT_ELEMENT
            );
        }

        return values[index];
    }

    /**
     * Returns whether another element is available.
     *
     * <p>This method does not change the current cursor position.</p>
     *
     * @return {@code true} if another element is available;
     *         otherwise {@code false}
     *
     * @throws IllegalStateException if this enumerator has been closed
     */
    @Override
    public boolean hasNext() {
        ensureOpen();

        return index + 1 < values.length;
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

        return current();
    }

    /**
     * Resets this enumerator to its initial position.
     *
     * <p>After this method returns, {@link #current()} is unavailable until
     * {@link #moveNext()} is called successfully.</p>
     *
     * @throws IllegalStateException if this enumerator has been closed
     */
    @Override
    public void reset() {
        ensureOpen();

        index = -1;
    }

    /**
     * Closes this enumerator.
     *
     * <p>This operation is idempotent. Closing this enumerator does not modify
     * or clear the backing array.</p>
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
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