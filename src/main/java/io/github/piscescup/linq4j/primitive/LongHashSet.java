package io.github.piscescup.linq4j.primitive;

import java.util.Arrays;

/**
 * Lightweight package-private hash set specialized for primitive
 * {@code long} values.
 */
final class LongHashSet {

    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;
    private static final byte DELETED = 2;

    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.65f;

    private long[] values;
    private byte[] states;
    private int size;
    private int threshold;

    LongHashSet() {
        this(DEFAULT_CAPACITY);
    }

    LongHashSet(int expectedSize) {
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

        values = new long[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);
    }

    boolean add(long value) {
        if (size + 1 > threshold) {
            resize(values.length << 1);
        }

        int mask = values.length - 1;
        int index = mixToInt(value) & mask;
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

    boolean contains(long value) {
        return findIndex(value) >= 0;
    }

    boolean remove(long value) {
        int index = findIndex(value);

        if (index < 0) {
            return false;
        }

        states[index] = DELETED;
        size--;
        return true;
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    void clear() {
        Arrays.fill(states, EMPTY);
        size = 0;
    }

    private int findIndex(long value) {
        int mask = values.length - 1;
        int index = mixToInt(value) & mask;

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

    private void resize(int newCapacity) {
        int capacity = tableSizeFor(newCapacity);

        long[] oldValues = values;
        byte[] oldStates = states;

        values = new long[capacity];
        states = new byte[capacity];
        threshold = threshold(capacity);

        for (int i = 0; i < oldValues.length; i++) {
            if (oldStates[i] == OCCUPIED) {
                addRehashed(oldValues[i]);
            }
        }
    }

    private void addRehashed(long value) {
        int mask = values.length - 1;
        int index = mixToInt(value) & mask;

        while (states[index] == OCCUPIED) {
            index = (index + 1) & mask;
        }

        values[index] = value;
        states[index] = OCCUPIED;
    }

    private static int threshold(int capacity) {
        return Math.max(1, (int) (capacity * LOAD_FACTOR));
    }

    private static int tableSizeFor(int capacity) {
        if (capacity <= 1) {
            return 1;
        }

        int highest = Integer.highestOneBit(capacity - 1);

        if (highest >= (1 << 30)) {
            return 1 << 30;
        }

        return highest << 1;
    }

    private static int mixToInt(long value) {
        long z = value;
        z ^= z >>> 33;
        z *= 0xff51afd7ed558ccdL;
        z ^= z >>> 33;
        z *= 0xc4ceb9fe1a85ec53L;
        z ^= z >>> 33;

        return (int) (z ^ (z >>> 32));
    }
}
