package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.enumerator.DoubleEnumerator;
import org.jetbrains.annotations.NotNull;

import java.util.function.DoubleConsumer;

/**
 * Defines the internal enumeration contract for a sequence of primitive
 * {@code double} values.
 *
 * <p>This interface provides primitive-specialized traversal without exposing
 * {@code Iterable<Double>}, thereby avoiding boxing during enumeration.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface InternalDoubleEnumerable {


    /**
     * Returns an enumerator over the primitive {@code double} elements
     * of this sequence.
     *
     * @return an {@link DoubleEnumerator}
     */

    @NotNull
    DoubleEnumerator enumerator();


    /**
     * Performs the specified action for each element of this sequence.
     *
     * @param action the action to perform for each element
     * @throws NullPointerException if {@code action} is {@code null}
     */
    default void forEach(@NotNull DoubleConsumer action) {
        try (DoubleEnumerator enumerator = enumerator()) {
            enumerator.forEachRemaining(action);
        }
    }
}
