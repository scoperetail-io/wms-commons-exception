/*
 *  ValidationFailedException.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms.exceptions;


/**
 * Custom exception class representing a scenario where a validation process has failed.
 *
 * <p>This exception extends {@code Exception} and is typically thrown when a validation check
 * results in a failure.
 * <p>The exception provides information about the reason for the validation failure through the specified message.
 */
public class ValidationFailedException extends Exception {

    private static final long serialVersionUID = 1511798872836281601L;


    /**
     * Constructs a new {@code ValidationFailedException} with the specified exception message.
     *
     * @param exception the detail message explaining the reason for the validation failure
     */
    public ValidationFailedException(final String exception) {
        super(exception);
    }
}
