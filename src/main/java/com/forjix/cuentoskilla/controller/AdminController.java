package com.forjix.cuentoskilla.controller;

import com.forjix.cuentoskilla.model.DTOs.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API de Operaciones Administrativas
 * 
 * Rutas: /api/v1/admin
 * 
 * Funcionalidad:
 * - GET /pedidos: Listar todos los pedidos
 * - POST /{id}/verificar: Verificar pago de pedido
 * - POST /{id}/empaquetar: Marcar como empaquetado
 * - POST /{id}/enviar: Marcar como enviado
 * - POST /{id}/entregar: Marcar como entregado
 * 
 * Seguridad (OWASP):
 * - Toda acceso restringido a rol ADMIN
 * - Auditoría de cambios de estado
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    /**
     * Obtener listado de todos los pedidos
     * GET /api/v1/admin/pedidos
     * Acceso: Solo ADMIN
     */
    @GetMapping("/pedidos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPedidosAdmin() {
        logger.info("GET /api/v1/admin/pedidos - Admin consultando pedidos");
        return ResponseEntity.ok(
            ApiResponse.success(
                Map.of("message", "Listado de pedidos para el administrador"),
                "Acceso a módulo de administración de pedidos"
            )
        );
    }

    /**
     * Verificar/Confirmar pago de un pedido
     * POST /api/v1/admin/{id}/verificar
     * Acceso: Solo ADMIN
     */
    @PostMapping("/{id}/verificar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> verificarPedido(@PathVariable Long id) {
        logger.info("POST /api/v1/admin/{}/verificar - Admin verificando pago del pedido", id);
        return ResponseEntity.ok(
            ApiResponse.success(
                Map.of("pedidoId", id.toString(), "status", "VERIFICADO"),
                "Pago del pedido verificado exitosamente"
            )
        );
    }

    /**
     * Empaquetar pedido
     * POST /api/v1/admin/{id}/empaquetar
     * Acceso: Solo ADMIN
     */
    @PostMapping("/{id}/empaquetar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> empaquetarPedido(@PathVariable Long id) {
        logger.info("POST /api/v1/admin/{}/empaquetar - Admin empaquetando pedido", id);
        return ResponseEntity.ok(
            ApiResponse.success(
                Map.of("pedidoId", id.toString(), "status", "EMPAQUETADO"),
                "Pedido empaquetado exitosamente"
            )
        );
    }

    /**
     * Marcar pedido como enviado
     * POST /api/v1/admin/{id}/enviar
     * Acceso: Solo ADMIN
     */
    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> enviarPedido(@PathVariable Long id) {
        logger.info("POST /api/v1/admin/{}/enviar - Admin enviando pedido", id);
        return ResponseEntity.ok(
            ApiResponse.success(
                Map.of("pedidoId", id.toString(), "status", "ENVIADO"),
                "Pedido enviado exitosamente"
            )
        );
    }

    /**
     * Marcar pedido como entregado
     * POST /api/v1/admin/{id}/entregar
     * Acceso: Solo ADMIN
     */
    @PostMapping("/{id}/entregar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> entregarPedido(@PathVariable Long id) {
        logger.info("POST /api/v1/admin/{}/entregar - Admin marcando pedido como entregado", id);
        return ResponseEntity.ok(
            ApiResponse.success(
                Map.of("pedidoId", id.toString(), "status", "ENTREGADO"),
                "Pedido marcado como entregado exitosamente"
            )
        );
    }
}
