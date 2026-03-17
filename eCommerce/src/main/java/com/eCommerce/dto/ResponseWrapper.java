package com.eCommerce.dto;

/**
 * Generic wrapper for API responses to comply with the OpenAPI specification,
 * which requires a root "data" object for successful responses.
 *
 * @param <T> The type of the data payload.
 */
public record ResponseWrapper<T>(T data) {
}