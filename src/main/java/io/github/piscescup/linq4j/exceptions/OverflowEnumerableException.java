package io.github.piscescup.linq4j.exceptions;

/**
 * <p>
 * An exception thrown when the number of elements in an {@code Enumerable}
 * sequence exceeds the maximum count that can be represented by the
 * corresponding counting operation.
 * </p>
 *
 * <p>
 * This exception is primarily used by counting operations such as
 * {@code count()} and {@code longCount()} when the number of elements
 * exceeds the supported range of their return type.
 * </p>
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public class OverflowEnumerableException extends EnumerableException {

    /**
     * The message template used to describe the overflow condition.
     */
    private static final String OVERFLOW_MSG_TEMPLATE =
        "The count: %s of the Enumerable sequence is overflow, the maximum is %s";

    /**
     * Constructs an {@code OverflowEnumerableException} with the specified
     * count and maximum supported count.
     *
     * @param count the actual count that caused the overflow
     * @param maximumCount the maximum count supported by the operation
     */
    public OverflowEnumerableException(int count, int maximumCount) {
        super(String.format(OVERFLOW_MSG_TEMPLATE, count, maximumCount));
    }

    /**
     * Constructs an {@code OverflowEnumerableException} with the specified
     * count and maximum supported count.
     *
     * @param count the actual count that caused the overflow
     * @param maximumCount the maximum count supported by the operation
     */
    public OverflowEnumerableException(long count, long maximumCount) {
        super(String.format(OVERFLOW_MSG_TEMPLATE, count, maximumCount));
    }

    /**
     * Constructs an {@code OverflowEnumerableException} for the specified
     * count using {@link Long#MAX_VALUE} as the maximum supported count.
     *
     * @param count the actual count that caused the overflow
     */
    public OverflowEnumerableException(int count) {
        super(String.format(OVERFLOW_MSG_TEMPLATE, count, Integer.MAX_VALUE));
    }

    /**
     * Constructs an {@code OverflowEnumerableException} for the specified
     * count using {@link Long#MAX_VALUE} as the maximum supported count.
     *
     * @param count the actual count that caused the overflow
     */
    public OverflowEnumerableException(long count) {
        super(String.format(OVERFLOW_MSG_TEMPLATE, count, Long.MAX_VALUE));
    }
}
