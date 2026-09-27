package com.bikebackend.exception;

/**
 * 403 Forbidden
 *
 * Use when the caller IS authenticated but does NOT have
 * permission to perform the requested operation.
 *
 * This means: "I know who you are, but you can't do this."
 *
 * Examples:
 *   - USER tries to access another user's profile
 *   - USER tries to call an ADMIN-only endpoint
 *   - GARAGE tries to access a service request from a different garage
 */
public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super(403, message);
    }
}
