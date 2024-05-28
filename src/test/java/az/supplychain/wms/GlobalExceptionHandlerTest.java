/*
 *  GlobalExceptionHandlerTest.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;

import az.supplychain.wms.dto.TestRequestDTO;
import az.supplychain.wms.exceptions.EntityNotFoundException;
import az.supplychain.wms.testController.SomeController;
import jakarta.validation.ConstraintViolationException;

@SpringBootTest
@AutoConfigureMockMvc
public class GlobalExceptionHandlerTest {
  @Autowired private MockMvc mockMvc;
  @InjectMocks private SomeController someController;

  @Configuration
  public static class GlobalExceptionHandlerIntegrationTestConfig {

    // NOTE: For setting custom test config, uncomment this config
    // class and
    // remove the config class from @SpringBootTest.

    @Bean
    MethodValidationPostProcessor methodValidationPostProcessor() {
      final MethodValidationPostProcessor methodValidationPostProcessor =
          new MethodValidationPostProcessor();
      methodValidationPostProcessor.setValidator(validator());

      return methodValidationPostProcessor;
    }

    @Bean
    LocalValidatorFactoryBean validator() {
      final LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();

      return localValidatorFactoryBean;
    }
  }

  @BeforeEach
  public void setup() {
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(someController) // instantiate controller.
            .setControllerAdvice(new RestApiExceptionHandler()) // bind with
            .build();
  }

  @Test
  public void testExceptionThrowingControllerExists() {
    // A helper test to check if test controller created is in the spring test
    // context
    assertThat(someController).isNotNull();
  }

  /**
   * Tests that MissingServletRequestParameterException is handled via global exception handler.
   *
   * @throws Exception
   */
  @Test
  public void testHandleMissingServletRequestParameterException() throws Exception {
    this.mockMvc
        .perform(get("/tests/exception/missing-parameters"))
        .andExpect(status().isBadRequest()) // HTTP
        // 400
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException()
                        instanceof MissingServletRequestParameterException))
        .andExpect(
            result ->
                Assertions.assertEquals(
                    "Required request parameter 'param1' for method parameter type String is not present",
                    Objects.requireNonNull(result.getResolvedException()).getMessage()));
  }

  @Test
  public void testHandleHttpMediaTypeNotSupportedException() throws Exception {
    this.mockMvc
        .perform(
            post("/tests/exception/invalid-media-type-for-json")
                .contentType(MediaType.APPLICATION_XML)
                .content("{\"field1\":\"ABC\"}"))
        .andExpect(status().isUnsupportedMediaType()) // HTTP 415
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException() instanceof HttpMediaTypeNotSupportedException))
        .andExpect(
            result ->
                Assertions.assertEquals(
                    "Content-Type 'application/xml' is not supported",
                    Objects.requireNonNull(result.getResolvedException()).getMessage()));
  }

  @Test
  public void testHandleMethodArgumentNotValidException() throws Exception {
    TestRequestDTO sample = new TestRequestDTO(null, "ABC");
    ObjectMapper objectMapper = new ObjectMapper();
    this.mockMvc
        .perform(
            post("/tests/exception/method-args-invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sample)))
        .andExpect(status().isBadRequest()) // HTTP 400
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException() instanceof MethodArgumentNotValidException))
        .andExpect(jsonPath("$.error.details[0].message").value("Invalid value null on field field1 for object testRequestDTO: must not be blank."))
        .andExpect(jsonPath("$.error.message").value("Validation error"));
  }

  @Test
  public void testHandleConstraintViolationException() throws Exception {
    TestRequestDTO sample = new TestRequestDTO("person1", "");
    ObjectMapper objectMapper = new ObjectMapper();

    this.mockMvc
        .perform(
            post("/tests/exception/payload-constraint-violated")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sample)))
        .andExpect(status().isBadRequest()) // HTTP
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException() instanceof ConstraintViolationException))
        .andExpect(
            result ->
                Assertions.assertEquals(
                    "Payload validation failed",
                    Objects.requireNonNull(result.getResolvedException()).getMessage()))
        .andExpect(jsonPath("$.error.message").value("Payload validation failed"))
        .andExpect(jsonPath("$.error.details[0].message").value("Invalid value  on field TestRequestDTO(field1=person1, field2=) for object TestRequestDTO: must not be blank."));
  }

  @Test
  public void testHandleCustomEntityNotFoundException() throws Exception {
    TestRequestDTO sample = new TestRequestDTO("A", "B");
    ObjectMapper objectMapper = new ObjectMapper();

    this.mockMvc
        .perform(
            post("/tests/exception/custom-entity-not-found")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sample)))
        .andExpect(status().isNotFound())
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException() instanceof EntityNotFoundException))
        .andExpect(
            result ->
                Assertions.assertEquals(
                    "SomeController was not found for parameters {}",
                    Objects.requireNonNull(result.getResolvedException()).getMessage()));
  }

  @Test
  public void testHandleEntityNotFoundException() throws Exception {
    TestRequestDTO sample = new TestRequestDTO("A", "B");
    ObjectMapper objectMapper = new ObjectMapper();

    this.mockMvc
        .perform(
            post("/tests/exception/entity-not-found")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sample)))
        .andExpect(status().isNotFound())
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException()
                        instanceof jakarta.persistence.EntityNotFoundException));
  }

  @Test
  public void testHandleHttpMessageNotReadableException() throws Exception {
    this.mockMvc
        .perform(
            post("/tests/exception/malformed-json")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{field1: \"ABC\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException() instanceof HttpMessageNotReadableException))
        .andExpect(jsonPath("$.error.message").value("Malformed JSON request: JSON parse error: Unexpected character ('f' (code 102)): was expecting double-quote to start field name"));
  }

  @Test
  public void testHandleHttpMessageNotWritableException() throws Exception {
    this.mockMvc
        .perform(get("/tests/exception/json-output-error"))
        .andExpect(status().isInternalServerError())
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException() instanceof HttpMessageNotWritableException))
        .andExpect(jsonPath("$.error.message").value("Error writing JSON output: Dummy non writable message raised!"));
  }

  @Test
  public void testHandleNoHandlerFoundException() throws Exception {
    this.mockMvc
        .perform(get("/non-existent-url"))
        .andExpect(status().is4xxClientError())
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException() instanceof NoHandlerFoundException))
        .andExpect(
            result ->
                Assertions.assertEquals(
                    "No endpoint GET /non-existent-url.",
                    Objects.requireNonNull(result.getResolvedException()).getMessage()))
        .andExpect(
            jsonPath("$.error.message")
                .value("Could not find the GET method for URL /non-existent-url: No endpoint GET /non-existent-url."));
  }

  @Test
  public void testHandleDataIntegrityViolationException() throws Exception {
    this.mockMvc
        .perform(
            post("/tests/exception/data-integrity-violation")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isConflict())
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException() instanceof DataIntegrityViolationException))
        .andExpect(jsonPath("$.error.message").value("Database error: Dummy data integrity violation raised!"));
  }

  @Test
  public void testHandleMethodArgumentTypeMismatchException() throws Exception {
    this.mockMvc
        .perform(get("/tests/exception/type-mismatch?param1=invalidInt"))
        .andExpect(status().isBadRequest())
        .andExpect(
            result ->
                Assertions.assertTrue(
                    result.getResolvedException() instanceof MethodArgumentTypeMismatchException))
        .andExpect(
            result ->
                Assertions.assertEquals(
                    "Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; For input string: \"invalidInt\"",
                    Objects.requireNonNull(result.getResolvedException()).getMessage()))
        .andExpect(
            jsonPath("$.error.message")
                .value(
                    "The parameter 'param1' of value 'invalidInt' could not be converted to type 'Integer': Failed to convert value of type 'java.lang.String' to required type 'java.lang.Integer'; For input string: \"invalidInt\""));
  }
}
