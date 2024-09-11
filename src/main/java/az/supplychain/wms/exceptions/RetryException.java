/*
 *  RetryException.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms.exceptions;


/**
 * Custom exception class representing a scenario where a retry operation encounters an exception.
 *
 * <p>This exception extends {@code RuntimeException} and is typically thrown when a retry operation
 * fails after multiple attempts.
 *
 * <p>The exception provides information about the reason for the retry failure through the specified message
 * and the original cause (Throwable) that led to the exception.
 */
public class RetryException extends RuntimeException {

    private static final long serialVersionUID = -7916703682615442612L;


    /**
     * Constructs a new {@code RetryException} with the specified message and cause.
     *
     * @param message the detail message explaining the reason for the retry failure
     * @param th      the original cause (Throwable) that led to the retry exception
     */
    public RetryException(final String message, final Throwable th) {
        super(message, th);
    }
}
