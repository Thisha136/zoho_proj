package com.bikebackend.exception;

/**
 * 404 Not Found
 *
 * Use when a requested resource does not exist in the database.
 *
 * Examples:
 *   - GET /users/99 — user 99 does not exist
 *   - PATCH /users/99/status — user 99 not found
 */
public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super(404, message);
    }
}
