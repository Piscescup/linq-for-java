package io.github.piscescup.linq4j.exceptions;

/**
 * Class {@code EnumerableException} is the root of the Enumerable exceptions' hierarchy.
 *
 * @author REN YuanTong
 * @since 1.0.0
 */
public class EnumerableException extends RuntimeException {
    public EnumerableException(String message) {
        super(message);
    }
}
