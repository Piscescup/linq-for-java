package io.github.piscescup.linq4j;

import java.util.function.Consumer;

/**
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public class AbstractEnumerator<T> implements Enumerator<T> {

    @Override
    public boolean moveNext() {
        return false;
    }

    @Override
    public T current() {
        return null;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        return null;
    }

    @Override
    public void close() {

    }
}
