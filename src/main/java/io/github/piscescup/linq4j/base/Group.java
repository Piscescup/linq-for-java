package io.github.piscescup.linq4j.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A standard mutable implementation of the {@link Groupable} interface.
 *
 * <p>This class encapsulates a key and a backing {@link List} of elements, allowing
 * new items to be dynamically appended via {@link #addElement(Object)} and
 * {@link #addElements(Collection)}.
 *
 * @param <K> the type of the group key
 * @param <V> the type of the grouped elements
 * @author REN YuanTong
 * @since 1.0.0
 * @see Groupable
 * @see UnmodifiableGrouping
 */
public final class Grouping<K, V>
    extends AbstractGrouping<K, V>
    implements Groupable<K, V> {

    /**
     * Constructs a {@code Grouping} with the specified key and backing element list.
     *
     * @param key the key associated with this group
     * @param groupElements the backing list used to store elements
     */
    public Grouping(K key, List<V> groupElements) {
        super(key, groupElements);
    }

    /**
     * {@inheritDoc}
     *
     * @param element the element to be added
     * @return this {@code Grouping} instance
     */
    @Override
    public Groupable<K, V> addElement(V element) {
        groupElements.add(element);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @param elements the collection of elements to be added
     * @return this {@code Grouping} instance
     * @throws NullPointerException if the specified collection is {@code null}
     */
    @Override
    public Groupable<K, V> addElements(Collection<V> elements) {
        groupElements.addAll(elements);
        return this;
    }

}
