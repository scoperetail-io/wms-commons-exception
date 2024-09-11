/*
 *  ApiValidationError.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms.apierror;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Represents a validation error in a RESTful API, typically used within the context of error responses.
 *
 * <p>This class extends {@code ApiSubError} and is annotated with Lombok's {@code @Data} annotation, which
 * automatically generates getters, setters, toString, equals, and hashCode methods.
 *
 * <p>The {@code @EqualsAndHashCode} annotation is used to generate the equals and hashCode methods, excluding
 * the superclass (callSuper = false).
 *
 * <p>The {@code @AllArgsConstructor} annotation generates a constructor with parameters for all fields in the class.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class ApiValidationError implements ApiSubError {
    private String object;
    private String field;
    private Object rejectedValue;
    private String message;

    /**
     * Constructs a new {@code ApiValidationError} with the specified object and error message.
     *
     * @param object  the object associated with the validation error
     * @param message the error message describing the validation error
     */
    ApiValidationError(final String object, final String message) {
        this.object = object;
        this.message = message;
    }
}
