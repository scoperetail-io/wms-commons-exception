/*
 *  SomeController.java
 *  Copyright 2024 AutoZone, Inc.
 *  Content is confidential to and proprietary information of AutoZone, Inc.,
 *  its subsidiaries and affiliates.
 */
package az.supplychain.wms.testController;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import az.supplychain.wms.dto.TestRequestDTO;
import az.supplychain.wms.exceptions.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@RestController
@RequestMapping("/tests/exception")
public class SomeController {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @GetMapping(value = "/missing-parameters")
  public @ResponseBody String endpoint_receives_missing_params(
      @RequestParam("param1") String param1) {
    return param1;
  }

  @PostMapping(value = "/invalid-media-type-for-json", consumes = "application/json")
  public ResponseEntity<String> endpoint_receives_invalid_media_type(
      @RequestBody TestRequestDTO requestBody) {
    return ResponseEntity.ok("Succesfull Operation!");
  }

  @PostMapping(value = "/payload-constraint-violated", consumes = "application/json")
  public ResponseEntity<String> payload_constraint_violated(
      @RequestBody TestRequestDTO requestDto) {
    Set<ConstraintViolation<TestRequestDTO>> violations = validator.validate(requestDto);
    
    throw new ConstraintViolationException("Payload validation failed",violations);
  }

  @PostMapping(value = "/method-args-invalid", consumes = "application/json")
  public ResponseEntity<String> method_args_invalid(
      @RequestBody @Validated TestRequestDTO requestDto) {
    return ResponseEntity.ok("Request successfully processed");
  }

  @PostMapping(value = "/custom-entity-not-found", consumes = "application/json")
  public ResponseEntity<String> custom_entity_not_found(
      @RequestBody @Valid TestRequestDTO requestDto) {
    throw new EntityNotFoundException(getClass());
  }

  @PostMapping(value = "/entity-not-found", consumes = "application/json")
  public ResponseEntity<String> entity_not_found(@RequestBody @Valid TestRequestDTO requestDto) {
    throw new jakarta.persistence.EntityNotFoundException("Raised dummy entity not found exception.");
  }

  @GetMapping("/type-mismatch")
  public String testMethodArgumentTypeMismatch(@RequestParam(name = "param1") Integer param1) {
    return "This should trigger a MethodArgumentTypeMismatchException if param1 is not an integer";
  }

  @PostMapping("/malformed-json")
  public String testHttpMessageNotReadable(@RequestBody TestRequestDTO request) {
    return "This should trigger a HttpMessageNotReadableException if JSON is malformed";
  }

  @GetMapping("/json-output-error")
  public String testHttpMessageNotWritable() {
    throw new HttpMessageNotWritableException("Dummy non writable message raised!");
  }

  @PostMapping("/data-integrity-violation")
  public void testDataIntegrityViolation() {
    throw new org.springframework.dao.DataIntegrityViolationException(
        "Dummy data integrity violation raised!",
        new ConstraintViolationException("Dummy constraint violation raised", null));
  }
}