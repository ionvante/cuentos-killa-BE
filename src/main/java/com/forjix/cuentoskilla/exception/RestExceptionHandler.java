package com.forjix.cuentoskilla.exception;

import com.forjix.cuentoskilla.service.storage.StorageException;
import io.sentry.Sentry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxSize(MaxUploadSizeExceededException ex) {
        Sentry.captureException(ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("MAX_UPLOAD_SIZE_EXCEEDED", "File too large"));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiError> handleStorage(StorageException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = ex.getMessage();
        if (!"INVALID_FILE".equals(code) && !"MAX_UPLOAD_SIZE_EXCEEDED".equals(code)) {
            code = "STORAGE_ERROR";
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        Sentry.captureException(ex);
        return ResponseEntity.status(status)
                .body(new ApiError(code, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        Sentry.captureException(ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", ex.getMessage()));
    }
}
