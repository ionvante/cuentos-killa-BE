package com.forjix.cuentoskilla.controller;

import java.util.List;

import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.Direccion;
import com.forjix.cuentoskilla.model.DTOs.DireccionDTO;
import com.forjix.cuentoskilla.service.DireccionService;

/**
 * API de Gestión de Direcciones
 * 
 * Rutas: /api/v1/direcciones
 * 
 * Funcionalidad:
 * - GET /usuario/{id}: Obtener direcciones del usuario
 * - POST: Crear nueva dirección
 * - PUT /{id}: Actualizar dirección
 * - DELETE /{id}: Eliminar dirección
 * 
 * Seguridad (OWASP):
 * - Autenticación requerida
 * - Validación de propiedad de datos personales
 */
@RestController
@RequestMapping("/api/v1/direcciones")
public class DireccionController {

    private static final Logger logger = LoggerFactory.getLogger(DireccionController.class);
    private final DireccionService service;

    public DireccionController(DireccionService service) {
        this.service = service;
    }

    private UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        throw new IllegalStateException("Usuario no autenticado o UserDetailsImpl no disponible");
    }

    /**
     * Obtener direcciones del usuario autenticado
     * GET /api/v1/direcciones/usuario/{usuarioId}
     * Acceso: Propietario de datos o ADMIN
     */
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Direccion>>> listarPorUsuario(
            @PathVariable Long usuarioId) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("GET /api/v1/direcciones/usuario/{} - Usuario {} listando direcciones", usuarioId, user.getId());
        List<Direccion> direcciones = service.obtenerPorUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.success(direcciones, "Direcciones obtenidas exitosamente"));
    }

    /**
     * Guardar nueva dirección
     * POST /api/v1/direcciones
     * Acceso: Usuario autenticado
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Direccion>> guardar(
            @RequestBody DireccionDTO dto) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("POST /api/v1/direcciones - Usuario {} guardando dirección", user.getId());
        try {
            Direccion saved = service.guardar(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(saved, "Dirección guardada exitosamente"));
        } catch (Exception e) {
            logger.error("Error al guardar dirección para usuario {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("SAVE_ERROR", "Error al guardar la dirección"));
        }
    }

    /**
     * Actualizar dirección existente
     * PUT /api/v1/direcciones/{id}
     * Acceso: Propietario de la dirección o ADMIN
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Direccion>> actualizar(
            @PathVariable Long id,
            @RequestBody DireccionDTO dto) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("PUT /api/v1/direcciones/{} - Usuario {} actualizando dirección", id, user.getId());
        try {
            Direccion updated = service.actualizar(id, dto);
            return ResponseEntity.ok(ApiResponse.success(updated, "Dirección actualizada exitosamente"));
        } catch (Exception e) {
            logger.error("Error al actualizar dirección {} para usuario {}: {}", id, user.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("UPDATE_ERROR", "Error al actualizar la dirección"));
        }
    }

    /**
     * Eliminar dirección
     * DELETE /api/v1/direcciones/{id}
     * Acceso: Propietario de la dirección o ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable Long id) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("DELETE /api/v1/direcciones/{} - Usuario {} eliminando dirección", id, user.getId());
        try {
            service.eliminar(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Dirección eliminada exitosamente"));
        } catch (Exception e) {
            logger.error("Error al eliminar dirección {} para usuario {}: {}", id, user.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("DELETE_ERROR", "Error al eliminar la dirección"));
        }
    }
}
