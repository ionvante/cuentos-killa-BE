package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.config.UserDetailsImpl;
import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import com.forjix.cuentoskilla.service.BoletaService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1")
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @GetMapping({"/orders/{id}/boleta", "/pedidos/{id}/boleta"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> descargarBoleta(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()));

        try {
            BoletaService.BoletaArchivo boletaArchivo = boletaService.obtenerBoletaParaDescarga(id, user.getId(), isAdmin);
            Path filePath = boletaArchivo.filePath();
            Resource resource = toResource(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + boletaArchivo.numeroComprobante() + ".pdf\"")
                    .body(resource);
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "No tienes permiso para descargar esta boleta"));
        } catch (IllegalStateException ex) {
            if ("ORDER_NOT_READY_FOR_BOLETA".equals(ex.getMessage()) || "BOLETA_NOT_READY".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("BOLETA_NOT_READY", "El pedido aun no tiene boleta disponible"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_STATE", ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
        }
    }

    @PostMapping({"/orders/{id}/boleta/retry", "/pedidos/{id}/boleta/retry"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reintentarBoleta(@PathVariable Long id) {
        try {
            BoletaService.BoletaRetryResult result = boletaService.reintentarGeneracionBoleta(id);
            HttpStatus status = "GENERADA".equals(result.estadoGeneracion()) ? HttpStatus.OK : HttpStatus.ACCEPTED;
            String message = "GENERADA".equals(result.estadoGeneracion())
                    ? "Boleta generada correctamente"
                    : "Boleta en error; revisar ultimo error y reintentar";

            return ResponseEntity.status(status).body(ApiResponse.success(
                    Map.of(
                            "orderId", result.orderId(),
                            "numeroComprobante", result.numeroComprobante(),
                            "estadoGeneracion", result.estadoGeneracion(),
                            "intentos", result.intentos(),
                            "ultimoError", result.ultimoError() == null ? "" : result.ultimoError()
                    ),
                    message
            ));
        } catch (IllegalStateException ex) {
            if ("ORDER_NOT_READY_FOR_BOLETA".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("ORDER_NOT_READY_FOR_BOLETA",
                                "El pedido aun no esta en PAGO_VERIFICADO"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("INVALID_STATE", ex.getMessage()));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
        }
    }

    private Resource toResource(Path filePath) {
        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new NoSuchElementException("Archivo de boleta no encontrado");
        }
    }
}

