package com.forjix.cuentoskilla.exception;

import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.service.storage.StorageException;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador centralizado de excepciones para APIs
 * Sigue estructura estándar OWASP de respuestas
 * 
 * No expone detalles internos del servidor
 * Proporciona códigos de error consistentes
 * Integrado con Sentry para monitoreo
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * Maneja errores de validación de entrada
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        logger.warn("Validation error: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "VALIDATION_ERROR",
                        "Error en validación de datos",
                        errors,
                        request.getDescription(false).replace("uri=", "")));
    }

    /**
     * Maneja excepciones de autenticación
     */
    @ExceptionHandler({ BadCredentialsException.class, UsernameNotFoundException.class })
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            Exception ex,
            WebRequest request) {

        logger.warn("Authentication failed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        "AUTHENTICATION_ERROR",
                        "Credenciales inválidas",
                        null,
                        request.getDescription(false).replace("uri=", "")));
    }

    /**
     * Maneja excepciones de archivo muy grande
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxSize(
            MaxUploadSizeExceededException ex,
            WebRequest request) {

        Sentry.captureException(ex);
        logger.error("File upload size exceeded", ex);

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(
                        "FILE_TOO_LARGE",
                        "El archivo excede el tamaño máximo permitido (5MB)",
                        null,
                        request.getDescription(false).replace("uri=", "")));
    }

    /**
     * Maneja excepciones de almacenamiento
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<Object>> handleStorage(
            StorageException ex,
            WebRequest request) {

        Sentry.captureException(ex);
        logger.error("Storage error: {}", ex.getMessage(), ex);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = "STORAGE_ERROR";

        if ("INVALID_FILE".equals(ex.getMessage())) {
            status = HttpStatus.BAD_REQUEST;
            code = "INVALID_FILE";
        } else if ("MAX_UPLOAD_SIZE_EXCEEDED".equals(ex.getMessage())) {
            status = HttpStatus.PAYLOAD_TOO_LARGE;
            code = "FILE_TOO_LARGE";
        }

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(
                        code,
                        "Error al procesar el archivo",
                        null,
                        request.getDescription(false).replace("uri=", "")));
    }

    /**
     * Cliente cerró la conexión antes de recibir la respuesta.
     * Normal en desarrollo (hot-reload, navegación rápida).
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientAbort(AsyncRequestNotUsableException ex) {
        logger.debug("Client disconnected: {}", ex.getMessage());
    }

    /**
     * Maneja todas las excepciones genéricas no capturadas
     * NO expone detalles internos
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(
            Exception ex,
            WebRequest request) {

        Sentry.captureException(ex);
        logger.error("Unhandled exception", ex);

        // NO exponer el mensaje exacto de la excepción
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "INTERNAL_SERVER_ERROR",
                        "Error interno del servidor. Por favor, intente más tarde.",
                        null,
                        request.getDescription(false).replace("uri=", "")));
    }
}
