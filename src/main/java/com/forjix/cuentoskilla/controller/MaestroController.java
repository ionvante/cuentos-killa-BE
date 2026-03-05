package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Maestro;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.service.MaestroService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de Gestión de Maestros (Catálogo General)
 * 
 * Rutas: /api/v1/maestros
 * 
 * Funcionalidad:
 * - GET: Obtener todos los maestros
 * - GET /grupo/{grupo}: Obtener maestros por grupo
 * - POST: Crear maestro (solo ADMIN)
 * - PUT /{id}: Actualizar maestro (solo ADMIN)
 * - DELETE /{id}: Eliminar maestro (solo ADMIN)
 * 
 * Seguridad (OWASP):
 * - GET disponible públicamente
 * - Operaciones CRUD restringidas a ADMIN
 */
@RestController
@RequestMapping("/api/v1/maestros")
public class MaestroController {

    private static final Logger logger = LoggerFactory.getLogger(MaestroController.class);
    private final MaestroService maestroService;

    public MaestroController(MaestroService maestroService) {
        this.maestroService = maestroService;
    }

    /**
     * Obtener todos los maestros
     * GET /api/v1/maestros
     * Acceso: Público
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Maestro>>> obtenerTodosMaestros() {
        logger.info("GET /api/v1/maestros - Obteniendo todos los maestros");
        List<Maestro> maestros = maestroService.obtenerTodos();
        return ResponseEntity.ok(ApiResponse.success(maestros, "Maestros obtenidos exitosamente"));
    }

    /**
     * Obtener maestros por grupo
     * GET /api/v1/maestros/grupo/{grupo}
     * Acceso: Público
     * 
     * @param grupo Nombre del grupo
     */
    @GetMapping("/grupo/{grupo}")
    public ResponseEntity<ApiResponse<List<Maestro>>> obtenerMaestrosPorGrupo(
            @PathVariable String grupo) {
        logger.info("GET /api/v1/maestros/grupo/{} - Obteniendo maestros por grupo", grupo);
        List<Maestro> maestros = maestroService.obtenerPorGrupo(grupo);
        return ResponseEntity.ok(ApiResponse.success(maestros, "Maestros del grupo obtenidos exitosamente"));
    }

    /**
     * Crear nuevo maestro
     * POST /api/v1/maestros
     * Acceso: Solo ADMIN
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Maestro>> crearMaestro(@RequestBody Maestro maestro) {
        logger.info("POST /api/v1/maestros - Creando nuevo maestro");
        try {
            Maestro created = maestroService.crearMaestro(maestro);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Maestro creado exitosamente"));
        } catch (Exception e) {
            logger.error("Error al crear maestro: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("CREATION_ERROR", "Error al crear el maestro"));
        }
    }

    /**
     * Actualizar maestro existente
     * PUT /api/v1/maestros/{id}
     * Acceso: Solo ADMIN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Maestro>> actualizarMaestro(
            @PathVariable Long id,
            @RequestBody Maestro maestro) {
        logger.info("PUT /api/v1/maestros/{} - Actualizando maestro", id);
        try {
            Maestro updated = maestroService.actualizarMaestro(id, maestro);
            return ResponseEntity.ok(ApiResponse.success(updated, "Maestro actualizado exitosamente"));
        } catch (Exception e) {
            logger.error("Error al actualizar maestro {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("UPDATE_ERROR", "Error al actualizar el maestro"));
        }
    }

    /**
     * Eliminar maestro
     * DELETE /api/v1/maestros/{id}
     * Acceso: Solo ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminarMaestro(@PathVariable Long id) {
        logger.info("DELETE /api/v1/maestros/{} - Eliminando maestro", id);
        try {
            maestroService.eliminarMaestro(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Maestro eliminado exitosamente"));
        } catch (Exception e) {
            logger.error("Error al eliminar maestro {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("DELETE_ERROR", "Error al eliminar el maestro"));
        }
    }
}
