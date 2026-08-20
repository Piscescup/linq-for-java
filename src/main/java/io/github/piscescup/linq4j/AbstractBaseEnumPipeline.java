package io.github.piscescup.linq4j;

import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Provides common pipeline configuration shared by enumerable pipeline
 * implementations.
 *
 * <p>This class manages pipeline-wide state that is independent of the
 * representation used to enumerate elements, such as sequential or parallel
 * execution mode and close handlers.</p>
 *
 * <p>All stages belonging to the same pipeline share a common internal
 * context. Consequently, changes made through {@link #parallel()},
 * {@link #sequential()}, or {@link #onClose(Runnable)} are visible to every
 * stage that belongs to that pipeline.</p>
 *
 * <p>This class deliberately does not define an element traversal mechanism.
 * Reference pipelines may use {@link Enumerator}, while primitive-specialized
 * pipelines may use their own primitive enumerators without introducing
 * boxing.</p>
 *
 * @param <T> the logical element type of this enumerable
 * @param <SUB_BE> the concrete enumerable type returned by pipeline
 *                 configuration operations
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public abstract class AbstractBaseEnumPipeline<T, SUB_BE extends BaseEnumerable<T, SUB_BE>>
    implements BaseEnumerable<T, SUB_BE> {

    /**
     * Shared configuration of all stages belonging to this pipeline.
     */
    private final PipelineContext context;

    /**
     * Creates a source pipeline with the specified execution mode.
     *
     * <p>A new pipeline context is created for the source stage. All
     * downstream stages created from this source should inherit this context
     * through {@link #AbstractBaseEnumPipeline(AbstractBaseEnumPipeline)}.</p>
     *
     * @param parallel whether this pipeline is configured for parallel
     *                 evaluation
     */
    protected AbstractBaseEnumPipeline(boolean parallel) {
        this.context = new PipelineContext(parallel);
    }

    /**
     * Creates a pipeline stage that shares the configuration of the specified
     * upstream pipeline.
     *
     * <p>No configuration values are copied. Instead, this stage references
     * the same pipeline context as the upstream stage. Therefore changes to
     * execution mode or close handlers are shared by all stages belonging to
     * the same pipeline.</p>
     *
     * @param upstream the upstream pipeline whose configuration is shared
     */
    protected AbstractBaseEnumPipeline(
        @NotNull AbstractBaseEnumPipeline<?, ?> upstream
    ) {
        NullCheck.requireNonNull(upstream, "upstream");

        this.context = upstream.context;
    }

    /**
     * Returns whether this pipeline is configured for parallel evaluation.
     *
     * @return {@code true} if this pipeline is configured for parallel
     *         evaluation; otherwise {@code false}
     */
    @Override
    public final boolean isParallel() {
        return context.parallel;
    }

    /**
     * Returns whether this pipeline is configured for sequential evaluation.
     *
     * @return {@code true} if this pipeline is configured for sequential
     *         evaluation; otherwise {@code false}
     */
    @Override
    public final boolean isSequential() {
        return !context.parallel;
    }

    /**
     * Configures this pipeline for parallel evaluation.
     *
     * <p>The execution mode is stored in the shared pipeline context, so the
     * change is visible through every stage belonging to the same pipeline.</p>
     *
     * @return this enumerable
     */
    @Override
    public final @NotNull SUB_BE parallel() {
        context.parallel = true;
        return self();
    }

    /**
     * Configures this pipeline for sequential evaluation.
     *
     * <p>The execution mode is stored in the shared pipeline context, so the
     * change is visible through every stage belonging to the same pipeline.</p>
     *
     * @return this enumerable
     */
    @Override
    public final @NotNull SUB_BE sequential() {
        context.parallel = false;
        return self();
    }

    /**
     * Registers an action to be invoked when this enumerable is closed.
     *
     * <p>Close handlers are associated with the shared pipeline context and
     * are invoked in registration order when {@link #close()} is called.</p>
     *
     * <p>If multiple handlers throw exceptions, the first exception is
     * propagated and subsequent exceptions are attached to it as suppressed
     * exceptions.</p>
     *
     * @param closeHandler the action to invoke when this enumerable is closed
     * @return this enumerable
     * @throws NullPointerException if {@code closeHandler} is {@code null}
     */
    @Override
    public final @NotNull SUB_BE onClose(
        @NotNull Runnable closeHandler
    ) {
        NullCheck.requireNonNull(
            closeHandler,
            "closeHandler"
        );

        Runnable[] currentHandlers =
            context.closeHandlers;

        Runnable[] newHandlers =
            Arrays.copyOf(
                currentHandlers,
                currentHandlers.length + 1
            );

        newHandlers[currentHandlers.length] =
            closeHandler;

        context.closeHandlers =
            newHandlers;

        return self();
    }

    /**
     * Closes this enumerable and executes all registered close handlers.
     *
     * <p>Close handlers are executed at most once for a shared pipeline
     * context. Calling this method again after the handlers have already been
     * executed has no effect.</p>
     *
     * <p>If one handler throws an exception, remaining handlers are still
     * executed. The first thrown exception is propagated and any additional
     * exceptions are attached as suppressed exceptions.</p>
     */
    @Override
    public void close() {
        Runnable[] handlers =
            context.closeHandlers;

        if (handlers.length == 0) {
            return;
        }

        /*
         * Clear the handlers before invoking them so that close() remains
         * idempotent even if one of the handlers throws an exception.
         */
        context.closeHandlers =
            PipelineContext.EMPTY_CLOSE_HANDLERS;

        Throwable first = null;

        for (Runnable handler : handlers) {
            try {
                handler.run();
            } catch (Throwable throwable) {
                if (first == null) {
                    first = throwable;
                } else if (throwable != first) {
                    try {
                        first.addSuppressed(throwable);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        if (first == null) {
            return;
        }

        if (first instanceof RuntimeException exception) {
            throw exception;
        }

        throw (Error) first;
    }

    /**
     * Returns this pipeline as its concrete enumerable type.
     *
     * @return this pipeline
     */
    @SuppressWarnings("unchecked")
    private @NotNull SUB_BE self() {
        return (SUB_BE) this;
    }

    /**
     * Shared mutable configuration associated with one enumerable pipeline.
     *
     * <p>Each source pipeline owns exactly one context. Intermediate stages
     * reuse the same instance rather than copying individual configuration
     * values.</p>
     */
    private static final class PipelineContext {

        /**
         * Shared immutable empty close-handler array.
         */
        private static final Runnable[] EMPTY_CLOSE_HANDLERS =
            new Runnable[0];

        /**
         * Whether the pipeline is configured for parallel evaluation.
         */
        private boolean parallel;

        /**
         * Actions invoked when the pipeline is closed.
         */
        private Runnable[] closeHandlers =
            EMPTY_CLOSE_HANDLERS;

        /**
         * Creates a pipeline context.
         *
         * @param parallel whether the pipeline is initially parallel
         */
        private PipelineContext(boolean parallel) {
            this.parallel = parallel;
        }
    }
}