package com.perscholas.cashtran.controller;

import com.perscholas.cashtran.exception.EmailAlreadyRegisteredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(
      EmailAlreadyRegisteredException ex, HttpServletRequest request) {

    ErrorResponse body =
        new ErrorResponse(
            Instant.now().toString(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(
      ResponseStatusException ex, HttpServletRequest request) {

    int statusVal = ex.getStatusCode().value();
    String reason = HttpStatus.resolve(statusVal) != null ? HttpStatus.resolve(statusVal).getReasonPhrase() : ex.getStatusCode().toString();

    ErrorResponse body =
        new ErrorResponse(
            Instant.now().toString(),
            statusVal,
            reason,
            ex.getReason() != null ? ex.getReason() : "Request failed",
            request.getRequestURI(),
            null);

    return ResponseEntity.status(ex.getStatusCode()).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    List<ErrorResponse.FieldError> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe ->
                    new ErrorResponse.FieldError(
                        fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()))
            .collect(Collectors.toList());

    ErrorResponse body =
        new ErrorResponse(
            Instant.now().toString(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Validation failed",
            request.getRequestURI(),
            fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {

    List<ErrorResponse.FieldError> violations =
        ex.getConstraintViolations().stream()
            .map(
                cv ->
                    new ErrorResponse.FieldError(
                        cv.getPropertyPath().toString(), cv.getInvalidValue(), cv.getMessage()))
            .collect(Collectors.toList());

    ErrorResponse body =
        new ErrorResponse(
            Instant.now().toString(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Constraint violations",
            request.getRequestURI(),
            violations);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {

    String message =
        ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();

    ErrorResponse body =
        new ErrorResponse(
            Instant.now().toString(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            message,
            request.getRequestURI(),
            null);

    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
    // Log the full stack trace for unexpected errors
    log.error("Unhandled exception for request {} {}", request.getMethod(), request.getRequestURI(), ex);

    ErrorResponse body =
        new ErrorResponse(
            Instant.now().toString(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
            request.getRequestURI(),
            null);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
