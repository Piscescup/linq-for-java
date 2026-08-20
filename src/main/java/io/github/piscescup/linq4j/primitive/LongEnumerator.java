package io.github.piscescup.linq4j.primitive;

import io.github.piscescup.linq4j.Enumerator;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongConsumer;

/**
 * An enumerator over a sequence of primitive {@code long} values.
 *
 * <p>{@code LongEnumerator} is the primitive specialization of
 * {@link Enumerator} for {@code long} values. It provides cursor-based
 * enumeration semantics through {@link #moveNext()} and {@link #current()}
 * while avoiding boxing each element into a {@link Long}.</p>
 *
 * <p>An enumerator is initially positioned before the first element.
 * Each successful call to {@link #moveNext()} advances the cursor to the
 * next element, which can then be obtained through {@link #current()}.
 * When {@code moveNext()} returns {@code false}, the end of the sequence
 * has been reached.</p>
 *
 * <p>Unlike {@code Enumerator<Long>}, this interface exposes values directly
 * as primitive {@code long}s. It also deliberately does not extend
 * {@code Iterator<Long>}, because the {@link java.util.Iterator} contract
 * would require values to be represented as {@link Long} objects and
 * therefore introduce boxing during traversal.</p>
 *
 * <p>Implementations may optionally support operations such as
 * {@link #remove()} and {@link #reset()}. Unless otherwise documented,
 * these operations are not required to be supported.</p>
 *
 * @apiNote
 * An {@link InternalLongEnumerable} can be converted into a
 * {@code LongEnumerator} by using the
 * {@link InternalLongEnumerable#enumerator()} method.
 *
 * <p>If interoperability with APIs requiring boxed values is needed,
 * callers should explicitly convert the corresponding
 * {@link LongEnumerable} to a reference-type enumerable rather than relying
 * on implicit boxing during primitive enumeration.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface LongEnumerator extends AutoCloseable {

    /**
     * Advances this enumerator to the next element in the sequence.
     *
     * <p>If this method returns {@code true}, the enumerator is positioned
     * on a valid element and that value can be accessed through
     * {@link #current()}.</p>
     *
     * <p>If this method returns {@code false}, the end of the sequence has
     * been reached and {@link #current()} is no longer valid.</p>
     *
     * @return {@code true} if the enumerator was successfully advanced to
     *         the next element; {@code false} if the end of the sequence
     *         has been reached
     */
    boolean moveNext();

    /**
     * Returns the primitive {@code long} value at the current cursor position.
     *
     * <p>This method is valid only while the enumerator is positioned on an
     * element, normally after a successful call to {@link #moveNext()}.</p>
     *
     * @return the current primitive {@code long} value
     * @throws IllegalStateException if the enumerator is positioned before
     *         the first element or after the end of the sequence
     */
    long current();

    /**
     * Performs the specified action for each remaining element until all
     * elements have been processed.
     *
     * <p>Elements are supplied directly as primitive {@code long} values and
     * therefore do not require boxing.</p>
     *
     * @param action the action to be performed for each remaining element
     * @throws NullPointerException if {@code action} is {@code null}
     */
    default void forEachRemaining(@NotNull LongConsumer action) {
        NullCheck.requireNonNull(action, "action");

        while (moveNext()) {
            action.accept(current());
        }
    }

    /**
     * Removes from the underlying source the element at the current cursor
     * position, if removal is supported.
     *
     * <p>This is an optional operation. Implementations that do not support
     * removal may throw {@link UnsupportedOperationException}.</p>
     *
     * @throws UnsupportedOperationException if removal is not supported
     * @throws IllegalStateException if the enumerator is not positioned on
     *         an element that can be removed, or if the current element has
     *         already been removed
     */
    default void remove() {
        throw new UnsupportedOperationException(
            "Remove operation is not supported."
        );
    }

    /**
     * Resets this enumerator to its initial position, before the first
     * element in the sequence.
     *
     * <p>This is an optional operation and may not be supported by all
     * implementations.</p>
     *
     * @throws UnsupportedOperationException if reset is not supported
     */
    default void reset() {
        throw new UnsupportedOperationException(
            "Reset operation is not supported."
        );
    }

    /**
     * Closes this enumerator and releases any resources associated with the
     * current enumeration.
     *
     * <p>This method should be idempotent, so calling it multiple times has
     * the same effect as calling it once.</p>
     *
     * <p>Unless an implementation documents otherwise, the enumerator should
     * not be used after it has been closed.</p>
     */
    @Override
    void close();
}