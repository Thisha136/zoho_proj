package com.bikebackend.exception;

/**
 * 409 Conflict
 *
 * Use when the request cannot be completed because it conflicts
 * with the current state of the database.
 *
 * Examples:
 *   - POST /users — email already registered
 *   - POST /brands — brand name already exists
 *   - Duplicate key violation from MySQL
 */
public class ConflictException extends AppException {

    public ConflictException(String message) {
        super(409, message);
    }
}
