package io.github.piscescup.linq4j;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * Base interface for {@code Enumerable}s
 *
 * @author REN YuanTong
 * @since 1.0.0
 * @param <T> the type of the elements in this {@code Enumerable}
 * @param <SUB_BE> the type of the {@code Enumerable} implementing {@link BaseEnumerable}
 */
public interface BaseEnumerable<T, SUB_BE extends BaseEnumerable<T, SUB_BE> >
    extends InternalEnumerable<T>, AutoCloseable {

    /**
     * Returns an iterator for the elements of this enumerable.
     *
     * @return the element iterator for this enumerable
     */
    @Override
    default @NotNull Iterator<T> iterator() {
        return InternalEnumerable.super.iterator();
    }

    /**
     * Returns whether this Enumerable, if a terminal operation were to be executed,
     * would execute in parallel.  Calling this method after invoking a
     * terminal Enumerable operation method may yield unpredictable results.
     *
     * @return {@code true} if this Enumerable would execute in parallel if executed
     */
    boolean isParallel();

    /**
     * Returns an equivalent enumerable that is parallel.  May return
     * itself, either because the enumerable was already parallel, or because
     * the underlying enumerable state was modified to be parallel.
     *
     * @return a parallel enumerable
     */
    SUB_BE parallel();

    /**
     * Returns whether this enumerable, if a terminal operation were to be executed,
     * would execute in sequential.  Calling this method after invoking a
     * terminal enumerable operation method may yield unpredictable results.
     *
     * @return {@code true} if this enumerable would execute in sequential if executed
     */
    boolean isSequential();

    /**
     * Returns an equivalent enumerable that is sequential.  May return
     * itself, either because the enumerable was already sequential, or because
     * the underlying enumerable state was modified to be sequential.
     *
     * @return a sequential enumerable
     */
    SUB_BE sequential();

    /**
     * Returns an equivalent enumerable with an additional close handler.  Close
     * handlers are run when the {@link #close()} method
     * is called on the enumerable, and are executed in the order they were
     * added.  All close handlers are run, even if earlier close handlers throw
     * exceptions.  If any close handler throws an exception, the first
     * exception thrown will be relayed to the caller of {@code close()}, with
     * any remaining exceptions added to that exception as suppressed exceptions
     * (unless one of the remaining exceptions is the same exception as the
     * first exception, since an exception cannot suppress itself.)  May
     * return itself.
     *
     * @param closeHandler A task to execute when the enumerable is closed
     * @return a enumerable with a handler that is run if the enumerable is closed
     */
    SUB_BE onClose(@NotNull Runnable closeHandler);

    /**
     * Closes this enumerable, causing all close handlers for this enumerable pipeline
     * to be called.
     *
     * @see AutoCloseable#close()
     */
    @Override
    void close() throws Exception;
}
