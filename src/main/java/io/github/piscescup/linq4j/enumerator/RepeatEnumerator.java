package io.github.piscescup.linq4j.enumerator;

import io.github.piscescup.util.validation.ArgumentCheck;

import java.util.NoSuchElementException;

/**
 * An enumerator that returns the specified element a fixed number of times.
 *
 * <p>The repeated element is not copied. If the element is a reference
 * type, every position in the sequence refers to the same object.</p>
 *
 * @param <T> The type of the repeated element.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class RepeatEnumerator<T> implements Enumerator<T> {

    private final T element;

    private final int count;

    /**
     * The number of elements already returned.
     */
    private int index;

    private boolean hasCurrent;

    private boolean closed;

    /**
     * Creates an enumerator that repeats the specified element.
     *
     * @param element The element to repeat.
     * @param count The number of times to repeat the element.
     * @throws IllegalArgumentException If {@code count} is negative.
     */
    public RepeatEnumerator(T element, int count) {
        ArgumentCheck.requiresNonNegative(count);

        this.element = element;
        this.count = count;
    }

    @Override
    public boolean moveNext() {
        if (!hasNext()) {
            hasCurrent = false;
            return false;
        }

        index++;
        hasCurrent = true;

        return true;
    }

    @Override
    public T current() {
        if (closed || !hasCurrent) {
            throw new IllegalStateException(
                "The enumerator is not positioned on a valid element."
            );
        }

        return element;
    }

    @Override
    public boolean hasNext() {
        return !closed && index < count;
    }

    @Override
    public T next() {
        if (!moveNext()) {
            throw new NoSuchElementException(
                "No more elements are available."
            );
        }

        return current();
    }

    @Override
    public void reset() {
        ensureOpen();

        index = 0;
        hasCurrent = false;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        hasCurrent = false;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                "The enumerator has already been closed."
            );
        }
    }
}