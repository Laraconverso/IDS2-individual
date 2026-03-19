package com.eCommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Global exception handler to enforce the RFC 7807 standard.
 * Intercepts exceptions and formats the response as application/problem+json.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI BLANK_TYPE = URI.create("about:blank");

    /**
     * Handles 404 Not Found errors when a resource is missing.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setType(BLANK_TYPE);
        problemDetail.setTitle("Product Not Found");
        // RFC 7807: instance should be the request URI that caused the error
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(problemDetail);
    }

    /**
     * Handles 400 Bad Request errors from validation failures (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Concatenate all validation errors into a single detailed string
        String validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, validationErrors);
        problemDetail.setType(BLANK_TYPE);
        problemDetail.setTitle("Bad Request");
        // RFC 7807: include the instance URI where the validation failed
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(problemDetail);
    }
}