package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.model.DTOs.StatsDto;
import com.forjix.cuentoskilla.service.StatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de Estadísticas del Sistema
 * 
 * Rutas: /api/v1/admin/stats
 * 
 * Funcionalidad:
 * - GET: Obtener estadísticas agregadas
 * 
 * Seguridad (OWASP):
 * - Solo ADMIN puede acceder
 */
@RestController
@RequestMapping("/api/v1/admin/stats")
public class StatsController {

    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);
    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    /**
     * Obtener estadísticas agregadas
     * GET /api/v1/admin/stats?range=30
     * 
     * @param range Número de días o 'Nm' para últimos N meses
     * Acceso: Solo ADMIN
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estadísticas agregadas del sistema", parameters = {
            @Parameter(name = "range", description = "Número de días o 'Nm' para últimos N meses", required = true)
    })
    public ResponseEntity<ApiResponse<StatsDto>> getStats(@RequestParam String range) {
        logger.info("GET /api/v1/admin/stats - Admin consultando estadísticas con rango: {}", range);
        StatsDto stats = service.getStats(range);
        return ResponseEntity.ok(ApiResponse.success(stats, "Estadísticas obtenidas exitosamente"));
    }
}
