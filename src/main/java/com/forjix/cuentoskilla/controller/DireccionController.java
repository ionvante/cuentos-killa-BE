package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.Direccion;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.model.DTOs.DireccionDTO;
import com.forjix.cuentoskilla.service.DireccionService;
import java.util.List;
import java.util.NoSuchElementException;
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

    private boolean isAdmin(UserDetailsImpl user) {
        return user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Direccion>>> listarMisDirecciones() {
        UserDetailsImpl user = getCurrentUser();
        logger.info("GET /api/v1/direcciones/me - Usuario {} listando sus direcciones", user.getId());
        List<Direccion> direcciones = service.obtenerPorUsuario(user.getId(), user.getId(), isAdmin(user));
        return ResponseEntity.ok(ApiResponse.success(direcciones, "Direcciones obtenidas exitosamente"));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Direccion>>> listarPorUsuario(@PathVariable Long usuarioId) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("GET /api/v1/direcciones/usuario/{} - Usuario {} listando direcciones", usuarioId, user.getId());
        try {
            List<Direccion> direcciones = service.obtenerPorUsuario(usuarioId, user.getId(), isAdmin(user));
            return ResponseEntity.ok(ApiResponse.success(direcciones, "Direcciones obtenidas exitosamente"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "No tiene permisos para ver estas direcciones"));
        }
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Direccion>> guardar(@RequestBody DireccionDTO dto) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("POST /api/v1/direcciones - Usuario {} guardando direccion", user.getId());
        try {
            Direccion saved = service.guardar(dto, user.getId(), isAdmin(user));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(saved, "Direccion guardada exitosamente"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("USER_NOT_FOUND", "Usuario no encontrado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_ADDRESS_TYPE", "Tipo de direccion invalido"));
        } catch (Exception e) {
            logger.error("Error al guardar direccion para usuario {}: {}", user.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("SAVE_ERROR", "Error al guardar la direccion"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Direccion>> actualizar(@PathVariable Long id, @RequestBody DireccionDTO dto) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("PUT /api/v1/direcciones/{} - Usuario {} actualizando direccion", id, user.getId());
        try {
            Direccion updated = service.actualizar(id, dto, user.getId(), isAdmin(user));
            return ResponseEntity.ok(ApiResponse.success(updated, "Direccion actualizada exitosamente"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "No tiene permisos para actualizar esta direccion"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("ADDRESS_NOT_FOUND", "Direccion no encontrada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_ADDRESS_TYPE", "Tipo de direccion invalido"));
        } catch (Exception e) {
            logger.error("Error al actualizar direccion {} para usuario {}: {}", id, user.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("UPDATE_ERROR", "Error al actualizar la direccion"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        UserDetailsImpl user = getCurrentUser();
        logger.info("DELETE /api/v1/direcciones/{} - Usuario {} eliminando direccion", id, user.getId());
        try {
            service.eliminar(id, user.getId(), isAdmin(user));
            return ResponseEntity.ok(ApiResponse.success(null, "Direccion eliminada exitosamente"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "No tiene permisos para eliminar esta direccion"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("ADDRESS_NOT_FOUND", "Direccion no encontrada"));
        } catch (Exception e) {
            logger.error("Error al eliminar direccion {} para usuario {}: {}", id, user.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("DELETE_ERROR", "Error al eliminar la direccion"));
        }
    }
}
