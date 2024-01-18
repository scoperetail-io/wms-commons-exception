/*
 *  ApiError.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms.apierror;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import jakarta.validation.ConstraintViolation;
import lombok.Data;


/**
 * Represents an error response in a RESTful API.
 *
 * <p>This class is annotated with Lombok's {@code @Data} annotation, which automatically generates
 * getters, setters, toString, equals, and hashCode methods.
 *
 * <p>The {@code @JsonTypeInfo} annotation is used for configuring how type information is used during
 * serialization and deserialization. In this case, it is configured to include the error information
 * as a wrapper object with a custom identifier property named "error."
 *
 * <p>The {@code @JsonTypeIdResolver} annotation specifies a custom resolver, {@code LowerCaseClassNameResolver},
 * to resolve the type identifier in a case-insensitive manner.
 */

@Data
@JsonTypeInfo(
        include = JsonTypeInfo.As.WRAPPER_OBJECT,
        use = JsonTypeInfo.Id.CUSTOM,
        property = "error",
        visible = true)
@JsonTypeIdResolver(LowerCaseClassNameResolver.class)
public class ApiError {

    private HttpStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime timestamp;

    private String message;
    private String debugMessage;
    private List<ApiSubError> subErrors;

    private ApiError() {
        timestamp = LocalDateTime.now();
    }

    public ApiError(final HttpStatus status) {
        this();
        this.status = status;
    }

    public ApiError(final HttpStatus status, final Throwable ex) {
        this();
        this.status = status;
        this.message = "Unexpected error";
        this.debugMessage = ex.getLocalizedMessage();
    }

    public ApiError(final HttpStatus status, final String message, final Throwable ex) {
        this();
        this.status = status;
        this.message = message;
        this.debugMessage = ex.getLocalizedMessage();
    }

    private void addSubError(final ApiSubError subError) {
        if (subErrors == null) {
            subErrors = new ArrayList<>();
        }
        subErrors.add(subError);
    }

    private void addValidationError(
            final String object, final String field, final Object rejectedValue, final String message) {
        addSubError(new ApiValidationError(object, field, rejectedValue, message));
    }

    private void addValidationError(final String object, final String message) {
        addSubError(new ApiValidationError(object, message));
    }

    private void addValidationError(final FieldError fieldError) {
        this.addValidationError(
                fieldError.getObjectName(),
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage());
    }

    public void addValidationErrors(final List<FieldError> fieldErrors) {
        fieldErrors.forEach(this::addValidationError);
    }

    private void addValidationError(final ObjectError objectError) {
        this.addValidationError(objectError.getObjectName(), objectError.getDefaultMessage());
    }

    public void addValidationError(final List<ObjectError> globalErrors) {
        globalErrors.forEach(this::addValidationError);
    }

    /**
     * Utility method for adding error of ConstraintViolation. Usually when a @Validated validation
     * fails.
     *
     * @param cv the ConstraintViolation
     */
    private void addValidationError(final ConstraintViolation<?> cv) {
        this.addValidationError(
                cv.getRootBeanClass().getSimpleName(),
                cv.getLeafBean().toString(),
                cv.getInvalidValue(),
                cv.getMessage());
    }

    public void addValidationErrors(final Set<ConstraintViolation<?>> constraintViolations) {
        constraintViolations.forEach(this::addValidationError);
    }
}
