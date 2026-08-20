package io.github.piscescup.linq4j.primitive;


import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;

/**
 * An {@link IntEnumerator} implementation that enumerates the elements of a
 * primitive {@code int} array.
 *
 * <p>The enumerator is initially positioned before the first element of the
 * array. A call to {@link #moveNext()} advances the enumerator to the next
 * element. The current element can then be obtained by calling
 * {@link #current()}.</p>
 *
 * <p>This implementation operates directly on the supplied {@code int[]}
 * array and therefore does not require boxing primitive values into
 * {@link Integer} objects.</p>
 *
 * <p>The supplied array is not copied. Changes made to the array after this
 * enumerator is created may therefore be visible during enumeration.</p>
 *
 * <p>This enumerator supports {@link #reset()}, but does not support
 * {@link #remove()}.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
final class IntArrayEnumerator implements IntEnumerator {

    /**
     * The array being enumerated.
     */
    private final int @NotNull [] elements;

    /**
     * The index of the current element.
     *
     * <p>A value of {@code -1} indicates that the enumerator is positioned
     * before the first element.</p>
     */
    private int index = -1;

    /**
     * Indicates whether this enumerator has been closed.
     */
    private boolean closed;

    /**
     * Creates an enumerator over the specified primitive {@code int} array.
     *
     * @param elements The array to enumerate.
     * @throws NullPointerException If {@code elements} is {@code null}.
     */
    IntArrayEnumerator(
        int @NotNull [] elements
    ) {
        this.elements = NullCheck.requireNonNull(
            elements,
            "elements"
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean moveNext() {
        ensureOpen();

        if (index + 1 >= elements.length) {
            index = elements.length;
            return false;
        }

        ++index;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int current() {
        ensureOpen();

        if (index < 0 || index >= elements.length) {
            throw new NoSuchElementException(
                "The enumerator is not positioned on an element."
            );
        }

        return elements[index];
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException Always, because array elements
     *         cannot be removed through this enumerator.
     */
    @Override
    public void remove() {
        ensureOpen();

        throw new UnsupportedOperationException(
            "Removing elements from an array enumerator is not supported."
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        ensureOpen();

        index = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        closed = true;
        index = elements.length;
    }

    /**
     * Ensures that this enumerator has not been closed.
     *
     * @throws IllegalStateException If this enumerator has already been
     *         closed.
     */
    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                "The enumerator has already been closed."
            );
        }
    }
}