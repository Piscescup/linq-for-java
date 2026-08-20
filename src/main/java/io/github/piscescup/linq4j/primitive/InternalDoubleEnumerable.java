package io.github.piscescup.linq4j.primitive;

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

    @NotNull
    DoubleEnumerator enumerator();

    default void forEach(@NotNull DoubleConsumer action) {
        try (DoubleEnumerator enumerator = enumerator()) {
            enumerator.forEachRemaining(action);
        }
    }
}
