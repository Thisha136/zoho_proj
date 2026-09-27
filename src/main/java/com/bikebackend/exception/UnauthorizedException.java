package com.bikebackend.exception;

/**
 * 401 Unauthorized
 *
 * Use when the request has no valid authentication token,
 * or the token is expired/invalid.
 *
 * This means: "I don't know who you are."
 *
 * Examples:
 *   - No Authorization header on a protected endpoint
 *   - JWT token is expired
 *   - JWT signature is invalid
 *
 * Note: "Unauthorized" is a misleading HTTP name.
 * It actually means "unauthenticated".
 * Use ForbiddenException (403) when the identity IS known
 * but does not have permission.
 */
public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(401, message);
    }
}
