/*
 *  RestExceptionHandler.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms;

import az.it.boot.web.api.WebApiError;
import az.it.boot.web.api.WebApiErrorResponse;
import az.supplychain.wms.exceptions.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler for RESTful controllers in the application.
 *
 * <p>This class is annotated with {@code @ControllerAdvice} to indicate that it provides
 * centralized exception handling for all controllers. The
 * {@code @Order(Ordered.HIGHEST_PRECEDENCE)} annotation specifies the order in which the handler
 * should be executed, with the highest precedence.
 *
 * <p>This class extends {@code ResponseEntityExceptionHandler}, which is a convenient base class
 * for handling exceptions and providing standardized responses in a RESTful manner.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class RestApiExceptionHandler extends ResponseEntityExceptionHandler
    implements AuthenticationEntryPoint, AccessDeniedHandler {

  public static final String TIMESTAMP_KEY = "timestamp";
  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String CORRELATION_ID_HEADER = "Correlation-Id";
  static final DateTimeFormatter dateFormatter =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Handles the MissingServletRequestParameterException that occurs when a required request
   * parameter is missing.
   *
   * <p>This method is invoked when a required parameter is missing from the request. It builds an
   * appropriate error response with the missing parameter name and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Extracts the name of the missing parameter from the
   *       MissingServletRequestParameterException.
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Constructs an error message indicating the missing parameter.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, and correlation ID.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the MissingServletRequestParameterException that triggered the exception handling
   * @param headers the HttpHeaders of the request
   * @param status the HttpStatus of the response
   * @param request the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      final MissingServletRequestParameterException ex,
      final HttpHeaders headers,
      final HttpStatusCode status,
      final WebRequest request) {
    final String error = ex.getParameterName() + " parameter is missing";
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
    WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the HttpMediaTypeNotSupportedException that occurs when an unsupported media type is
   * requested.
   *
   * <p>This method is invoked when the requested media type is not supported by the application. It
   * builds an appropriate error response with the unsupported media type and the list of supported
   * media types.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Extracts the unsupported media type from the HttpMediaTypeNotSupportedException.
   *   <li>Retrieves the list of supported media types from the exception.
   *   <li>Constructs an error message indicating the unsupported media type and the list of
   *       supported media types.
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, and correlation ID.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the HttpMediaTypeNotSupportedException that triggered the exception handling
   * @param headers the HttpHeaders of the request
   * @param status the HttpStatus of the response
   * @param request the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
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
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
    String errorMessage = stringBuilder.substring(0, stringBuilder.length() - 2);
    WebApiError webApiError = buildWebApiError(errorMessage, httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the MethodArgumentNotValidException that occurs when method argument fails validation.
   *
   * <p>This method is invoked when a method argument fails validation. It builds an appropriate
   * error response with the validation error details and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to BAD_REQUEST.
   *   <li>Defines a generic error message for validation failure.
   *   <li>Checks if the exception contains field errors.
   *   <li>If field errors exist, builds a list of WebApiError objects from the field errors.
   *   <li>Builds a WebApiError object with the generic error message, HTTP status code, correlation
   *       ID, and field error details.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the MethodArgumentNotValidException that triggered the exception handling
   * @param headers the HttpHeaders of the request
   * @param status the HttpStatus of the response
   * @param webRequest the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
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
      webApiErrors =
          getApiErrorsFromFieldErrors(ex.getBindingResult().getFieldErrors(), correlationId);
    }
    WebApiError webApiError =
        buildWebApiError(errorMessage, httpStatusCode, correlationId, webApiErrors);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the ValidationException that occurs when validation fails.
   *
   * <p>This method is invoked when a validation exception is thrown, indicating that validation has
   * failed. It builds an appropriate error response with the validation error message and returns
   * it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to BAD_REQUEST.
   *   <li>Extracts the error message from the ValidationException.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, and correlation ID.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the ValidationException that triggered the exception handling
   * @param webRequest the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @ExceptionHandler(ValidationException.class)
  protected ResponseEntity<Object> handleValidationException(
      final ValidationException ex, final WebRequest webRequest) {
    String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
    WebApiError webApiError =
        buildWebApiError(
            sanitizeErrorMessage(ex.getMessage()), httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the ConstraintViolationException that occurs when constraint validation fails.
   *
   * <p>This method is invoked when a constraint violation exception is thrown, indicating that one
   * or more constraints have been violated. It builds an appropriate error response with the
   * constraint violation details and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to BAD_REQUEST.
   *   <li>Extracts the error message from the ConstraintViolationException.
   *   <li>Builds a list of WebApiError objects from the constraint violations.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, correlation ID, and
   *       constraint violation details.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the ConstraintViolationException that triggered the exception handling
   * @param webRequest the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @ExceptionHandler(ConstraintViolationException.class)
  protected ResponseEntity<Object> handleConstraintViolation(
      final ConstraintViolationException ex, final WebRequest webRequest) {
    String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
    WebApiError webApiError =
        buildWebApiError(
            sanitizeErrorMessage(ex.getMessage()),
            httpStatusCode,
            correlationId,
            getApiErrorsFromConstraintViolations(ex.getConstraintViolations(), correlationId));
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the EntityNotFoundException that occurs when an entity is not found.
   *
   * <p>This method is invoked when an EntityNotFoundException is thrown, indicating that a
   * requested entity could not be found. It builds an appropriate error response with the entity
   * not found error message and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to NOT_FOUND.
   *   <li>Extracts the error message from the EntityNotFoundException.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, and correlation ID.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the EntityNotFoundException that triggered the exception handling
   * @param webRequest the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @ExceptionHandler(EntityNotFoundException.class)
  protected ResponseEntity<Object> handleEntityNotFound(
      final EntityNotFoundException ex, final WebRequest webRequest) {
    String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.NOT_FOUND;
    WebApiError webApiError =
        buildWebApiError(
            sanitizeErrorMessage(ex.getMessage()), httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the jakarta.persistence.EntityNotFoundException that occurs when an entity is not
   * found.
   *
   * <p>This method is invoked when a jakarta.persistence.EntityNotFoundException is thrown,
   * indicating that a requested entity could not be found. It builds an appropriate error response
   * with the entity not found error message and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to NOT_FOUND.
   *   <li>Extracts the error message from the jakarta.persistence.EntityNotFoundException.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, and correlation ID.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the jakarta.persistence.EntityNotFoundException that triggered the exception handling
   * @param webRequest the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
  protected ResponseEntity<Object> handleEntityNotFound(
      final jakarta.persistence.EntityNotFoundException ex, final WebRequest webRequest) {
    String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.NOT_FOUND;
    WebApiError webApiError =
        buildWebApiError(
            sanitizeErrorMessage(ex.getMessage()), httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the HttpMessageNotReadableException that occurs when the request message is not
   * readable.
   *
   * <p>This method is invoked when an HttpMessageNotReadableException is thrown, indicating that
   * the request message is not readable. It builds an appropriate error response with the error
   * message and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to BAD_REQUEST.
   *   <li>Extracts the error message from the HttpMessageNotReadableException.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, and correlation ID.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the HttpMessageNotReadableException that triggered the exception handling
   * @param headers the HttpHeaders of the request
   * @param status the HttpStatus of the response
   * @param webRequest the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      final HttpMessageNotReadableException ex,
      final HttpHeaders headers,
      final HttpStatusCode status,
      final WebRequest webRequest) {
    String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
    final String error = "Malformed JSON request: " + sanitizeErrorMessage(ex.getMessage());
    WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the HttpMessageNotWritableException that occurs when the response message is not
   * writable.
   *
   * <p>This method is invoked when an HttpMessageNotWritableException is thrown, indicating that
   * the response message cannot be written. It builds an appropriate error response with the error
   * message and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to INTERNAL_SERVER_ERROR.
   *   <li>Extracts the error message from the HttpMessageNotWritableException.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, and correlation ID.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the HttpMessageNotWritableException that triggered the exception handling
   * @param headers the HttpHeaders of the request
   * @param status the HttpStatus of the response
   * @param webRequest the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotWritable(
      final HttpMessageNotWritableException ex,
      final HttpHeaders headers,
      final HttpStatusCode status,
      final WebRequest webRequest) {
    String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
    final String error = "Error writing JSON output: " + sanitizeErrorMessage(ex.getMessage());
    WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the NoHandlerFoundException that occurs when no handler is found for the requested URL.
   *
   * <p>This method is invoked when a NoHandlerFoundException is thrown, indicating that no handler
   * could be found to handle the requested URL. It builds an appropriate error response with the
   * error message and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to NOT_FOUND.
   *   <li>Extracts the requested URL from the HttpServletRequest.
   *   <li>Constructs an error message indicating that no handler could be found for the requested
   *       URL.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, and correlation ID.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the NoHandlerFoundException that triggered the exception handling
   * @param headers the HttpHeaders of the request
   * @param status the HttpStatus of the response
   * @param request the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(
      final NoHandlerFoundException ex,
      final HttpHeaders headers,
      final HttpStatusCode status,
      final WebRequest request) {
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
    final String error =
        String.format(
            "Could not find the %s method for URL %s: %s",
            ex.getHttpMethod(), ex.getRequestURL(), sanitizeErrorMessage(ex.getMessage()));
    WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the DataIntegrityViolationException that occurs when there is a data integrity
   * violation.
   *
   * <p>This method is invoked when a DataIntegrityViolationException is thrown, indicating that a
   * data integrity violation has occurred. It builds an appropriate error response with the error
   * message and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to CONFLICT.
   *   <li>Extracts the error message from the DataIntegrityViolationException.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, and correlation ID.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the DataIntegrityViolationException that triggered the exception handling
   * @param webRequest the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  protected ResponseEntity<Object> handleDataIntegrityViolation(
      final DataIntegrityViolationException ex, final WebRequest webRequest) {
    String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = null;
    String error = null;
    if (ex.getCause() instanceof ConstraintViolationException) {
      httpStatusCode = HttpStatus.CONFLICT;
      error = "Database error: " + sanitizeErrorMessage(ex.getMessage());
    } else {
      httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR;
      error = "Server error: " + sanitizeErrorMessage(ex.getMessage());
    }
    WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Handles the MethodArgumentTypeMismatchException that occurs when the method argument type does
   * not match.
   *
   * <p>This method is invoked when a MethodArgumentTypeMismatchException is thrown, indicating that
   * the type of a method argument does not match the expected type. It builds an appropriate error
   * response with the error message and returns it as a ResponseEntity.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Extracts the required type of the parameter that caused the type mismatch.
   *   <li>Retrieves the simple name of the required type, if present.
   *   <li>Retrieves the correlation ID from the request headers.
   *   <li>Sets the HTTP status code to BAD_REQUEST.
   *   <li>Constructs an error message indicating the type mismatch for the parameter, including the
   *       parameter name, value, required type, and the exception message.
   *   <li>Builds a WebApiError object with the error message, HTTP status code, correlation ID, and
   *       null details.
   *   <li>Creates a ResponseEntity with the WebApiError and the corresponding HTTP status code.
   * </ol>
   *
   * @param ex the MethodArgumentTypeMismatchException that triggered the exception handling
   * @param webRequest the WebRequest object representing the current request
   * @return a ResponseEntity containing the WebApiError object and the appropriate HTTP status code
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(
      final MethodArgumentTypeMismatchException ex, final WebRequest webRequest) {
    Optional<Class<?>> requiredType = Optional.ofNullable(ex.getRequiredType());
    String simpleName = "";
    if (requiredType.isPresent()) {
      simpleName = requiredType.get().getSimpleName();
    }
    String correlationId = webRequest.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.BAD_REQUEST;
    final String error =
        String.format(
            "The parameter '%s' of value '%s' could not be converted to type '%s': %s",
            ex.getName(), ex.getValue(), simpleName, sanitizeErrorMessage(ex.getMessage()));
    WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId, null);
    return buildResponseEntity(webApiError, httpStatusCode);
  }

  /**
   * Builds a list of WebApiError objects from the given list of FieldError objects.
   *
   * <p>This method iterates over the list of FieldError objects and constructs a WebApiError object
   * for each field error. The WebApiError object contains the error message, HTTP status code,
   * correlation ID, and null details.
   *
   * <p>The method performs the following steps for each FieldError:
   *
   * <ol>
   *   <li>Retrieves the error message for the invalid field using the
   *       getErrorMessageForInvalidField method.
   *   <li>Builds a WebApiError object with the error message, HTTP status code (BAD_REQUEST),
   *       correlation ID, and null details.
   *   <li>Adds the WebApiError object to the list of webApiErrors.
   * </ol>
   *
   * @param fieldErrors the list of FieldError objects representing the field validation errors
   * @param correlationId the correlation ID associated with the request
   * @return a list of WebApiError objects constructed from the field errors
   */
  private List<WebApiError> getApiErrorsFromFieldErrors(
      final List<FieldError> fieldErrors, final String correlationId) {
    List<WebApiError> webApiErrors = new ArrayList<>();
    WebApiError webApiError = null;
    HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
    String errorMessage = null;
    for (FieldError fieldError : fieldErrors) {
      errorMessage =
          getErrorMessageForInvalidField(
              fieldError.getObjectName(),
              fieldError.getField(),
              fieldError.getRejectedValue() != null
                  ? fieldError.getRejectedValue().toString()
                  : "null",
              fieldError.getDefaultMessage());
      webApiError = buildWebApiError(errorMessage, statusCode, correlationId, null);
      webApiErrors.add(webApiError);
    }
    return webApiErrors;
  }

  /**
   * Builds a list of WebApiError objects from the given set of ConstraintViolation objects.
   *
   * <p>This method iterates over the set of ConstraintViolation objects and constructs a
   * WebApiError object for each constraint violation. The WebApiError object contains the error
   * message, HTTP status code, correlation ID, and null details.
   *
   * <p>The method performs the following steps for each ConstraintViolation:
   *
   * <ol>
   *   <li>Retrieves the error message for the invalid field using the
   *       getErrorMessageForInvalidField method.
   *   <li>Builds a WebApiError object with the error message, HTTP status code (BAD_REQUEST),
   *       correlation ID, and null details.
   *   <li>Adds the WebApiError object to the list of webApiErrors.
   * </ol>
   *
   * @param constraintViolations the set of ConstraintViolation objects representing the constraint
   *     violations
   * @param correlationId the correlation ID associated with the request
   * @return a list of WebApiError objects constructed from the constraint violations
   */
  private List<WebApiError> getApiErrorsFromConstraintViolations(
      final Set<ConstraintViolation<?>> constraintViolations, final String correlationId) {
    List<WebApiError> webApiErrors = new ArrayList<>();
    WebApiError webApiError = null;
    HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
    String errorMessage = null;
    for (ConstraintViolation<?> constraintViolation : constraintViolations) {
      errorMessage =
          getErrorMessageForInvalidField(
              constraintViolation.getRootBeanClass().getSimpleName(),
              constraintViolation.getLeafBean().toString(),
              constraintViolation.getInvalidValue() != null
                  ? constraintViolation.getInvalidValue().toString()
                  : "null",
              constraintViolation.getMessage());
      webApiError = buildWebApiError(errorMessage, statusCode, correlationId, null);
      webApiErrors.add(webApiError);
    }
    return webApiErrors;
  }

  /**
   * Generates an error message for an invalid field.
   *
   * <p>This method constructs an error message string that includes the object name, field name,
   * rejected value, and the default error message. The error message is formatted using the
   * String.format method.
   *
   * <p>The error message format is as follows: "Invalid value {rejectedValue} on field {field} for
   * object {objectName}: {defaultMessage}."
   *
   * @param objectName the name of the object containing the invalid field
   * @param field the name of the invalid field
   * @param rejectedValue the rejected value for the field
   * @param defaultMessage the default error message associated with the field
   * @return the formatted error message string
   */
  private String getErrorMessageForInvalidField(
      final String objectName,
      final String field,
      final String rejectedValue,
      final String defaultMessage) {
    return String.format(
        "Invalid value %s on field %s for object %s: %s.",
        rejectedValue, field, objectName, defaultMessage);
  }

  /**
   * Generates a map of generic error properties.
   *
   * <p>This method creates a map of generic error properties that can be included in error
   * responses. The map contains the following properties:
   *
   * <ul>
   *   <li>timestamp: The current timestamp in UTC format, formatted using the dateFormatter.
   *   <li>correlationId: The correlation ID associated with the request.
   * </ul>
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Creates a new HashMap to store the generic error properties.
   *   <li>Retrieves the current timestamp in UTC format using OffsetDateTime.now(ZoneOffset.UTC)
   *       and formats it using the dateFormatter.
   *   <li>Adds the formatted timestamp to the map with the key "timestamp".
   *   <li>Adds the correlation ID to the map with the key "correlationId".
   *   <li>Returns the map of generic error properties.
   * </ol>
   *
   * @param correlationId the correlation ID associated with the request
   * @return a map of generic error properties containing the timestamp and correlation ID
   */
  private Map<String, String> getGenericErrorProperties(final String correlationId) {
    Map<String, String> genericProperties = new HashMap<>();
    genericProperties.put(TIMESTAMP_KEY, OffsetDateTime.now(ZoneOffset.UTC).format(dateFormatter));
    genericProperties.put(CORRELATION_ID_KEY, correlationId);
    return genericProperties;
  }

  /**
   * Builds a WebApiError object with the provided error details.
   *
   * <p>This method constructs a WebApiError object using the provided error message, HTTP status
   * code, correlation ID, and optional details. The WebApiError object represents an error response
   * in the API.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Creates a new WebApiError.Builder instance.
   *   <li>Sets the error code using the HTTP status code value.
   *   <li>Sets the error message.
   *   <li>Sets the generic error properties using the getGenericErrorProperties method, passing the
   *       correlation ID.
   *   <li>If the details list is not empty, sets the details in the WebApiError builder.
   *   <li>Builds and returns the WebApiError object.
   * </ol>
   *
   * @param errorMessage the error message to be included in the WebApiError
   * @param httpStatusCode the HTTP status code associated with the error
   * @param correlationId the correlation ID associated with the request
   * @param details an optional list of WebApiError objects representing additional error details
   * @return the constructed WebApiError object
   */
  private WebApiError buildWebApiError(
      final String errorMessage,
      final HttpStatusCode httpStatusCode,
      final String correlationId,
      final List<WebApiError> details) {
    WebApiError.Builder builder =
        WebApiError.builder()
            .code(String.valueOf(httpStatusCode.value()))
            .message(errorMessage)
            .properties(getGenericErrorProperties(correlationId));
    if (!ObjectUtils.isEmpty(details)) {
      builder.details(details);
    }
    return builder.build();
  }

  /**
   * Builds a ResponseEntity object with the provided WebApiError and HTTP status.
   *
   * <p>This method constructs a ResponseEntity object that represents the error response to be
   * returned to the client. It wraps the WebApiError object in a WebApiErrorResponse and sets the
   * HTTP status code of the response.
   *
   * <p>The method performs the following steps:
   *
   * <ol>
   *   <li>Creates a new WebApiErrorResponse object, passing the WebApiError as a constructor
   *       argument.
   *   <li>Creates a new ResponseEntity object, passing the WebApiErrorResponse and the HTTP status
   *       code.
   *   <li>Returns the constructed ResponseEntity object.
   * </ol>
   *
   * @param webApiError the WebApiError object representing the error details
   * @param httpStatus the HTTP status code to be set in the response
   * @return the constructed ResponseEntity object containing the WebApiErrorResponse and HTTP
   *     status
   */
  private ResponseEntity<Object> buildResponseEntity(
      final WebApiError webApiError, final HttpStatus httpStatus) {
    WebApiErrorResponse webApiErrorResponse = new WebApiErrorResponse(webApiError);
    return new ResponseEntity<>(webApiErrorResponse, httpStatus);
  }

  /**
   * Sanitizes the error message by masking sensitive information based on predefined patterns.
   *
   * <p>This method takes an error message as input and applies a set of predefined patterns or
   * rules to identify sensitive information. The sensitive patterns are defined as an array of
   * regular expressions that match common sensitive data such as passwords, secrets, tokens, or
   * connection strings.
   *
   * <p>The method iterates over each sensitive pattern and replaces any matches found in the error
   * message with a masked value, such as "[MASKED]". This ensures that sensitive information is not
   * exposed in the error messages returned to the client.
   *
   * <p>The predefined sensitive patterns can be extended or modified by adding more regular
   * expressions to the {@code sensitivePatterns} array.
   *
   * @param errorMessage the error message to be sanitized
   * @return the sanitized error message with sensitive information masked
   */
  private String sanitizeErrorMessage(String errorMessage) {
    // Define patterns or rules to identify sensitive information
    String[] sensitivePatterns = {
      ".*password.*", ".*secret.*", ".*token.*", ".*connection string.*",
      // Add more patterns as needed
    };

    // Iterate over the sensitive patterns and replace matches with a masked value
    for (String pattern : sensitivePatterns) {
      errorMessage = errorMessage.replaceAll(pattern, "[MASKED]");
    }

    return errorMessage;
  }

  /**
   * Handles authentication exceptions and returns a custom JSON response.
   *
   * <p>This method is called when an authentication exception occurs during the authentication
   * process. It generates a standardized JSON response containing the error details and sends it
   * back to the client.
   *
   * <p>The response includes the following information:
   *
   * <ul>
   *   <li>HTTP status code: {@link HttpStatus#UNAUTHORIZED} (401)
   *   <li>Timestamp: The current timestamp
   *   <li>Correlation ID: The correlation ID extracted from the request header
   *   <li>Error message: The sanitized error message from the authentication exception
   * </ul>
   *
   * <p>The error message is sanitized using the {@link #sanitizeErrorMessage(String)} method to
   * mask any sensitive information based on predefined patterns.
   *
   * <p>The response is set with the appropriate HTTP status code, content type, and the JSON
   * representation of the {@link WebApiError} object.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param authException the authentication exception that occurred
   * @throws IOException if an I/O error occurs while writing the response
   */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.UNAUTHORIZED;
    String error = sanitizeErrorMessage(authException.getMessage());
    WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId, null);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getOutputStream().println(objectMapper.writeValueAsString(webApiError));
  }

  /**
   * Handles the {@link AccessDeniedException} and returns a custom error response.
   *
   * <p>This method is invoked when an access denied exception occurs during request processing. It
   * generates a standardized error response with the following details:
   *
   * <ul>
   *   <li>HTTP status code: {@link HttpStatus#FORBIDDEN} (403)
   *   <li>Timestamp: The current timestamp
   *   <li>Correlation ID: The correlation ID extracted from the request header
   *   <li>Error message: The sanitized error message from the exception
   * </ul>
   *
   * <p>The error message is sanitized using the {@link #sanitizeErrorMessage(String)} method to
   * mask any sensitive information based on predefined patterns.
   *
   * <p>The response is set with the appropriate HTTP status code ({@link
   * HttpServletResponse#SC_FORBIDDEN}), content type ({@code "application/json"}), and the JSON
   * representation of the {@link WebApiError} object is written to the response's output stream.
   *
   * @param request the {@link HttpServletRequest} object representing the current request
   * @param response the {@link HttpServletResponse} object representing the current response
   * @param accessDeniedException the {@link AccessDeniedException} that occurred
   * @throws IOException if an I/O error occurs while writing the response
   * @throws ServletException if a servlet-specific error occurs
   */
  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    HttpStatus httpStatusCode = HttpStatus.FORBIDDEN;
    String error = sanitizeErrorMessage(accessDeniedException.getMessage());
    WebApiError webApiError = buildWebApiError(error, httpStatusCode, correlationId, null);
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType("application/json");
    response.getOutputStream().println(objectMapper.writeValueAsString(webApiError));
  }
}
