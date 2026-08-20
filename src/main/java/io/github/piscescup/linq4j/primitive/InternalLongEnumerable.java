package io.github.piscescup.linq4j.primitive;

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

    @NotNull
    LongEnumerator enumerator();

    default void forEach(@NotNull LongConsumer action) {
        try (LongEnumerator enumerator = enumerator()) {
            enumerator.forEachRemaining(action);
        }
    }
}
