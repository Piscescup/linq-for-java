package io.github.piscescup.linq4j;


import io.github.piscescup.util.validation.NullCheck;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * The main class for the LINQ for Java library.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public final class Linq {
    private Linq() {}

    @SafeVarargs
    @NotNull
    public static <T> Enumerable<T> of(@NotNull T... elements) {
        NullCheck.requireAllNonNull(elements);

        return new ReferenceEnumPipeline.Head<>(() -> new ArrayEnumerator<>(elements));
    }

    @NotNull
    public static <T> Enumerable<T> of(@NotNull Collection<T> elements) {
        NullCheck.requireAllNonNull(elements);

        return new ReferenceEnumPipeline.Head<>(() -> new CollectionEnumerator<>(elements));
    }

    @NotNull
    public static <T> Enumerable<T> fromIterator(@NotNull Supplier<? extends Iterator<? extends T>> iteratorSupplier) {
        NullCheck.requireNonNull(iteratorSupplier);

        return new ReferenceEnumPipeline.Head<>(() -> new IteratorEnumerator<>(iteratorSupplier.get()));
    }

    public static <T> Enumerable<T> fromEnumerator(@NotNull Supplier<? extends Enumerator<T>> enumeratorSupplier) {
        NullCheck.requireNonNull(enumeratorSupplier);

        return new ReferenceEnumPipeline.Head<>(enumeratorSupplier);
    }
}
