package com.forjix.cuentoskilla.exception;

import java.time.Instant;

public class ApiError {
    private final String code;
    private final String message;
    private final Instant timestamp = Instant.now();

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
