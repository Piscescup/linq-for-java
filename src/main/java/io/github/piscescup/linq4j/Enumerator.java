package io.github.piscescup.linq4j;

import io.github.piscescup.util.validation.NullCheck;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;


/**
 * An enumerator over a {@link InternalEnumerable}.
 * Enumerator differ from {@code Enumerable}s in two ways:
 * <ul>
 *      <li> Enumerators allow the caller to remove elements from the
 *           underlying {@code Enumerable}s during the enumeration with well-defined
 *           semantics.
 *      <li> Method names have been improved.
 * </ul>
 * @apiNote
 * An {@link InternalEnumerable} can be converted into an {@code Enumerator} by
 * using the {@link InternalEnumerable#enumerator()} method.
 *
 * @author REN YuanTong
 * @since 1.0.0
 * @param <T> the type of elements returned by this enumerator
 */
public interface Enumerator<T> extends AutoCloseable, Iterator<T> {

    /**
     * Advances the enumerator to the next element in the sequence.
     *
     * <p>If this method returns {@code true}, the current element
     * can be accessed via {@link #current()}.
     *
     * <p>If it returns {@code false}, the enumeration has reached the end.
     *
     * @return {@code true} if the enumerator was successfully advanced
     *         to the next element; {@code false} if the end has been reached
     */
    boolean moveNext();

    /**
     * Returns the element at the current cursor position.
     *
     * <p>This method is valid only after a successful call to
     * {@link #moveNext()}.
     *
     * @return the current element
     * @throws IllegalStateException if the enumerator is positioned
     *         before the first element or after the last element
     */
    T current();

    /**
     * Returns {@code true} if the enumeration has more elements.
     *
     * <p>This method is part of the {@link Iterator} contract.
     * Implementations may internally delegate to {@link #moveNext()}.
     *
     * @return {@code true} if more elements are available
     */
    @Override
    boolean hasNext();

    /**
     * Returns the next element in the sequence.
     *
     * <p>This method advances the enumerator and returns the element.
     * It is equivalent to:
     *
     * <pre>{@code
     * if (!moveNext()) {
     *     throw new NoSuchElementException();
     * }
     * return current();
     * }</pre>
     *
     * @return the next element
     * @throws NoSuchElementException if no more elements exist
     */
    @Override
    T next();

    /**
     * Performs the given action for each remaining element
     * until all elements have been processed.
     *
     * @param action the action to be performed for each element
     * @throws NullPointerException if the specified action is {@code null}
     */
    @Override
    default void forEachRemaining(Consumer<? super T> action) {
        NullCheck.requireNonNull(action, "action");
        try (Enumerator<T> e = this) {
            while (e.moveNext()) {
                action.accept(e.current());
            }
        }
    }

    /**
     * Removes from the underlying collection the last element
     * returned by this enumerator (optional operation).
     *
     * <p>Implementations may throw {@link UnsupportedOperationException}
     * if removal is not supported.
     *
     * @throws UnsupportedOperationException if the remove operation
     *         is not supported
     * @throws IllegalStateException if the {@code next()} method has not
     *         yet been called, or the {@code remove()} method has already
     *         been called after the last call to {@code next()}
     */
    @Override
    default void remove() {
        throw new UnsupportedOperationException("Remove operation is not supported.");
    }

    /**
     * Resets the enumerator to its initial position,
     * before the first element in the sequence.
     *
     * <p>This operation is optional and may not be supported
     * by all implementations.
     *
     * @throws UnsupportedOperationException if reset is not supported
     */
    default void reset() {
        throw new UnsupportedOperationException("Reset operation is not supported.");
    }

    /**
     * Closes this enumerator and releases any underlying resources.
     *
     * <p>This method should be idempotent (safe to call multiple times).
     *
     * <p>After calling this method, further operations on the enumerator
     * may result in undefined behavior.
     *
     */
    @Override
    void close();
}
