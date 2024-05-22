/*
 *  RestExceptionHandler.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import az.it.boot.web.api.WebApiError;
import az.it.boot.web.api.WebApiErrorResponse;
import az.supplychain.wms.exceptions.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;


/**
 * Global exception handler for RESTful controllers in the application.
 *
 * <p>This class is annotated with {@code @ControllerAdvice} to indicate that it provides centralized
 * exception handling for all controllers. The {@code @Order(Ordered.HIGHEST_PRECEDENCE)} annotation
 * specifies the order in which the handler should be executed, with the highest precedence.
 *
 * <p>This class extends {@code ResponseEntityExceptionHandler}, which is a convenient base class for
 * handling exceptions and providing standardized responses in a RESTful manner.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String CORRELATION_ID_HEADER = "Correlation-Id";

    /**
     * Handle MissingServletRequestParameterException. Triggered when a 'required' request parameter
     * is missing.
     *
     * @param ex      MissingServletRequestParameterException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the WebApiError object
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            final MissingServletRequestParameterException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {
        final String error = ex.getParameterName() + " parameter is missing";
        HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
        WebApiError webApiError = buildWebApiError(error, httpStatusCode);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handle HttpMediaTypeNotSupportedException. This one triggers when JSON is invalid as well.
     *
     * @param ex      HttpMediaTypeNotSupportedException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            final HttpMediaTypeNotSupportedException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ex.getContentType());
        stringBuilder.append(" media type is not supported. Supported media types are ");
        ex.getSupportedMediaTypes().forEach(t -> stringBuilder.append(t).append(", "));
        HttpStatus httpStatusCode = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        String errorMessage = stringBuilder.substring(0, stringBuilder.length() - 2);
        WebApiError webApiError = buildWebApiError(errorMessage, httpStatusCode);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handle MethodArgumentNotValidException. Triggered when an object fails @Valid validation.
     *
     * @param ex      the MethodArgumentNotValidException that is thrown when @Valid validation fails
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param webRequest WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest webRequest) {
        String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
        HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
        final String errorMessage = "Validation error";
        List<WebApiError> webApiErrors = null;
        if (ex.getBindingResult() != null && ex.getBindingResult().getFieldErrors() != null) {
            webApiErrors = getApiErrorsFromFieldErrors(ex.getBindingResult().getFieldErrors(), correlationId);
        }
        WebApiError webApiError = buildWebApiError(errorMessage, httpStatusCode, correlationId, webApiErrors);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handles jakarta.validation.ValidationException. Thrown when @Validated fails.
     *
     * @param ex the ValidationException
     * @return the ApiError object
     */
    @ExceptionHandler(ValidationException.class)
    protected ResponseEntity<Object> handleValidationException(
            final ValidationException ex,
            final WebRequest webRequest) {
        String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
        HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
        final String error = ex.getMessage();
        WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handles javax.validation.ConstraintViolationException. Thrown when @Validated fails.
     *
     * @param ex the ConstraintViolationException
     * @return the ApiError object
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolation(
            final ConstraintViolationException ex,
            final WebRequest webRequest) {
        String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
        HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
        final String error = ex.getMessage();
        WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId, getApiErrorsFromConstraintViolations(ex.getConstraintViolations(), correlationId));
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handles EntityNotFoundException. Created to encapsulate errors with more detail than
     * EntityNotFoundException.
     *
     * @param ex the EntityNotFoundException
     * @return the ApiError object
     */
    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(final EntityNotFoundException ex) {
        HttpStatus httpStatusCode = HttpStatus.NOT_FOUND;
        final String error = ex.getMessage();
        WebApiError webApiError = buildWebApiError(error, httpStatusCode);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handle javax.persistence.EntityNotFoundException
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    protected ResponseEntity<Object> handleEntityNotFound(
            final jakarta.persistence.EntityNotFoundException ex) {
        HttpStatus httpStatusCode = HttpStatus.NOT_FOUND;
        final String error = ex.getMessage();
        WebApiError webApiError = buildWebApiError(error, httpStatusCode);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handle HttpMessageNotReadableException. Happens when request JSON is malformed.
     *
     * @param ex      HttpMessageNotReadableException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            final HttpMessageNotReadableException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {
        final ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
        final String error = "Malformed JSON request: " + ex.getMessage();
        WebApiError webApiError = buildWebApiError(error, httpStatusCode);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handle HttpMessageNotWritableException.
     *
     * @param ex      HttpMessageNotWritableException
     * @param headers HttpHeaders
     * @param status  HttpStatus
     * @param request WebRequest
     * @return the ApiError object
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(
            final HttpMessageNotWritableException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {
        HttpStatus httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        final String error = "Error writing JSON output: " + ex.getMessage();
        WebApiError webApiError = buildWebApiError(error, httpStatusCode);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handle NoHandlerFoundException.
     *
     * @param ex
     * @param headers
     * @param status
     * @param request
     * @return
     */
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            final NoHandlerFoundException ex,
            final HttpHeaders headers,
            final HttpStatusCode status,
            final WebRequest request) {
        HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
        final String error = String.format(
                "Could not find the %s method for URL %s: %s", ex.getHttpMethod(), ex.getRequestURL(), ex.getMessage());
        WebApiError webApiError = buildWebApiError(error, httpStatusCode);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handle DataIntegrityViolationException, inspects the cause for different DB causes.
     *
     * @param ex the DataIntegrityViolationException
     * @return the ApiError object
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<Object> handleDataIntegrityViolation(
            final DataIntegrityViolationException ex, final WebRequest request) {
        HttpStatus httpStatusCode = null;
        String error = null;
        if (ex.getCause() instanceof ConstraintViolationException) {
            httpStatusCode = HttpStatus.CONFLICT;
            error = "Database error: " + ex.getMessage();
        } else {
            httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
            error = "Server error: " + ex.getMessage();
        }
        WebApiError webApiError = buildWebApiError(error, httpStatusCode);
        return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Handle Exception, handle generic Exception.class
     *
     * @param ex the Exception
     * @return the ApiError object
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(
            final MethodArgumentTypeMismatchException ex, final WebRequest request) {
        Optional<Class<?>> requiredType = Optional.ofNullable(ex.getRequiredType()) ;
        String simpleName = "";
         if(requiredType.isPresent()){
             simpleName = requiredType.get().getSimpleName();
         }
         HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
         final String error = String.format(
                 "The parameter '%s' of value '%s' could not be converted to type '%s': %s",
                 ex.getName(), ex.getValue(), simpleName, ex.getMessage());
         WebApiError webApiError = buildWebApiError(error, httpStatusCode);
         return buildResponseEntity(webApiError, httpStatusCode);
    }

    /**
     * Method that obtains the error details from the Field Errors in the form of
     * collection of WebApiErrors, with the correlation id of the request.
     *
     * @param fieldErrors
     * @param correlationId
     * @return
     */
    private List<WebApiError> getApiErrorsFromFieldErrors(final List<FieldError> fieldErrors, final String correlationId) {
        List<WebApiError> webApiErrors = new ArrayList<>();
        WebApiError webApiError = null;
        HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
        String errorMessage = null;
        for (FieldError fieldError : fieldErrors) {
            errorMessage = getErrorMessageForInvalidField(
                  fieldError.getObjectName(),
                  fieldError.getField(),
                  fieldError.getRejectedValue() != null ? fieldError.getRejectedValue().toString() : "null",
                  fieldError.getDefaultMessage());
            webApiError = buildWebApiError(errorMessage.toString(), statusCode, correlationId);
            webApiErrors.add(webApiError);
        }
        return webApiErrors;
    }

    /**
     * Method that obtains the error details from the Invalid Argument Exception in the form of
     * collection of WebApiErrors, with the correlation id of the request.
     *
     * @param constraintViolations
     * @param correlationId
     * @return
     */
    private List<WebApiError> getApiErrorsFromConstraintViolations(final Set<ConstraintViolation<?>> constraintViolations, final String correlationId) {
        List<WebApiError> webApiErrors = new ArrayList<>();
        WebApiError webApiError = null;
        HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
        String errorMessage = null;
        for (ConstraintViolation<?> constraintViolation : constraintViolations) {
            errorMessage = getErrorMessageForInvalidField(
                    constraintViolation.getRootBeanClass().getSimpleName(),
                    constraintViolation.getLeafBean().toString(),
                    constraintViolation.getInvalidValue().toString(),
                    constraintViolation.getMessage());
            webApiError = buildWebApiError(errorMessage, statusCode, correlationId);
            webApiErrors.add(webApiError);
        }
        return webApiErrors;
    }

    /**
     * This method generates the error message verbiage for an invalid field error due to a constraint validation
     * error or any other error while mapping fields in the request.
     *
     * @param objectName
     * @param rejectedValue
     * @param field
     * @param defaultMessage
     * @return
     */
    private String getErrorMessageForInvalidField(
            final String objectName,
            final String field,
            final String rejectedValue,
            final String defaultMessage) {
        final StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Invalid value ");
        errorMessage.append(rejectedValue);
        errorMessage.append(" on field ");
        errorMessage.append(field);
        errorMessage.append(" for object ");
        errorMessage.append(objectName);
        errorMessage.append(": ");
        errorMessage.append(defaultMessage);
        errorMessage.append(".");
        return errorMessage.toString();
    }

    /**
     * Method that obtains generic properties for all the errors, such as timestamp and correlation id.
     *
     * @return
     */
    private Map<String, String> getGenericErrorProperties(final String correlationId) {
        Map<String, String> genericProperties = new HashMap<>();
        genericProperties.put(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC).format(dateFormatter));
        genericProperties.put(CORRELATION_ID_KEY, correlationId);
        return genericProperties;
    }

    /**
     * Bridge method that calls buildWebApiError with no correlation id or details argument
     *
     * @param errorMessage
     * @param httpStatusCode
     * @return
     */
    private WebApiError buildWebApiError(final String errorMessage, final HttpStatusCode httpStatusCode) {
        return buildWebApiError(errorMessage, httpStatusCode, null, null);
    }

    /**
     * Bridge method that calls buildWebApiError with no details argument
     *
     * @param errorMessage
     * @param httpStatusCode
     * @return
     */
    private WebApiError buildWebApiError(final String errorMessage, final HttpStatusCode httpStatusCode, final String correlationId) {
        return buildWebApiError(errorMessage, httpStatusCode, correlationId, null);
    }

    /**
     * Builds a {@code WebApiError} for the given {@code String}, {@code HttpStatusCode} and {@code Throwable}
     *
     * <p>This method is used to build an error response with details that can be added in the error response entity
     * to be returned to the client.
     *
     * @param errorMessage the {@code String} containing error message
     * @param httpStatusCode the {@code HttpStatusCode} containing the response status code
     * @param details the {@code List<WebApiError>} containing the details that caused the failure
     * @return a {@code WebApiError} containing the provided error message and HTTP status code
     */
    private WebApiError buildWebApiError(
        final String errorMessage,
        final HttpStatusCode httpStatusCode,
        final String correlationId,
        final List<WebApiError> details) {
        WebApiError.Builder builder =
                WebApiError
                .builder()
                .code(String.valueOf(httpStatusCode.value()))
                .message(errorMessage)
                .properties(getGenericErrorProperties(correlationId));
        if (!ObjectUtils.isEmpty(details)) {
            builder.details(details);
        }
        return builder.build();
    }

    /**
     * Builds a {@code ResponseEntity} for the given {@code WebApiError}.
     *
     * <p>This method is typically used in exception handling scenarios to construct a standardized
     * response entity containing error details.
     * <p>The {@code ResponseEntity} contains the provided {@code WebApiError} as the response body and
     * its corresponding HTTP status.
     *
     * @param webApiError the {@code WebApiError} containing error details
     * @param HttpStatus the {@code WebApiError} containing error details
     * @return a {@code ResponseEntity} containing the provided {@code WebApiError}
     */
    private ResponseEntity<Object> buildResponseEntity(
            final WebApiError webApiError,
            final HttpStatus httpStatus) {
        WebApiErrorResponse webApiErrorResponse = new WebApiErrorResponse(webApiError);
        return new ResponseEntity<>(webApiErrorResponse, httpStatus);
    }

}
