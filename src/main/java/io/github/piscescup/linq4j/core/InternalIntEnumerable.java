package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.enumerator.IntEnumerator;
import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntConsumer;

/**
 * Defines the internal primitive-int enumeration contract used by
 * {@link IntEnumerable}.
 *
 * <p>This interface provides access to an {@link IntEnumerator} and
 * primitive-specialized traversal operations. Unlike
 * {@code InternalEnumerable<Integer>}, values are transferred as primitive
 * {@code int} values and therefore do not require boxing.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface InternalIntEnumerable {

    /**
     * Returns an enumerator over the primitive {@code int} elements
     * of this sequence.
     *
     * @return an {@link IntEnumerator}
     */
    @NotNull
    IntEnumerator enumerator();

    /**
     * Performs the specified action for each element of this sequence.
     *
     * @param action the action to perform for each element
     * @throws NullPointerException if {@code action} is {@code null}
     */
    default void forEach(@NotNull IntConsumer action) {
        NullCheck.requireNonNull(action, "action");

        try (IntEnumerator enumerator = enumerator()) {
            while (enumerator.moveNext()) {
                action.accept(enumerator.current());
            }
        }
    }
}