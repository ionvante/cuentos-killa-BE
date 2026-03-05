package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Cuento;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.repository.CuentoRepository;
import com.forjix.cuentoskilla.service.CuentoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * API de Gestión de Cuentos (Libros/Productos)
 * 
 * Rutas: /api/v1/cuentos
 * 
 * Funcionalidad:
 * - GET: Obtener todos los cuentos (público)
 * - GET /{id}: Obtener cuento específico (público)
 * - POST: Crear cuento (solo ADMIN)
 * - PUT: Actualizar cuento (solo ADMIN)
 * - DELETE: Eliminar cuento (solo ADMIN)
 * 
 * Seguridad (OWASP):
 * - Rate limiting aplicado
 * - Validación de entrada
 * - Manejo de archivos seguro
 */
@RestController
@RequestMapping("/api/v1/cuentos")
public class CuentoController {

    private static final Logger logger = LoggerFactory.getLogger(CuentoController.class);
    private final CuentoRepository cuentoRepository;
    private final CuentoService cuentoService;
    private final ObjectMapper objectMapper;

    public CuentoController(CuentoRepository cuentoRepository, CuentoService cuentoService, ObjectMapper objectMapper) {
        this.cuentoRepository = cuentoRepository;
        this.cuentoService = cuentoService;
        this.objectMapper = objectMapper;
    }

    /**
     * Obtener todos los cuentos
     * GET /api/v1/cuentos
     * Acceso: Público
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Cuento>>> getAll() {
        logger.info("GET /api/v1/cuentos - Listando todos los cuentos");
        List<Cuento> cuentos = cuentoRepository.findAll();
        return ResponseEntity.ok(
            ApiResponse.success(cuentos, "Cuentos obtenidos exitosamente")
        );
    }

    /**
     * Obtener cuentos con paginación
     * GET /api/v1/cuentos/page?page=0&size=10
     * Acceso: Público
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<Cuento>>> getCuentosPaginados(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("GET /api/v1/cuentos/page - Paginación: page={}, size={}", page, size);
        Page<Cuento> cuentosPaginados = cuentoService.obtenerCuentosPaginados(PageRequest.of(page, size));
        return ResponseEntity.ok(
            ApiResponse.success(cuentosPaginados, "Cuentos paginados obtenidos exitosamente")
        );
    }

    /**
     * Obtener cuento por ID
     * GET /api/v1/cuentos/{id}
     * Acceso: Público
     * 
     * @param id ID del cuento
     * @return Datos completos del cuento
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Cuento>> getById(@PathVariable Long id) {
        logger.info("GET /api/v1/cuentos/{} - Obteniendo detalles del cuento", id);
        return cuentoRepository.findById(id)
                .map(cuento -> ResponseEntity.ok(
                    ApiResponse.success(cuento, "Cuento obtenido exitosamente")
                ))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(
                        "CUENTO_NOT_FOUND",
                        "Cuento no encontrado con ID: " + id
                    )));
    }

    /**
     * Crear nuevo cuento
     * POST /api/v1/cuentos
     * Acceso: Solo ADMIN
     * 
     * @param cuentoJson JSON del cuento
     * @param file Imagen opcional
     */
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Cuento>> create(
            @RequestPart("cuento") String cuentoJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        logger.info("POST /api/v1/cuentos - Creando nuevo cuento");
        try {
            Cuento cuento = objectMapper.readValue(cuentoJson, Cuento.class);
            Cuento created = cuentoService.save(cuento, file);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Cuento creado exitosamente"));
        } catch (IOException e) {
            logger.error("Error al parsear JSON del cuento", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                    "INVALID_JSON",
                    "Error en formato del JSON proporcionado"
                ));
        } catch (Exception e) {
            logger.error("Error al crear cuento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    "CREATION_ERROR",
                    "Error al crear el cuento"
                ));
        }
    }

    /**
     * Actualizar cuento
     * PUT /api/v1/cuentos/{id}
     * Acceso: Solo ADMIN
     * 
     * @param id ID del cuento
     * @param cuentoJson JSON actualizado
     * @param file Nueva imagen opcional
     */
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Cuento>> update(
            @PathVariable Long id,
            @RequestPart("cuento") String cuentoJson,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        logger.info("PUT /api/v1/cuentos/{} - Actualizando cuento", id);
        try {
            Cuento cuento = objectMapper.readValue(cuentoJson, Cuento.class);
            return cuentoService.update(id, cuento, file)
                    .map(updated -> ResponseEntity.ok(
                        ApiResponse.success(updated, "Cuento actualizado exitosamente")
                    ))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(
                            "CUENTO_NOT_FOUND",
                            "Cuento no encontrado con ID: " + id
                        )));
        } catch (IOException e) {
            logger.error("Error al parsear JSON del cuento", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                    "INVALID_JSON",
                    "Error en formato del JSON proporcionado"
                ));
        } catch (Exception e) {
            logger.error("Error al actualizar cuento", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                    "UPDATE_ERROR",
                    "Error al actualizar el cuento"
                ));
        }
    }

    /**
     * Actualizar estado de disponibilidad
     * PUT /api/v1/cuentos/{id}/estado
     * Acceso: Solo ADMIN
     * 
     * @param id ID del cuento
     * @param estado Estado (habilitado/deshabilitado)
     */
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Cuento>> updateEstado(
            @PathVariable Long id,
            @RequestBody Cuento estado) {
        logger.info("PUT /api/v1/cuentos/{}/estado - Actualizando estado", id);
        return cuentoRepository.findById(id)
                .map(existing -> {
                    existing.setHabilitado(estado.isHabilitado());
                    Cuento updated = cuentoRepository.save(existing);
                    return ResponseEntity.ok(
                        ApiResponse.success(
                            updated,
                            "Estado del cuento actualizado a: " + (estado.isHabilitado() ? "HABILITADO" : "DESHABILITADO")
                        )
                    );
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(
                        "CUENTO_NOT_FOUND",
                        "Cuento no encontrado con ID: " + id
                    )));
    }

    /**
     * Eliminar cuento
     * DELETE /api/v1/cuentos/{id}
     * Acceso: Solo ADMIN
     * 
     * @param id ID del cuento a eliminar
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        logger.info("DELETE /api/v1/cuentos/{} - Eliminando cuento", id);
        if (cuentoRepository.existsById(id)) {
            cuentoRepository.deleteById(id);
            return ResponseEntity.ok(
                ApiResponse.success(null, "Cuento eliminado exitosamente")
            );
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                    "CUENTO_NOT_FOUND",
                    "Cuento no encontrado con ID: " + id
                ));
        }
    }
}
