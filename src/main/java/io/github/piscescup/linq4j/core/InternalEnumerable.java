package io.github.piscescup.linq4j.core;

import io.github.piscescup.linq4j.enumerator.Enumerator;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Defines the internal enumeration contract for a sequence of reference-type
 * elements.
 *
 * <p>{@code InternalEnumerable<T>} provides the fundamental traversal mechanism
 * used by {@link Enumerable}. Each call to {@link #enumerator()} creates an
 * {@link Enumerator} that represents an independent traversal of the sequence.
 * The returned enumerator maintains the state associated with that particular
 * enumeration, such as the current cursor position and any operation-specific
 * traversal state.</p>
 *
 * <p>This interface also integrates the library's {@link Enumerator} abstraction
 * with Java's standard {@link Iterable} API. The default implementation of
 * {@link #iterator()} simply returns the enumerator created by
 * {@link #enumerator()}, since {@code Enumerator<T>} also implements
 * {@link Iterator}.</p>
 *
 * <p>As a result, implementations of this interface can be traversed using both
 * the cursor-based enumeration API:</p>
 *
 * <pre>{@code
 * try (Enumerator<String> enumerator = enumerable.enumerator()) {
 *     while (enumerator.moveNext()) {
 *         System.out.println(enumerator.current());
 *     }
 * }
 * }</pre>
 *
 * <p>and the standard enhanced {@code for} statement:</p>
 *
 * <pre>{@code
 * for (String element : enumerable) {
 *     System.out.println(element);
 * }
 * }</pre>
 *
 * <p>The query itself and the state of an individual enumeration are separate.
 * Implementations should therefore normally create a new enumerator for every
 * call to {@link #enumerator()}, rather than reusing traversal state between
 * independent enumerations.</p>
 *
 * <p>This interface is intended for reference-type sequences. Primitive
 * specializations use corresponding internal enumeration interfaces so that
 * primitive values can be traversed without boxing.</p>
 *
 * @param <T> the type of elements in this enumerable sequence
 *
 * @author REN YuanTong
 * @since 1.0.0
 *
 * @see Enumerable
 * @see Enumerator
 * @see Iterable
 * @see Iterator
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
