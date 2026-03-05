package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.Departamento;
import com.forjix.cuentoskilla.model.Distrito;
import com.forjix.cuentoskilla.model.Provincia;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.service.UbigeoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de Ubigeo (Ubicación Geográfica)
 * 
 * Rutas: /api/v1/ubigeo
 * 
 * Funcionalidad:
 * - GET /departamentos: Obtener todos los departamentos
 * - GET /provincias/{id}: Obtener provincias de un departamento
 * - GET /distritos/{id}: Obtener distritos de una provincia
 * 
 * Seguridad (OWASP):
 * - Acceso público (datos de referencia)
 * - Sin autenticación requerida
 */
@RestController
@RequestMapping("/api/v1/ubigeo")
public class UbigeoController {

    private static final Logger logger = LoggerFactory.getLogger(UbigeoController.class);
    private final UbigeoService ubigeoService;

    public UbigeoController(UbigeoService ubigeoService) {
        this.ubigeoService = ubigeoService;
    }

    /**
     * Obtener todos los departamentos
     * GET /api/v1/ubigeo/departamentos
     * Acceso: Público
     */
    @GetMapping("/departamentos")
    public ResponseEntity<ApiResponse<List<Departamento>>> obtenerDepartamentos() {
        logger.info("GET /api/v1/ubigeo/departamentos - Obteniendo todos los departamentos");
        List<Departamento> departamentos = ubigeoService.obtenerTodosDepartamentos();
        return ResponseEntity.ok(ApiResponse.success(departamentos, "Departamentos obtenidos exitosamente"));
    }

    /**
     * Obtener provincias de un departamento
     * GET /api/v1/ubigeo/provincias/{departamentoId}
     * Acceso: Público
     * 
     * @param departamentoId ID del departamento
     */
    @GetMapping("/provincias/{departamentoId}")
    public ResponseEntity<ApiResponse<List<Provincia>>> obtenerProvincias(@PathVariable String departamentoId) {
        logger.info("GET /api/v1/ubigeo/provincias/{} - Obteniendo provincias", departamentoId);
        List<Provincia> provincias = ubigeoService.obtenerProvinciasPorDepartamento(departamentoId);
        return ResponseEntity.ok(ApiResponse.success(provincias, "Provincias obtenidas exitosamente"));
    }

    /**
     * Obtener distritos de una provincia
     * GET /api/v1/ubigeo/distritos/{provinciaId}
     * Acceso: Público
     * 
     * @param provinciaId ID de la provincia
     */
    @GetMapping("/distritos/{provinciaId}")
    public ResponseEntity<ApiResponse<List<Distrito>>> obtenerDistritos(@PathVariable String provinciaId) {
        logger.info("GET /api/v1/ubigeo/distritos/{} - Obteniendo distritos", provinciaId);
        List<Distrito> distritos = ubigeoService.obtenerDistritosPorProvincia(provinciaId);
        return ResponseEntity.ok(ApiResponse.success(distritos, "Distritos obtenidos exitosamente"));
    }
}
