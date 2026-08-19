package io.github.piscescup.linq4j;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Describes one ordering level in an {@link OrderedEnumerable}.
 *
 * <p>A criterion can either create a comparator for the fast single-level
 * sorting path or prepare a key cache used by multi-level sorting. Key
 * caching ensures that selectors used by {@code ThenBy}-style operations
 * are evaluated only once per element instead of once per comparison.</p>
 *
 * @param <T> the element type
 */
interface OrderCriterion<T> {

    /**
     * Precomputes the keys required by this criterion for the specified
     * elements.
     *
     * @param elements the elements to prepare keys for
     * @return a cache that compares elements by their original indexes
     */
    @NotNull
    OrderKeyCache prepare(@NotNull List<T> elements);

    /**
     * Creates an element comparator used by the single-level fast path.
     *
     * @return a comparator for elements
     */
    @NotNull
    Comparator<T> elementComparator();

    /**
     * Ordering criterion for reference-type keys.
     *
     * @param <T> the element type
     * @param <K> the key type
     */
    record ReferenceOrderCriterion<T, K>(@NotNull Function<? super T, ? extends K> keySelector,
                                         @NotNull Comparator<? super K> comparator, boolean descending)
        implements OrderCriterion<T> {

        @Override
        public @NotNull OrderKeyCache prepare(
            @NotNull List<T> elements
        ) {
            Object[] keys = new Object[elements.size()];

            for (int i = 0; i < elements.size(); i++) {
                keys[i] = keySelector.apply(elements.get(i));
            }

            return (left, right) -> {
                @SuppressWarnings("unchecked")
                K leftKey = (K) keys[left];

                @SuppressWarnings("unchecked")
                K rightKey = (K) keys[right];

                return descending
                    ? comparator.compare(rightKey, leftKey)
                    : comparator.compare(leftKey, rightKey);
            };
        }

        @Override
        public @NotNull Comparator<T> elementComparator() {
            if (descending) {
                return (left, right) ->
                    comparator.compare(
                        keySelector.apply(right),
                        keySelector.apply(left)
                    );
            }

            return (left, right) ->
                comparator.compare(
                    keySelector.apply(left),
                    keySelector.apply(right)
                );
        }
    }

    /**
     * Ordering criterion specialized for {@code int} keys.
     *
     * <p>The multi-level sorting path stores keys in an {@code int[]} so no
     * boxing is required during comparison.</p>
     *
     * @param <T> the element type
     */
    record IntOrderCriterion<T>(@NotNull ToIntFunction<? super T> keySelector, boolean descending)
        implements OrderCriterion<T> {

        @Override
        public @NotNull OrderKeyCache prepare(
            @NotNull List<T> elements
        ) {
            int[] keys = new int[elements.size()];

            for (int i = 0; i < keys.length; i++) {
                keys[i] = keySelector.applyAsInt(elements.get(i));
            }

            return descending
                ? (left, right) -> Integer.compare(keys[right], keys[left])
                : (left, right) -> Integer.compare(keys[left], keys[right]);
        }

        @Override
        public @NotNull Comparator<T> elementComparator() {
            return descending
                ? (left, right) -> Integer.compare(
                keySelector.applyAsInt(right),
                keySelector.applyAsInt(left)
            )
                : (left, right) -> Integer.compare(
                keySelector.applyAsInt(left),
                keySelector.applyAsInt(right)
            );
        }
    }

    /**
     * Ordering criterion specialized for {@code long} keys.
     *
     * <p>The multi-level sorting path stores keys in a {@code long[]} so no
     * boxing is required during comparison.</p>
     *
     * @param <T> the element type
     */
    record LongOrderCriterion<T>(@NotNull ToLongFunction<? super T> keySelector, boolean descending)
        implements OrderCriterion<T> {

        @Override
        public @NotNull OrderKeyCache prepare(
            @NotNull List<T> elements
        ) {
            long[] keys = new long[elements.size()];

            for (int i = 0; i < keys.length; i++) {
                keys[i] = keySelector.applyAsLong(elements.get(i));
            }

            return descending
                ? (left, right) -> Long.compare(keys[right], keys[left])
                : (left, right) -> Long.compare(keys[left], keys[right]);
        }

        @Override
        public @NotNull Comparator<T> elementComparator() {
            return descending
                ? (left, right) -> Long.compare(
                keySelector.applyAsLong(right),
                keySelector.applyAsLong(left)
            )
                : (left, right) -> Long.compare(
                keySelector.applyAsLong(left),
                keySelector.applyAsLong(right)
            );
        }
    }

    /**
     * Ordering criterion specialized for {@code double} keys.
     *
     * <p>The multi-level sorting path stores keys in a {@code double[]} so no
     * boxing is required during comparison.</p>
     *
     * @param <T> the element type
     */
    record DoubleOrderCriterion<T>(@NotNull ToDoubleFunction<? super T> keySelector, boolean descending)
        implements OrderCriterion<T> {

        @Override
        public @NotNull OrderKeyCache prepare(
            @NotNull List<T> elements
        ) {
            double[] keys = new double[elements.size()];

            for (int i = 0; i < keys.length; i++) {
                keys[i] = keySelector.applyAsDouble(elements.get(i));
            }

            return descending
                ? (left, right) -> Double.compare(keys[right], keys[left])
                : (left, right) -> Double.compare(keys[left], keys[right]);
        }

        @Override
        public @NotNull Comparator<T> elementComparator() {
            return descending
                ? (left, right) -> Double.compare(
                keySelector.applyAsDouble(right),
                keySelector.applyAsDouble(left)
            )
                : (left, right) -> Double.compare(
                keySelector.applyAsDouble(left),
                keySelector.applyAsDouble(right)
            );
        }
    }
}