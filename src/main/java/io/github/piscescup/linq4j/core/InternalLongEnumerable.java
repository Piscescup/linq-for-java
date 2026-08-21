package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.enumerator.LongEnumerator;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongConsumer;

/**
 * Defines the internal enumeration contract for a sequence of primitive
 * {@code long} values.
 *
 * <p>This interface provides primitive-specialized traversal without exposing
 * {@code Iterable<Long>}, thereby avoiding boxing during enumeration.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface InternalLongEnumerable {


    /**
     * Returns an enumerator over the primitive {@code long} elements
     * of this sequence.
     *
     * @return an {@link LongEnumerator}
     */
    @NotNull
    LongEnumerator enumerator();


    /**
     * Performs the specified action for each element of this sequence.
     *
     * @param action the action to perform for each element
     * @throws NullPointerException if {@code action} is {@code null}
     */
    default void forEach(@NotNull LongConsumer action) {
        try (LongEnumerator enumerator = enumerator()) {
            enumerator.forEachRemaining(action);
        }
    }
}
