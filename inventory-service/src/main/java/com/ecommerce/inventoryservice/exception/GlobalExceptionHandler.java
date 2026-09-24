package com.ecommerce.inventoryservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InventoryNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleInventoryNotFound(InventoryNotFoundException ex,
                                                               HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(InventoryAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleInventoryAlreadyExists(InventoryAlreadyExistsException ex,
                                                                    HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(FieldError::getField, this::message, (first, second) -> first));

    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "Validation failed",
        request.getRequestURI(),
        fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getRequestURI());
    return ResponseEntity.status(status).body(errorResponse);
  }

  private String message(FieldError fieldError) {
    return fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value";
  }
}
