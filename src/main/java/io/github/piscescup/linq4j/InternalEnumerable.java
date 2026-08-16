package io.github.piscescup.linq4j;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface InternalEnumerable<T> extends Iterable<T> {

    /**
     * Returns an enumerator over elements of type {@code T}.
     *
     * @return an Enumerator.
     */
    Enumerator<T> enumerator();

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     * @see enumerator()
     */
    @Override
    default @NotNull Iterator<T> iterator() {
        return enumerator();
    }

    /**
     * Performs the given action for each element of the {@code InternalEnumerable}
     * until all elements have been processed or the action throws an
     * exception.  Actions are performed in the order of enumeration, if that
     * order is specified.  Exceptions thrown by the action are relayed to the
     * caller.
     * <p>
     * The behavior of this method is unspecified if the action performs
     * side-effects that modify the underlying source of elements, unless an
     * overriding class has specified a concurrent modification policy.
     *
     * @implSpec
     * <p>The default implementation behaves as if:
     * <pre>{@code
     * try (Enumerator<T> enumerator = this.enumerator()) {
     *     while (enumerator.moveNext()) {
     *         action.accept(enumerator.current());
     *     }
     * }
     * }</pre>
     *
     * @param action The action to be performed for each element
     * @throws NullPointerException if the specified action is null
     */
    default void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        try (Enumerator<T> enumerator = this.enumerator()) {
            while (enumerator.moveNext()) {
                action.accept(enumerator.current());
            }
        }
    }


}
