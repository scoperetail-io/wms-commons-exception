/*
 *  ApplicationException.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms.exceptions;

import lombok.Getter;


/**
 * Custom exception class representing application-specific runtime exceptions.
 *
 * <p>This exception extends {@code RuntimeException} and is annotated with Lombok's {@code @Getter}
 * annotation, which automatically generates getter methods for all non-static fields.
 *
 * <p>This exception includes additional fields such as {@code errorCode}, {@code message}, and {@code severity}
 * to provide more context about the exception
 */
@Getter
public class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = -4785047332398578526L;

    /**
     * Error code associated with the exception.
     */
    private final String errorCode;

    /**
     * Additional message providing context about the exception.
     */
    private final String message;

    /**
     * Severity level of the exception.
     */
    private final String severity;


    /**
     * Constructs a new {@code ApplicationException} with the specified message.
     *
     * @param message the detail message
     */
    public ApplicationException(final String message) {
        super(message);
        this.message = message;
        severity = null;
        errorCode = null;
    }


    /**
     * Constructs a new {@code ApplicationException} with the specified message and cause.
     *
     * @param message the detail message
     * @param t       the cause of the exception
     */
    public ApplicationException(final String message, final Throwable t) {
        super(message, t);
        this.message = message;
        errorCode = null;
        severity = null;
    }


    /**
     * Constructs a new {@code ApplicationException} with the specified error code, message, and cause.
     *
     * @param errorCode the error code associated with the exception
     * @param message   the detail message
     * @param t         the cause of the exception
     */
    public ApplicationException(final String errorCode, final String message, final Throwable t) {
        super(message, t);
        this.errorCode = errorCode;
        this.message = message;
        severity = null;
    }


    /**
     * Constructs a new {@code ApplicationException} with the specified error code and message.
     *
     * @param errorCode the error code associated with the exception
     * @param message   the detail message
     */
    public ApplicationException(final String errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        severity = null;
    }

    /**
     * Constructs a new {@code ApplicationException} with the specified error code, message, and severity.
     *
     * @param errorCode the error code associated with the exception
     * @param message   the detail message
     * @param severity  the severity level of the exception
     */
    public ApplicationException(final String errorCode, final String message, final String severity) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
        this.severity = severity;
    }
}
