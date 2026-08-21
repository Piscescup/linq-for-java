package io.github.piscescup.linq4j.enumerator;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * An {@link Enumerator} implementation backed by a {@link Collection}.
 *
 * <p>This enumerator traverses the elements of the supplied collection by
 * using its {@link Collection#iterator()} method. Consequently, the iteration
 * order of this enumerator is the iteration order defined by the underlying
 * collection.</p>
 *
 * <p>For example, an {@link java.util.ArrayList} preserves list order,
 * a {@link java.util.LinkedHashSet} preserves insertion order, while a
 * {@link java.util.HashSet} does not guarantee a particular iteration
 * order.</p>
 *
 * <p>The supplied collection is not copied. Changes to the collection while
 * this enumerator is being consumed are therefore subject to the behavior of
 * the collection's iterator and may result in a
 * {@link java.util.ConcurrentModificationException}.</p>
 *
 * <p>The {@link #hasNext()} method does not advance the enumeration and may be
 * called repeatedly without consuming an element.</p>
 *
 * @param <E> the type of elements enumerated
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class CollectionEnumerator<E> implements Enumerator<E> {

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
     * The collection containing the elements to enumerate.
     */
    @NotNull
    private final Collection<? extends E> values;

    /**
     * The iterator used by the current enumeration.
     */
    @NotNull
    private Iterator<? extends E> iterator;

    /**
     * The element at the current cursor position.
     */
    private E current;

    /**
     * Indicates whether {@link #current} currently contains a valid element.
     */
    private boolean hasCurrent;

    /**
     * Indicates whether this enumerator has been closed.
     */
    private boolean closed;

    /**
     * Creates an enumerator over the specified collection.
     *
     * <p>The collection itself is retained and is not copied.</p>
     *
     * @param values the collection whose elements are to be enumerated
     *
     * @throws NullPointerException if {@code values} is {@code null}
     */
    public CollectionEnumerator(
        @NotNull final Collection<? extends E> values
    ) {
        this.values = Objects.requireNonNull(
            values,
            "values"
        );

        this.iterator = values.iterator();
    }

    /**
     * Advances this enumerator to the next element.
     *
     * <p>If another element exists, this method makes that element available
     * through {@link #current()} and returns {@code true}. When the collection
     * has been exhausted, this method clears the current element and returns
     * {@code false}.</p>
     *
     * @return {@code true} if another element was available;
     *         otherwise {@code false}
     *
     * @throws IllegalStateException if this enumerator has been closed
     */
    @Override
    public boolean moveNext() {
        ensureOpen();

        if (!iterator.hasNext()) {
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
     * <p>This method does not advance the enumeration.</p>
     *
     * @return {@code true} if another element is available;
     *         otherwise {@code false}
     *
     * @throws IllegalStateException if this enumerator has been closed
     */
    @Override
    public boolean hasNext() {
        ensureOpen();

        return iterator.hasNext();
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
     * Resets this enumerator to its initial position.
     *
     * <p>A new iterator is obtained from the backing collection. After this
     * method returns, {@link #current()} is unavailable until
     * {@link #moveNext()} is called successfully.</p>
     *
     * @throws IllegalStateException if this enumerator has been closed
     */
    @Override
    public void reset() {
        ensureOpen();

        iterator = values.iterator();
        current = null;
        hasCurrent = false;
    }

    /**
     * Closes this enumerator.
     *
     * <p>This operation is idempotent. Closing this enumerator does not modify
     * or clear the backing collection.</p>
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        current = null;
        hasCurrent = false;
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