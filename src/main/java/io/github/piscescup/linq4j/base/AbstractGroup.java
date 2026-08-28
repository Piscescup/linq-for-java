package io.github.piscescup.linq4j.base;

import io.github.piscescup.util.validation.NullCheck;

import java.util.Collection;
import java.util.List;

/**
 * The base class of the {@link Groupable}
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public abstract class AbstractGroup<K, V> implements Groupable<K, V> {
    protected final K key;

    protected final List<V> groupElements;

    /**
     * Constructs a {@code Grouping} with the specified key and backing element list.
     *
     * @param key the key associated with this group
     * @param groupElements the backing list used to store elements
     */
    public AbstractGroup(K key, List<V> groupElements) {
        NullCheck.requireNonNull(key, "key");
        NullCheck.requireNonNull(groupElements, "groupElements");
        this.key = key;
        this.groupElements = groupElements;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public K getGroupKey() {
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<V> getGroupElements() {
        return groupElements;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Groupable<K, V> addElement(V element);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Groupable<K, V> addElements(Collection<V> elements);

    /**
     * <p>
     * Returns a string representation of the {@link Groupable}.
     * </p>
     * Below is the default format:
     * <pre>{@code
     * <key> - [elements...]
     * }</pre>
     *
     * @return a string representation of the {@link Groupable}
     */
    @Override
    public String toString() {
        return "<" + key.toString() + ">" + groupElements.toString();
    }
}
