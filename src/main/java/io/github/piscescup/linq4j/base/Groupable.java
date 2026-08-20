package io.github.piscescup.linq4j.base;

import io.github.piscescup.linq4j.Enumerable;
import io.github.piscescup.linq4j.Linq;

import java.util.Collection;
import java.util.List;

/**
 * Represents a group of elements identified by a shared key.
 *
 * <p>A {@code Groupable} object pairs a key with a collection of elements associated
 * with that key, providing methods to query elements and support fluent element appending.
 *
 * @param <K> the type of the group key
 * @param <V> the type of the grouped elements
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface Groupable<K, V> {

    /**
     * Returns the key associated with this group.
     *
     * @return the group key
     */
    K getGroupKey();

    /**
     * Returns the elements contained in this group.
     *
     * @return a {@link List} of elements belonging to this group
     */
    List<V> getGroupElements();

    /**
     * Adds an element to this group.
     *
     * @param element the element to be added
     * @return this {@code Groupable} instance to support method chaining
     * @throws UnsupportedOperationException if the {@code addElement} operation
     *         is not supported by this grouping implementation
     */
    Groupable<K, V> addElement(V element);

    /**
     * Adds all elements in the specified collection to this group.
     *
     * @param elements the collection of elements to be added
     * @return this {@code Groupable} instance to support method chaining
     * @throws UnsupportedOperationException if the {@code addElements} operation
     *         is not supported by this grouping implementation
     * @throws NullPointerException if the specified collection is {@code null}
     */
    Groupable<K, V> addElements(Collection<V> elements);

    /**
     * Returns the elements contained in this group as an {@link Enumerable}.
     *
     * @return an {@link Enumerable} view over the group elements
     */
    default Enumerable<V> getEnumerableGroupElements() {
        return Linq.of(getGroupElements());
    }
}