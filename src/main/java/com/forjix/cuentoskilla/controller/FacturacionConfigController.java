package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.model.DTOs.FacturacionParametroRequest;
import com.forjix.cuentoskilla.model.DTOs.FacturacionParametroResponse;
import com.forjix.cuentoskilla.service.FacturacionConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/facturacion/parametros")
@PreAuthorize("hasRole('ADMIN')")
public class FacturacionConfigController {

    private final FacturacionConfigService facturacionConfigService;

    public FacturacionConfigController(FacturacionConfigService facturacionConfigService) {
        this.facturacionConfigService = facturacionConfigService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FacturacionParametroResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.success(facturacionConfigService.listarParametros(), "Parámetros de facturación"));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ApiResponse<FacturacionParametroResponse>> obtener(@PathVariable String codigo) {
        try {
            return ResponseEntity.ok(ApiResponse.success(facturacionConfigService.obtenerParametro(codigo), "Parámetro obtenido"));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_INPUT", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FacturacionParametroResponse>> crear(@RequestBody FacturacionParametroRequest request) {
        try {
            FacturacionParametroResponse created = facturacionConfigService.crearParametro(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(created, "Parámetro creado"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_INPUT", ex.getMessage()));
        }
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<ApiResponse<FacturacionParametroResponse>> actualizar(
            @PathVariable String codigo,
            @RequestBody FacturacionParametroRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    facturacionConfigService.actualizarParametro(codigo, request),
                    "Parámetro actualizado"
            ));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_INPUT", ex.getMessage()));
        }
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable String codigo) {
        try {
            facturacionConfigService.eliminarParametro(codigo);
            return ResponseEntity.ok(ApiResponse.success(null, "Parámetro desactivado"));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_INPUT", ex.getMessage()));
        }
    }
}

