package com.bikebackend.exception;

/**
 * 400 Bad Request
 *
 * Use when the client sends invalid or missing data.
 *
 * Examples:
 *   - required field is blank
 *   - invalid status value
 *   - malformed ID in URL
 */
public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(400, message);
    }
}
