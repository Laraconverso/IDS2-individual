package com.eCommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import jakarta.validation.ConstraintViolationException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler to enforce the RFC 7807 standard.
 * Intercepts exceptions and formats the response strictly as application/problem+json.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String CONTENT_TYPE = "application/problem+json";

    /**
     * Helper method to bypass Spring Boot's automatic hiding of "about:blank".
     * Generates a strict RFC 7807 Map.
     */
    private Map<String, Object> buildProblemDetail(HttpStatus status, String title, String detail, String instance) {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "about:blank"); // Forced to appear in the JSON
        problem.put("title", title);
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("instance", instance);
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.NOT_FOUND, "Product Not Found", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", detail, request.getRequestURI()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        log.warn("Path variable validation error: {}", ex.getMessage());
        String detail = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", detail, request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", detail, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed request body: {}", ex.getMessage());
        String detail = "Invalid request body or data type mismatch.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", detail, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Unsupported media type: {}", ex.getContentType());
        String detail = "Unsupported Media Type. Expected application/json, got: " + ex.getContentType();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", detail, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        log.warn("Not acceptable media type: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.NOT_ACCEPTABLE, "Not Acceptable", "Expected application/json.", request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", "Database constraint violation. Please verify your input.", request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not supported: {}", ex.getMethod());
        String detail = "Method '" + ex.getMethod() + "' is not supported.";
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", detail, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch for parameter '{}'", ex.getName());
        String detail = String.format("The parameter '%s' has an invalid type.", ex.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", detail, request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        String detail = String.format("The required parameter '%s' is missing.", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", detail, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.parseMediaType(CONTENT_TYPE))
                .body(buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.", request.getRequestURI()));
    }
}