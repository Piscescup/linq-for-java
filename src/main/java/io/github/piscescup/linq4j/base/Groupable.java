package io.github.piscescup.linq4j.base;

import io.github.piscescup.linq4j.Enumerable;

import java.util.List;

/**
 * Represents a group of elements identified by a group key.
 *
 * @param <K> the type of the group key
 * @param <E> the type of the grouped elements
 * @author REN YuanTong
 * @since 1.0.0
 */
public interface Groupable<K, E> {

    /**
     * Returns the key associated with this group.
     *
     * @return the group key
     */
    K getGroupKey();

    /**
     * Returns the elements contained in this group.
     *
     * @return the elements of this group
     */
    List<E> getGroupElements();

    /**
     * Returns the elements contained in this group as an {@link Enumerable}.
     *
     * @return an enumerable view of the group elements
     */
    default Enumerable<E> getEnumerableGroupElements() {
        return Linq.of(getGroupElements());
    }
}