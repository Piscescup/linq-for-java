package io.github.piscescup.linq4j.base;

import io.github.piscescup.util.validation.NullCheck;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * An unmodifiable implementation of the {@link Groupable} interface.
 *
 * <p>Query operations on this grouping read through to the specified list, but
 * any modification attempts—such as {@link #addElement(Object)} or
 * {@link #addElements(Collection)}—will throw an {@link UnsupportedOperationException}.
 *
 * @param <K> the type of the group key
 * @param <V> the type of the grouped elements
 * @author REN YuanTong
 * @since 1.0.0
 * @see Groupable
 * @see Grouping
 * @see Collections#unmodifiableList(List)
 */
public final class UnmodifiableGrouping<K, V> implements Groupable<K, V> {
    private static final String CANNOT_ADD_EX =
        "The add operation is unsupported in UnmodifiableGrouping.";

    private final K key;

    private final List<V> groupElements;

    /**
     * Constructs an unmodifiable grouping backed by the specified elements.
     *
     * @param key the key associated with this group
     * @param groupElements the list of elements for this group
     * @throws NullPointerException if {@code groupElements} is {@code null}
     */
    public UnmodifiableGrouping(K key, List<V> groupElements) {
        NullCheck.requireNonNull(key, "key");
        NullCheck.requireNonNull(groupElements, "groupElements");
        this.key = key;
        this.groupElements = Collections.unmodifiableList(groupElements);
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
     *
     * @return an unmodifiable {@link List} of elements belonging to this group
     */
    @Override
    public List<V> getGroupElements() {
        return groupElements;
    }

    /**
     * <b>Warning: Unsupported Operation.</b>
     *
     * @throws UnsupportedOperationException always, as this grouping is unmodifiable
     */
    @Override
    public Groupable<K, V> addElement(V element) {
        throw new UnsupportedOperationException(CANNOT_ADD_EX);
    }

    /**
     * <b>Warning: Unsupported Operation.</b>
     *
     * @throws UnsupportedOperationException always, as this grouping is unmodifiable
     */
    @Override
    public Groupable<K, V> addElements(Collection<V> elements) {
        throw new UnsupportedOperationException(CANNOT_ADD_EX);
    }
}