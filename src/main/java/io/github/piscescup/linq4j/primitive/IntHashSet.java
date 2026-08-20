package io.github.piscescup.linq4j.primitive;

import java.util.Arrays;

/**
 * A lightweight hash set specialized for primitive {@code int} values.
 *
 * <p>This class is intended for internal use by primitive enumerable
 * operations such as {@code distinct}, {@code union}, {@code intersect},
 * and {@code except}. Values are stored directly as primitive integers and
 * therefore do not require boxing into {@link Integer} objects.</p>
 *
 * <p>The implementation uses open addressing with linear probing. The table
 * is automatically resized when the number of stored values reaches the
 * configured load threshold.</p>
 *
 * <p>This class is not thread-safe.</p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
final class IntHashSet {

    /**
     * Indicates that a slot has never contained a value.
     */
    private static final byte EMPTY = 0;

    /**
     * Indicates that a slot currently contains a value.
     */
    private static final byte OCCUPIED = 1;

    /**
     * Indicates that a value was removed from a previously occupied slot.
     */
    private static final byte DELETED = 2;

    /**
     * The default initial table capacity.
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * The maximum fraction of occupied entries before the table is resized.
     */
    private static final float LOAD_FACTOR = 0.65f;

    /**
     * The primitive values stored in the hash table.
     */
    private int[] values;

    /**
     * State information corresponding to each table slot.
     */
    private byte[] states;

    /**
     * The number of values currently contained in the set.
     */
    private int size;

    /**
     * The number of values at which the table is resized.
     */
    private int threshold;

    /**
     * Creates an empty primitive integer hash set with the default initial
     * capacity.
     */
    IntHashSet() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates an empty primitive integer hash set with sufficient capacity
     * for approximately the specified number of values.
     *
     * @param expectedSize the expected number of values
     */
    IntHashSet(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException(
                "expectedSize cannot be negative: " + expectedSize
            );
        }

        int capacity = tableSizeFor(
            Math.max(
                DEFAULT_CAPACITY,
                (int) Math.ceil(expectedSize / LOAD_FACTOR)
            )
        );

        values = new int[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);
    }

    /**
     * Adds the specified primitive value to this set.
     *
     * @param value the value to add
     * @return {@code true} if the value was not already present;
     *         otherwise, {@code false}
     */
    boolean add(int value) {
        if (size + 1 > threshold) {
            resize(values.length << 1);
        }

        int mask = values.length - 1;
        int index = mix(value) & mask;
        int firstDeleted = -1;

        while (true) {
            byte state = states[index];

            if (state == EMPTY) {
                int insertionIndex =
                    firstDeleted >= 0 ? firstDeleted : index;

                values[insertionIndex] = value;
                states[insertionIndex] = OCCUPIED;
                size++;

                return true;
            }

            if (state == OCCUPIED && values[index] == value) {
                return false;
            }

            if (state == DELETED && firstDeleted < 0) {
                firstDeleted = index;
            }

            index = (index + 1) & mask;
        }
    }

    /**
     * Determines whether this set contains the specified value.
     *
     * @param value the value to locate
     * @return {@code true} if this set contains {@code value};
     *         otherwise, {@code false}
     */
    boolean contains(int value) {
        return findIndex(value) >= 0;
    }

    /**
     * Removes the specified primitive value from this set.
     *
     * @param value the value to remove
     * @return {@code true} if the value was present and removed;
     *         otherwise, {@code false}
     */
    boolean remove(int value) {
        int index = findIndex(value);

        if (index < 0) {
            return false;
        }

        states[index] = DELETED;
        size--;

        return true;
    }

    /**
     * Returns the number of values currently contained in this set.
     *
     * @return the number of values in this set
     */
    int size() {
        return size;
    }

    /**
     * Determines whether this set contains no values.
     *
     * @return {@code true} if this set is empty; otherwise, {@code false}
     */
    boolean isEmpty() {
        return size == 0;
    }

    /**
     * Removes all values from this set.
     */
    void clear() {
        Arrays.fill(states, EMPTY);
        size = 0;
    }

    /**
     * Locates the table index containing the specified value.
     *
     * @param value the value to locate
     * @return the occupied table index containing {@code value},
     *         or {@code -1} if the value is absent
     */
    private int findIndex(int value) {
        int mask = values.length - 1;
        int index = mix(value) & mask;

        while (true) {
            byte state = states[index];

            if (state == EMPTY) {
                return -1;
            }

            if (state == OCCUPIED && values[index] == value) {
                return index;
            }

            index = (index + 1) & mask;
        }
    }

    /**
     * Resizes the backing table and rehashes all currently stored values.
     *
     * @param newCapacity the requested new table capacity
     */
    private void resize(int newCapacity) {
        int capacity = tableSizeFor(newCapacity);

        int[] oldValues = values;
        byte[] oldStates = states;

        values = new int[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);

        int oldSize = size;
        size = 0;

        for (int i = 0; i < oldValues.length; i++) {
            if (oldStates[i] == OCCUPIED) {
                addRehashed(oldValues[i]);
            }
        }

        size = oldSize;
    }

    /**
     * Adds a value while rebuilding the table.
     *
     * <p>This method assumes that the value does not already occur in the
     * destination table and does not perform capacity checks.</p>
     *
     * @param value the value to add
     */
    private void addRehashed(int value) {
        int mask = values.length - 1;
        int index = mix(value) & mask;

        while (states[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }

        values[index] = value;
        states[index] = OCCUPIED;
    }

    /**
     * Computes the resize threshold for the specified table capacity.
     *
     * @param capacity the table capacity
     * @return the maximum number of entries before resizing
     */
    private static int threshold(int capacity) {
        return Math.max(
            1,
            (int) (capacity * LOAD_FACTOR)
        );
    }

    /**
     * Returns a power-of-two table capacity greater than or equal to the
     * requested capacity.
     *
     * @param capacity the requested capacity
     * @return the normalized table capacity
     */
    private static int tableSizeFor(int capacity) {
        if (capacity <= 1) {
            return 1;
        }

        int highest =
            Integer.highestOneBit(capacity - 1);

        if (highest >= (1 << 30)) {
            return 1 << 30;
        }

        return highest << 1;
    }

    /**
     * Applies a bit-mixing function to the specified integer value before
     * selecting a table bucket.
     *
     * @param value the value whose hash is mixed
     * @return the mixed hash value
     */
    private static int mix(int value) {
        int hash = value;

        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        hash *= 0x846ca68b;
        hash ^= hash >>> 16;

        return hash;
    }
}