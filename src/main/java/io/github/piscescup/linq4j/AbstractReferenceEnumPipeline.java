package io.github.piscescup.linq4j;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * <h2>Abstract Enumerable Pipeline</h2>
 *
 * <p>
 * Provides the common infrastructure for an enumerable query pipeline.
 * A pipeline consists of a source stage followed by zero or more
 * intermediate operation stages. Each stage describes how values produced
 * by its upstream stage are transformed into values exposed by the current
 * stage.
 * </p>
 *
 * <p>
 * Unlike {@link java.util.stream.Stream}, an enumerable pipeline is reusable.
 * Creating an intermediate operation does not consume or invalidate the
 * upstream pipeline, and the same pipeline may be enumerated multiple times.
 * Each call to {@link #enumerator()} creates a new enumeration chain with
 * independent execution state.
 * </p>
 *
 * <p>
 * Pipeline evaluation is deferred. Constructing a pipeline does not enumerate
 * its source. The source is accessed only when an {@link Enumerator} is
 * requested and that enumerator is subsequently advanced.
 * </p>
 *
 * <h3>Pipeline structure</h3>
 *
 * <p>
 * Each intermediate stage maintains a reference to its immediately preceding
 * stage. The source stage has no preceding stage and owns the factory used to
 * create source enumerators.
 * </p>
 *
 * <pre>{@code
 * Head<T>
 *    ^
 *    |
 * Where<T>
 *    ^
 *    |
 * Select<T, R>
 *    ^
 *    |
 * Take<R>
 * }</pre>
 *
 * <p>
 * Multiple pipelines may share the same upstream stage:
 * </p>
 *
 * <pre>{@code
 *              Head<T>
 *              /     \
 *             /       \
 *        Where<T>   Select<T, R>
 *            |
 *         Take<T>
 * }</pre>
 *
 * <p>
 * Pipeline stages describe a query and should not contain mutable state
 * associated with a particular enumeration. State such as the current
 * element, remaining element count, distinct-value sets, buffers, or nested
 * enumerators belongs to the {@link Enumerator} instances created during
 * evaluation.
 * </p>
 *
 * @param <T_IN> the type of elements accepted from the upstream pipeline stage
 * @param <T_OUT> the type of elements produced by this pipeline stage
 * @param <ENUM_OUT> type of the subclass implementing {@code BaseEnumerable}
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
abstract class AbstractReferenceEnumPipeline<T_IN, T_OUT, ENUM_OUT extends BaseEnumerable<T_OUT, ENUM_OUT>>
    extends AbstractBaseEnumPipeline<T_OUT, ENUM_OUT>
    implements BaseEnumerable<T_OUT, ENUM_OUT>, InternalEnumerable<T_OUT>
{

    /**
     * The source stage of this pipeline.
     *
     * <p>
     * For the source stage itself, this field refers to {@code this}.
     * For an intermediate stage, it refers to the source stage inherited
     * from its upstream stage.
     * </p>
     */
    @SuppressWarnings("rawtypes")
    protected final AbstractReferenceEnumPipeline sourceStage;

    /**
     * The immediately preceding stage of this pipeline.
     *
     * <p>
     * This field is {@code null} only for the source stage.
     * </p>
     */
    @SuppressWarnings("rawtypes")
    protected final @Nullable AbstractReferenceEnumPipeline previousStage;

    /**
     * Factory used to create enumerators for the source of this pipeline.
     *
     * <p>
     * This field is present only on the source stage. Intermediate stages
     * obtain their source through {@link #sourceStage}.
     * </p>
     *
     * <p>
     * The supplier is retained for the lifetime of the pipeline rather than
     * consumed after its first use. It must therefore return a new independent
     * {@link Enumerator} whenever it is invoked.
     * </p>
     */
    private @Nullable Supplier<? extends Enumerator<?>> sourceSupplier;

    /**
     * Number of intermediate stages between this stage and the source stage.
     *
     * <p>
     * The source stage has depth {@code 0}. Its immediate downstream stage has
     * depth {@code 1}, and so forth.
     * </p>
     */
    private final int depth;

    /**
     * Specifies whether this pipeline is configured for parallel evaluation.
     *
     * <p>
     * This value currently represents execution configuration only. Concrete
     * evaluation implementations may use it when parallel execution support is
     * provided.
     * </p>
     */
    private boolean parallel;

    /**
     * The action when close.
     */
    private Runnable sourceCloseAction;

    /**
     * Creates the source stage of an enumerable pipeline.
     *
     * <p>
     * The supplied factory is not invoked by this constructor. It is retained
     * and invoked when a new enumeration of the pipeline is requested.
     * </p>
     *
     * @param sourceSupplier a factory that creates a new source enumerator
     *                       for each enumeration
     * @param parallel whether the pipeline is configured for parallel
     *                 evaluation
     */
    protected AbstractReferenceEnumPipeline(
        @NotNull Supplier<? extends Enumerator<T_OUT>> sourceSupplier,
        boolean parallel
    ) {
        super(parallel);

        this.previousStage = null;
        this.sourceStage = this;
        this.sourceSupplier = Objects.requireNonNull(
            sourceSupplier,
            "sourceSupplier"
        );
        this.depth = 0;
    }


    /**
     * Creates an intermediate stage appended to the specified upstream
     * pipeline.
     *
     * <p>
     * Creating an intermediate stage does not consume, modify, or invalidate
     * the upstream stage. Consequently, multiple independent pipelines may
     * share the same upstream pipeline.
     * </p>
     *
     * @param previousStage the immediately preceding pipeline stage
     */
    protected AbstractReferenceEnumPipeline(
        @NotNull AbstractReferenceEnumPipeline<?, T_IN, ?> previousStage
    ) {
        super(previousStage);

        this.previousStage = Objects.requireNonNull(
            previousStage,
            "previousStage"
        );

        this.sourceStage = previousStage.sourceStage;
        this.sourceSupplier = null;
        this.depth = previousStage.depth + 1;

        /*
         * Execution configuration is inherited by the newly created query
         * stage. Because the pipeline is reusable, creating this stage does
         * not mutate the source or the upstream stage.
         */
        this.parallel = previousStage.parallel;
    }

    /**
     * Returns the depth of this stage in the pipeline.
     *
     * <p>
     * The source stage has depth {@code 0}.
     * </p>
     *
     * @return the number of intermediate stages between this stage and the
     *         source stage
     */
    protected final int depth() {
        return depth;
    }

    /**
     * Returns whether this stage is the source stage of its pipeline.
     *
     * @return {@code true} if this stage is the source stage; otherwise
     *         {@code false}
     */
    protected final boolean isSourceStage() {
        return previousStage == null;
    }

    /**
     * Returns whether the intermediate operation represented by this stage
     * maintains state while processing its input.
     *
     * <p>
     * Stateless operations normally transform, filter, skip, or otherwise
     * process elements without retaining information about previously
     * processed elements. Stateful operations may maintain sets, buffers,
     * ordering information, grouping information, or other state during an
     * enumeration.
     * </p>
     *
     * <p>
     * The source stage does not represent an intermediate operation and may
     * therefore reject calls to this method.
     * </p>
     *
     * @return {@code true} if this stage represents a stateful intermediate
     *         operation; otherwise {@code false}
     */
    protected abstract boolean opIsStateful();

    /**
     * Creates an enumerator representing the operation performed by this
     * pipeline stage.
     *
     * <p>
     * The supplied enumerator represents the output of the immediately
     * preceding stage. The returned enumerator must expose the output of this
     * stage while pulling elements lazily from the upstream enumerator.
     * </p>
     *
     * <p>
     * Any mutable execution state required by the operation should be stored
     * in the returned enumerator rather than in this pipeline stage.
     * </p>
     *
     * @param upstream the enumerator produced by the immediately preceding
     *                 pipeline stage
     * @return an enumerator that applies this stage to the upstream sequence
     */
    protected abstract @NotNull Enumerator<T_OUT> opWrapEnumerator(
        @NotNull Enumerator<T_IN> upstream
    );

    /**
     * Creates a new enumerator for this pipeline.
     *
     * <p>
     * A new source enumerator and a new set of intermediate enumerators are
     * created for every invocation. Enumerators returned by separate calls are
     * therefore independent and may be consumed separately.
     * </p>
     *
     * <p>
     * Requesting an enumerator constructs the execution chain but does not,
     * by itself, require the source elements to be traversed. Source elements
     * are consumed when the resulting enumerator is advanced.
     * </p>
     *
     * @return a new enumerator for this pipeline
     */
    @Override
    @SuppressWarnings("unchecked")
    public final @NotNull Enumerator<T_OUT> enumerator() {
        if (isSourceStage()) {
            return sourceEnumerator();
        }

        Enumerator<T_IN> upstream =
            (Enumerator<T_IN>) previousStage.enumerator();

        return opWrapEnumerator(upstream);
    }

    /**
     * Creates a new enumerator directly from the source stage.
     *
     * @return a newly created source enumerator
     * @throws IllegalStateException if invoked on a non-source stage
     */
    @SuppressWarnings("unchecked")
    private @NotNull Enumerator<T_OUT> sourceEnumerator() {
        if (!isSourceStage()) {
            throw new IllegalStateException(
                "Only a source stage can create a source enumerator."
            );
        }

        /*
         * sourceSupplier is guaranteed to be non-null for a source stage by
         * the source-stage constructor.
         */
        Supplier<? extends Enumerator<?>> supplier =
            Objects.requireNonNull(sourceSupplier);

        return (Enumerator<T_OUT>) Objects.requireNonNull(
            supplier.get(),
            "The source supplier returned null."
        );
    }


    @Override
    public void close() {
        Runnable closeAction = sourceStage.sourceCloseAction;
        if (closeAction != null) {
            sourceStage.sourceCloseAction = null;
            closeAction.run();
        }
    }

    static Runnable composeWithExceptions(Runnable first, Runnable second) {
        return () -> {
            try {
                first.run();
            } catch (Throwable e1) {
                try {
                    second.run();
                }
                catch (Throwable e2) {
                    try {
                        e1.addSuppressed(e2);
                    } catch (Throwable ignore) {}
                }
                throw e1;
            }
            second.run();
        };
    }

}