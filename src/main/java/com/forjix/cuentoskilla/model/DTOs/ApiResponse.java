package com.forjix.cuentoskilla.model.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Respuesta API estándar siguiendo OWASP
 * 
 * Estructura:
 * {
 *   "success": boolean,
 *   "code": string,
 *   "message": string,
 *   "data": object,
 *   "errors": array,
 *   "timestamp": datetime,
 *   "path": string
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String code;
    private String message;
    private T data;
    private Object errors;
    private LocalDateTime timestamp;
    private String path;
    
    // Constructor vacío
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor para éxito con data
    public ApiResponse(boolean success, String code, String message, T data) {
        this();
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    // Constructor para respuesta con errores
    public ApiResponse(boolean success, String code, String message, Object errors, String path) {
        this();
        this.success = success;
        this.code = code;
        this.message = message;
        this.errors = errors;
        this.path = path;
    }
    
    // Factory methods
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, "SUCCESS", message, data);
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "SUCCESS", "Operación exitosa", data);
    }
    
    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = code;
        response.message = message;
        return response;
    }
    
    public static <T> ApiResponse<T> error(String code, String message, Object errors, String path) {
        return new ApiResponse<>(false, code, message, errors, path);
    }
    
    // Getters y Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public Object getErrors() {
        return errors;
    }
    
    public void setErrors(Object errors) {
        this.errors = errors;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
}
