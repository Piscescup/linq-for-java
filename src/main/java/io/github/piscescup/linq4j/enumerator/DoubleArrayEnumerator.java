package io.github.piscescup.linq4j.enumerator;

import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

public final class DoubleArrayEnumerator implements DoubleEnumerator {

    private final double[] elements;
    private int index = -1;
    private boolean closed;

    public DoubleArrayEnumerator(double @NotNull [] elements) {
        this.elements = NullCheck.requireNonNull(elements, "elements");
    }

    @Override
    public boolean moveNext() {
        ensureOpen();

        if (index + 1 >= elements.length) {
            index = elements.length;
            return false;
        }

        index++;
        return true;
    }

    @Override
    public double current() {
        ensureOpen();

        if (index < 0 || index >= elements.length) {
            throw new IllegalStateException(
                "The enumerator is not positioned on an element."
            );
        }

        return elements[index];
    }

    @Override
    public void reset() {
        ensureOpen();
        index = -1;
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                "The enumerator has already been closed."
            );
        }
    }
}
