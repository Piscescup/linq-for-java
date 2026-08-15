package io.github.piscescup.linq4j;

/**
 * A sequence of elements that supports aggregate operations in a declarative
 * style, analogous to the .NET {@code IEnumerable<T>} interface and its
 * Language Integrated Query (LINQ) extensions.  The following example
 * illustrates a typical aggregate operation using an {@code Enumerable}:
 *
 * <pre>{@code
 * int sum = Linq.fromIterable(widgets)
 *     .where(w -> w.getColor() == RED)
 *     .select(w -> w.getWeight())
 *     .sum();
 * }</pre>
 *
 * In this example, {@code widgets} is a {@code Collection<Widget>}.  We obtain
 * an {@code Enumerable<Widget>} via {@code asEnumerable()} (or a similar factory
 * method), filter it to retain only the red widgets using {@code where}, and
 * then transform each remaining widget into an {@code int} representing its
 * weight via {@code select}. Finally, the terminal operation {@code sum} computes
 * the total weight of all red widgets.
 *
 * <p>An {@code Enumerable} can be viewed as a <em>query</em> over its source
 * data.  The query is expressed as a <em>pipeline</em> of operations, which
 * consists of a source (e.g., a collection, an array, a generator function, or
 * an I/O channel), zero or more <em>intermediate operations</em> (such as
 * {@code where}, {@code select}, or {@code orderBy}) that transform one
 * {@code Enumerable} into another, and a <em>terminal operation</em> (such as
 * {@code sum}, {@code count}, or {@code forEach}) that produces a result or
 * performs a side-effect.  The pipeline is <em>lazy</em>; computation on the
 * source data is deferred until the terminal operation is invoked, and elements
 * are consumed only as needed.
 *
 * <p>Implementations are allowed significant freedom in optimizing the execution
 * of the pipeline.  For example, an implementation may elide intermediate
 * operations entirely if it can prove that doing so does not affect the final
 * result.  Consequently, behavioral parameters (such as predicates or mapping
 * functions) may not be invoked in all cases, and any side-effects they might
 * have are not guaranteed to occur—unless specifically documented by the
 * terminal operation (e.g., {@code forEach} is guaranteed to execute its action
 * for each element).  For more details, see the discussion on
 * <a href="package-summary.html#SideEffects">side-effects</a>.
 *
 * <p>While collections and enumerables share some superficial similarities,
 * their purposes differ.  Collections are primarily concerned with the efficient
 * storage, management, and direct access to their elements.  In contrast, an
 * {@code Enumerable} does not provide direct access to its elements; instead,
 * it is a <em>query</em> that declaratively describes the source and the
 * operations to be performed upon it.  If the built-in operations are insufficient,
 * you may obtain a traditional iterator via {@link #iterator()} or
 * {@link #spliterator()} to perform manual traversal.
 *
 * <p>Most operations accept user-specified behavioral parameters (typically
 * lambda expressions or method references) that must be <em>non-interfering</em>
 * (they do not modify the stream source) and, in most cases, <em>stateless</em>
 * (their result must not depend on any state that might change during pipeline
 * execution).  These parameters must also be <em>non-null</em> unless otherwise
 * specified.
 *
 * <p>An {@code Enumerable} should be operated on (i.e., intermediate or terminal
 * operations invoked) only once.  Reusing the same enumerable—for example, by
 * attempting to traverse it multiple times or by "forking" it into multiple
 * pipelines—is not supported and may lead to {@link IllegalStateException} if
 * detected.  Some operations may return the same instance rather than a new
 * object, making detection of reuse not always possible, but clients should
 * avoid such patterns.
 *
 * <p>An {@code Enumerable} may implement {@link AutoCloseable} and provide a
 * {@link #close()} method.  Once closed, any further operation will throw
 * {@link IllegalStateException}.  Most enumerable instances do not require
 * explicit closing, as they are backed by in-memory collections or generating
 * functions that hold no external resources.  However, if the source is an I/O
 * channel (e.g., lines read from a file), the enumerable <em>must</em> be used
 * within a try-with-resources block or equivalent to ensure timely release of
 * resources.
 *
 * <p>Execution may be sequential or parallel; this is a property of the
 * enumerable instance.  The initial execution mode is determined by the factory
 * method used to create it (e.g., a sequential or parallel factory).  The mode
 * can be changed using {@link #sequential()} or {@link #parallel()}, and the
 * current mode can be queried with {@link #isParallel()}.
 *
 * @param <T> the type of the elements in this enumerable
 * @since 1.0.0
 * @see BaseEnumerable
 * @see <a href="https://learn.microsoft.com/en-us/dotnet/api/system.collections.ienumerable?view=net-10.0">
 *     IEnumerable in .NET</a>
 * @see <a href="https://learn.microsoft.com/en-us/dotnet/csharp/linq/get-started/introduction-to-linq-queries">
 *     Language Integrated Query (LINQ)</a>
 */
public interface Enumerable<T>
    extends BaseEnumerable<T, Enumerable<T>>
{

}
