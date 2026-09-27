package com.bikebackend.exception;

/**
 * Base exception for all application errors.
 *
 * Every subclass carries an HTTP status code so the servlet
 * can use exception.getStatus() directly — no guessing whether
 * an exception means 400 or 404.
 *
 * Flow:
 *   Service throws NotFoundException("User not found")
 *       ↓
 *   Servlet catches AppException
 *       ↓
 *   Servlet writes: HTTP 404 + {"error": "User not found"}
 */
public class AppException extends RuntimeException {

    private final int status;

    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
