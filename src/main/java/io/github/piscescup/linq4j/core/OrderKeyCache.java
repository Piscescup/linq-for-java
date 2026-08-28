package io.github.piscescup.linq4j.core;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/**
 * Compares two source elements by their indexes in the buffered sequence.
 *
 * <p>Implementations normally read from precomputed key arrays.</p>
 */
@FunctionalInterface
interface OrderKeyCache {

    /**
     * Compares two elements by their original indexes.
     *
     * @param leftIndex the index of the left element
     * @param rightIndex the index of the right element
     * @return a negative value, zero, or a positive value as the left
     *         element is less than, equal to, or greater than the right
     *         element
     */
    int compare(int leftIndex, int rightIndex);


    /**
     * Key cache used when the primary ordering was created from an arbitrary
     * element comparator through {@link Comparator#order(Comparator)}.
     *
     * <p>An arbitrary comparator has no extractable key that can be cached, so
     * the primary comparison still delegates to that comparator. Secondary
     * {@code ThenBy} criteria can nevertheless use precomputed keys.</p>
     *
     * @param <T> the element type
     */
    record ElementOrderKeyCache<T>(@NotNull List<T> elements, @NotNull Comparator<? super T> comparator)
        implements OrderKeyCache {

        @Override
        public int compare(int leftIndex, int rightIndex) {
            return comparator.compare(
                elements.get(leftIndex),
                elements.get(rightIndex)
            );
        }
    }

}