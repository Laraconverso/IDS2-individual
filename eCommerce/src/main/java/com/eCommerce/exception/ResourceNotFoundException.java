package com.eCommerce.exception;

/**
 * Exception thrown when a requested resource is not found in the system.
 * This will be mapped to an HTTP 404 Not Found response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}